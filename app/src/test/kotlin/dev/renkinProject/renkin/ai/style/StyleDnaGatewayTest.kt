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

@RunWith(RobolectricTestRunner::class)
@Config(application=Application::class,sdk=[33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyleDnaGatewayTest {
    private fun response(text: String)=json("id" to "resp_mock","status" to "completed","usage" to json("input_tokens" to 40,"output_tokens" to 50),
        "output" to JSONArray().put(json("type" to "reasoning")).put(json("type" to "message","status" to "completed","content" to JSONArray().put(json("type" to "output_text","text" to text))))).toString()
    private fun client(server: MockWebServer,timeout: Long = 2000)=OpenAiStyleDnaGateway({"offline_fixture_${java.util.UUID.randomUUID()}"},
        OkHttpClient.Builder().callTimeout(timeout,TimeUnit.MILLISECONDS).build(),server.url("/v1/responses"))
    private fun call(mode: DnaAnalysisMode = DnaAnalysisMode.STANDARD): DnaCall {
        val (p,images)=dnaFixturePlan(mode,if(mode==DnaAnalysisMode.QUICK)12 else 24);val step=p.steps.first()
        return DnaCall(p,step,step.samples.associate { p.bindings.getValue(it) to images.getValue(p.bindings.getValue(it)) },emptyList())
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
        for(status in listOf(400,401,403,429,503)) {
            val server=MockWebServer();server.start()
            try {
                server.enqueue(MockResponse().setResponseCode(status).addHeader("Retry-After","0").setBody("{\"error\":{\"message\":\"private provider prose\"}}"))
                server.enqueue(MockResponse().setBody(response(dnaReportsBody(dnaFixtureReports(call().plan)))))
                var posts=0;val outcome=client(server).call(call()){posts++}
                assertTrue(outcome is DnaCallResult.Failed);assertEquals(1,posts);assertEquals(1,server.requestCount)
                assertFalse(outcome.toString().contains("private provider prose"))
                if(status==503)assertTrue((outcome as DnaCallResult.Failed).billingUnknown)
            } finally {server.shutdown()}
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
