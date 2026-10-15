package dev.renkinProject.renkin.ai.style

import android.app.Application
import android.util.Base64
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.TimeUnit

/** Representative REST response, not a capture from the owner's phone. The response
 * text.format includes the request's structured-output configuration in the official
 * Response/ResponseTextConfig contract. No invented nested metadata is needed. */
internal fun dnaFullResponseFixture(request: DnaCall, text: String): String = json(
    "id" to "resp_full_fixture", "object" to "response", "created_at" to 1780000000,
    "status" to "completed", "error" to null, "incomplete_details" to null,
    "model" to request.plan.model, "instructions" to "offline fixture",
    "max_output_tokens" to 12288, "parallel_tool_calls" to true, "previous_response_id" to null,
    "reasoning" to json("effort" to "none", "summary" to null), "store" to false,
    "temperature" to 1, "text" to dnaBody(request).getJSONObject("text"),
    "tool_choice" to "auto", "tools" to JSONArray(), "top_p" to 1, "truncation" to "disabled",
    "metadata" to json(), "usage" to json("input_tokens" to 40, "output_tokens" to 50,
        "input_tokens_details" to json("cached_tokens" to 0), "output_tokens_details" to json("reasoning_tokens" to 0), "total_tokens" to 90),
    "output" to JSONArray().put(json("type" to "reasoning", "id" to "rs_fixture", "summary" to JSONArray()))
        .put(json("type" to "message", "id" to "msg_fixture", "status" to "completed", "role" to "assistant",
            "content" to JSONArray().put(json("type" to "output_text", "text" to text,
                "annotations" to JSONArray(), "logprobs" to JSONArray()))))
).toString()

@RunWith(RobolectricTestRunner::class)
@Config(application=Application::class,sdk=[33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyleDnaGatewayTest {
    @Test fun frozenReportSchemaMatchesNonemptyTextExactCountAndAllowedBindings() {
        for (mode in DnaAnalysisMode.entries) {
            val request = call(mode)
            val schema = dnaBody(request).getJSONObject("text").getJSONObject("format").getJSONObject("schema")
            val reports = schema.getJSONObject("properties").getJSONObject("reports")
            assertEquals(request.step.samples.size, reports.optInt("minItems", -1))
            assertEquals(request.step.samples.size, reports.getInt("maxItems"))
            val properties = reports.getJSONObject("items").getJSONObject("properties")
            assertEquals(request.step.samples, dnaStrings(properties.getJSONObject("sample_id").getJSONArray("enum"), 24, 48))
            assertEquals(request.step.samples.map { request.plan.bindings.getValue(it) },
                dnaStrings(properties.getJSONObject("sample_hash").getJSONArray("enum"), 24, 64))
            val observation = properties.getJSONObject("traits").getJSONObject("properties")
                .getJSONObject("corners").getJSONObject("properties").getJSONObject("value")
            val pattern = Regex(observation.getString("pattern"))
            for (bad in listOf("", "   ", "private\ntext", "x".repeat(241))) assertFalse(pattern.containsMatchIn(bad))
            for (good in listOf("Unknown", "rounded ends", "slight gradient")) assertTrue(pattern.containsMatchIn(good))
        }
    }
    private fun response(text: String)=json("id" to "resp_mock","status" to "completed","usage" to json("input_tokens" to 40,"output_tokens" to 50),
        "output" to JSONArray().put(json("type" to "reasoning")).put(json("type" to "message","status" to "completed","content" to JSONArray().put(json("type" to "output_text","text" to text))))).toString()
    private fun client(server: MockWebServer,timeout: Long = 2000)=OpenAiStyleDnaGateway({"offline_fixture_${java.util.UUID.randomUUID()}"},
        OkHttpClient.Builder().callTimeout(timeout,TimeUnit.MILLISECONDS).build(),server.url("/v1/responses"))
    private fun call(mode: DnaAnalysisMode = DnaAnalysisMode.STANDARD): DnaCall {
        val (p,images)=dnaFixturePlan(mode,if(mode==DnaAnalysisMode.QUICK)12 else 24);val step=p.steps.first()
        return DnaCall(p,step,step.samples.associate { p.bindings.getValue(it) to images.getValue(p.bindings.getValue(it)) },emptyList())
    }
    @Test fun returnedReportFormatSchemaExceedsOldEnvelopeDepthButSixReportsAreValid() = runBlocking {
        val request = call()
        val expected = dnaReportsBody(dnaFixtureReports(request.plan, request.step.samples))
        val raw = dnaFullResponseFixture(request, expected)
        fun depth(value: Any?, level: Int = 0): Int = when (value) {
            is JSONObject -> value.keys().asSequence().map { depth(value.get(it), level + 1) }.maxOrNull() ?: level
            is JSONArray -> (0 until value.length()).maxOfOrNull { depth(value.get(it), level + 1) } ?: level
            else -> level
        }
        assertEquals(14, depth(JSONObject(raw)))
        assertThrows(IllegalArgumentException::class.java) { strictObject(raw, 128 * 1024, 12) }
        // The unchanged Legacy parser accepts the same outer response shape with its
        // own shallower real schema. This is a structural difference, not a model guess.
        val legacy = JSONObject(raw).put("text", analysisBody(analysisFixtureRequest()).getJSONObject("text"))
        legacy.getJSONArray("output").getJSONObject(1).getJSONArray("content").getJSONObject(0)
            .put("text", analysisFixtureStyle().encode())
        assertTrue(parseAnalysisResponse(legacy.toString()) is StyleAnalysisOutcome.Suggested)
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(raw))
            var markers = 0
            val outcome = client(server).call(request) { markers++ }
            assertEquals(1, markers); assertEquals(1, server.requestCount)
            assertTrue("valid six-image REST envelope was rejected: $outcome", outcome is DnaCallResult.Received)
            outcome as DnaCallResult.Received
            assertEquals(expected, outcome.text)
            val bindings = request.step.samples.associateWith { request.plan.bindings.getValue(it) }
            val reports = StyleDnaCodec.parseReports(outcome.text, bindings)
            assertEquals(6, reports.size); assertEquals(bindings.keys.toList(), reports.map { it.sampleId })
            assertEquals(bindings.values.toList(), reports.map { it.sampleHash })
            assertEquals("resp_full_fixture", outcome.responseId)
            assertEquals(AnalysisUsage(40, 50), outcome.usage)
        }
    }
    @Test fun realWireHasIndividuallyLabeledImagesStrictReportsAndNoLocalIdentity()=runBlocking {
        for(mode in DnaAnalysisMode.entries) for(detail in listOf("low","high")) {
            val server=MockWebServer();server.start()
            try {
                val original=call(mode);val request=original.copy(plan=original.plan.copy(detail=detail))
                server.enqueue(MockResponse().setBody(response(dnaReportsBody(dnaFixtureReports(request.plan,request.step.samples)))))
                var submissions=0;val result=client(server).call(request){submissions++}
                assertTrue(result is DnaCallResult.Received);assertEquals(1,submissions);assertEquals(1,server.requestCount)
                val wire=server.takeRequest();assertEquals("/v1/responses",wire.path);assertEquals(request.step.id,wire.getHeader("X-Client-Request-Id"))
                val raw=wire.body.readUtf8();val body=JSONObject(raw)
                assertFalse(body.getBoolean("store"));assertEquals(DEFAULT_ANALYSIS_MODEL,body.getString("model"));assertEquals(12288,body.getInt("max_output_tokens"))
                val content=body.getJSONArray("input").getJSONObject(0).getJSONArray("content")
                assertEquals(1+request.step.samples.size*2,content.length())
                request.step.samples.forEachIndexed { n,id ->
                    val label=JSONObject(content.getJSONObject(1+n*2).getString("text"));assertEquals(id,label.getString("sample_id"))
                    val image=content.getJSONObject(2+n*2);assertEquals("input_image",image.getString("type"));assertEquals(detail,image.getString("detail"))
                    assertArrayEquals(request.images.getValue(request.plan.bindings.getValue(id)),Base64.decode(image.getString("image_url").substringAfter(','),Base64.DEFAULT))
                }
                val format=body.getJSONObject("text").getJSONObject("format");assertEquals("json_schema",format.getString("type"));assertTrue(format.getBoolean("strict"))
                val schema=format.getJSONObject("schema");assertFalse(schema.getBoolean("additionalProperties"))
                assertFalse(raw.contains("profileId"));assertFalse(raw.contains("catalogStamp"));assertFalse(raw.contains("project_ai_assets"));assertFalse(raw.contains("offline_fixture"))
            } finally {server.shutdown()}
        }
    }
    @Test fun synthesisUploadsVerifiedReportsAndMeasuredTextWithZeroImageParts() {
        val (p,images)=dnaFixturePlan();val reports=dnaFixtureReports(p)
        val wire=dnaBody(DnaCall(p,p.steps.last(),emptyMap(),reports));val content=wire.getJSONArray("input").getJSONObject(0).getJSONArray("content")
        assertEquals(1,content.length());assertEquals("input_text",content.getJSONObject(0).getString("type"))
        assertEquals(24,JSONObject(content.getJSONObject(0).getString("text")).getJSONArray("verifiedReports").length())
        assertFalse(wire.toString().contains("data:image"));assertTrue(images.isNotEmpty())
        assertTrue(runCatching {dnaBody(DnaCall(p,p.steps.last(),emptyMap(),reports.dropLast(1)))}.isFailure)
    }
    @Test fun allRejectionsAndRetryAfter503HaveExactlyOneSubmissionAndNoFallback()=runBlocking {
        for(status in listOf(400,401,402,403,422,429,500,503)) {
            val server=MockWebServer();server.start()
            try {
                server.enqueue(MockResponse().setResponseCode(status).addHeader("Retry-After","0").setBody("{\"error\":{\"message\":\"private provider prose\"}}"))
                server.enqueue(MockResponse().setBody(response(dnaReportsBody(dnaFixtureReports(call().plan)))))
                var posts=0;val outcome=client(server).call(call()){posts++}
                assertTrue(outcome is DnaCallResult.Failed);assertEquals(1,posts);assertEquals(1,server.requestCount)
                assertFalse(outcome.toString().contains("private provider prose"))
                assertEquals(DnaCallResult.Failed(when (status) {
                    400,402,422 -> "REQUEST_REJECTED_$status"
                    401,403 -> "UNAUTHORIZED"
                    429 -> "RATE_LIMITED"
                    else -> "UNCERTAIN"
                }, status >= 500), outcome)
            } finally {server.shutdown()}
        }
    }
    @Test fun documentedIncompleteAndFailedResponsesHaveDistinctSafeCodesWithoutReplay() = runBlocking {
        for (status in listOf("incomplete", "failed")) {
            MockWebServer().use { server ->
                server.enqueue(MockResponse().setBody(json("id" to "resp_unfinished", "status" to status,
                    "incomplete_details" to json("reason" to "max_output_tokens"),
                    "error" to json("message" to "private provider prose"), "output" to JSONArray()).toString()))
                var markers = 0
                val outcome = client(server).call(call()) { markers++ }
                assertEquals(DnaCallResult.Failed(if (status == "incomplete") "RESPONSE_INCOMPLETE" else "RESPONSE_FAILED", true), outcome)
                assertEquals(1, markers); assertEquals(1, server.requestCount)
                assertFalse(outcome.toString().contains("private"))
            }
        }
    }

    @Test fun completedEnvelopeAllowsMetadataReasoningOrderAndSplitTextButRejectsToolsAndBadJson() = runBlocking {
        val request = call(DnaAnalysisMode.QUICK)
        val expected = dnaReportsBody(dnaFixtureReports(request.plan).reversed())
        val cut = expected.length / 2
        val envelope = JSONObject(response(expected)).put("metadata", json("irrelevant" to "ignored"))
        envelope.put("output", JSONArray().put(json("type" to "message", "role" to "assistant", "content" to JSONArray()
            .put(json("type" to "output_text", "text" to expected.take(cut), "annotations" to JSONArray()))
            .put(json("type" to "output_text", "text" to expected.drop(cut), "logprobs" to JSONArray()))))
            .put(json("type" to "reasoning", "summary" to JSONArray())))
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(envelope.toString()))
            val received = client(server).call(request) {} as DnaCallResult.Received
            assertEquals(expected, received.text)
            assertEquals(request.plan.samples.map { it.id }, StyleDnaCodec.parseReports(received.text, request.plan.bindings).map { it.sampleId })
            for ((raw, reason) in listOf("{truncated" to "ENVELOPE_JSON_INVALID", JSONObject(envelope.toString()).put("output", JSONArray().put(
                json("type" to "function_call", "name" to "untrusted"))).toString() to "ENVELOPE_OUTPUT_ITEM")) {
                server.enqueue(MockResponse().setBody(raw))
                assertEquals(DnaCallResult.Failed(reason), client(server).call(request) {})
            }
            assertEquals(3, server.requestCount)
        }
    }

    @Test fun scopedSchemaDepthAndFixedEnvelopeReasonsKeepEveryRejectionAndOneShotMarker() = runBlocking {
        val request = call()
        val valid = dnaFullResponseFixture(request, dnaReportsBody(dnaFixtureReports(request.plan, request.step.samples)))
        fun modified(action: (JSONObject) -> Unit) = JSONObject(valid).also(action).toString()
        fun nested(levels: Int): Any = if (levels == 0) "leaf" else json("nested" to nested(levels - 1))
        fun part(root: JSONObject) = root.getJSONArray("output").getJSONObject(1).getJSONArray("content").getJSONObject(0)
        val failures = listOf(
            "{truncated" to "ENVELOPE_JSON_INVALID",
            valid.replaceFirst("{", "{\"status\":\"completed\",") to "ENVELOPE_JSON_DUPLICATE",
            modified { it.put("status", "queued") } to "ENVELOPE_ROOT_STATUS",
            modified { it.put("error", json("message" to "private response prose")) } to "ENVELOPE_ROOT_ERROR",
            modified { it.remove("output") } to "ENVELOPE_OUTPUT",
            modified { it.put("output", JSONArray((0..32).map { json("type" to "reasoning") })) } to "ENVELOPE_OUTPUT",
            modified { it.put("output", JSONArray().put(json("type" to "function_call", "arguments" to "private"))) } to "ENVELOPE_OUTPUT_ITEM",
            modified { it.getJSONArray("output").getJSONObject(1).put("status", "in_progress") } to "ENVELOPE_MESSAGE_STATUS",
            modified { part(it).put("type", "untrusted_content") } to "ENVELOPE_CONTENT",
            modified { part(it).put("text", "") } to "ENVELOPE_TEXT_EMPTY",
            modified { it.put("id", "private\ninvalid") } to "ENVELOPE_RESPONSE_ID",
            modified { it.put("metadata", nested(12)) } to "ENVELOPE_JSON_DEPTH",
            modified { it.getJSONObject("text").getJSONObject("format").put("schema", nested(12)) } to "ENVELOPE_JSON_DEPTH",
            // A lookalike path under an array must not activate the root-only allowance.
            modified { it.put("text", JSONArray().put(json("format" to json("schema" to nested(10))))) } to "ENVELOPE_JSON_DEPTH",
            modified { it.getJSONObject("text").getJSONObject("format").getJSONObject("schema")
                .put("enum", JSONArray((0..128).map { it })) } to "ENVELOPE_JSON_INVALID",
            valid.replaceFirst("\"strict\":true", "\"strict\":true,\"strict\":false") to "ENVELOPE_JSON_DUPLICATE",
            modified { it.getJSONObject("text").getJSONObject("format").getJSONObject("schema").put("description", "BAD_SURROGATE") }
                .replace("BAD_SURROGATE", "\\uD800") to "ENVELOPE_JSON_INVALID"
        )
        MockWebServer().use { server ->
            var markers = 0
            for ((raw, reason) in failures) {
                server.enqueue(MockResponse().setBody(raw))
                val result = client(server).call(request) { markers++ }
                assertEquals(reason, DnaCallResult.Failed(reason), result)
                assertEquals(markers, server.requestCount)
                assertFalse(result.toString().contains("private"))
            }
            assertEquals(failures.size, markers)
            val refusal = modified { part(it).put("type", "refusal").put("refusal", "private prose") }
            server.enqueue(MockResponse().setBody(refusal))
            assertEquals(DnaCallResult.Failed("REFUSED"), client(server).call(request) { markers++ })
            assertEquals(markers, server.requestCount)
        }
        // Default parser and byte limits remain unchanged; no global depth increase.
        assertThrows(StrictJsonRejected::class.java) { strictObject(valid, 128 * 1024, 12) }
        val oversized = "{\"value\":\"" + "x".repeat(128 * 1024) + "\"}"
        assertEquals(DnaCallResult.Failed("ENVELOPE_JSON_SIZE"), parseDnaResponse(oversized))
    }

    @Test fun boundedBodyFaultsAndUnavailableCredentialNeverImplyAValidStyleOrAutomaticRetry() = runBlocking {
        val request = call(DnaAnalysisMode.QUICK)
        MockWebServer().use { server ->
            var markers = 0
            val unavailable = OpenAiStyleDnaGateway({ error("private decryption exception") }, OkHttpClient(), server.url("/v1/responses"))
            assertEquals(DnaCallResult.Failed("UNAUTHORIZED"), unavailable.call(request) { markers++ })
            assertEquals(0, markers); assertEquals(0, server.requestCount)
            val cancelled = runCatching { client(server).call(request) { throw CancellationException("cancel before durable marker") } }
            assertTrue(cancelled.exceptionOrNull() is CancellationException)
            assertEquals(0, markers); assertEquals(0, server.requestCount)
            val valid = response(dnaReportsBody(dnaFixtureReports(request.plan)))
            val faults = listOf(
                MockResponse().setBody(okio.Buffer().write(byteArrayOf(0xc3.toByte(), 0x28))),
                MockResponse().setBody(valid).setHeader("Content-Length", valid.toByteArray().size + 128)
                    .setSocketPolicy(SocketPolicy.DISCONNECT_AT_END),
                MockResponse().setBody("x".repeat(128*1024+1)))
            for (fault in faults) {
                val count = server.requestCount
                server.enqueue(fault)
                assertEquals(DnaCallResult.Failed("UNCERTAIN", true), client(server).call(request) { markers++ })
                assertEquals(count + 1, server.requestCount)
            }
            assertEquals(3, markers); assertEquals(3, server.requestCount)
        }
    }

    @Test fun refusalMalformedPartialOversizeAndTimeoutDoNotResubmit()=runBlocking {
        val refused=json("id" to "resp_refused","status" to "completed","output" to JSONArray().put(json("type" to "message","content" to JSONArray().put(json("type" to "refusal","refusal" to "private"))))).toString()
        for(raw in listOf(refused,"not json",response("{}"),"x".repeat(128*1024+1))) {
            val server=MockWebServer();server.start()
            try {server.enqueue(MockResponse().setBody(raw));var count=0;val r=client(server).call(call()){count++}
                assertEquals(1,count);assertEquals(1,server.requestCount)
                if(raw==response("{}"))assertTrue(r is DnaCallResult.Received) else assertTrue(r is DnaCallResult.Failed)
                if(r is DnaCallResult.Received)assertTrue(runCatching {StyleDnaCodec.parseReports(r.text,call().plan.bindings)}.isFailure)
            } finally {server.shutdown()}
        }
        val server=MockWebServer();server.start()
        try {server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));val r=client(server,80).call(call()){}
            assertTrue(r is DnaCallResult.Failed && r.billingUnknown);assertEquals(1,server.requestCount)
        } finally {server.shutdown()}
    }
}
