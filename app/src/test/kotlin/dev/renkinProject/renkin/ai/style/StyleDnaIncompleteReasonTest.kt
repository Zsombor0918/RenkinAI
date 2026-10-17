package dev.renkinProject.renkin.ai.style

import android.app.Application
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Synthetic public-shaped envelopes, never captures of private provider output. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyleDnaIncompleteReasonTest {
    private fun envelope(details: Any? = null): JSONObject = json(
        "id" to "resp_offline_incomplete", "object" to "response", "status" to "incomplete",
        "error" to null, "incomplete_details" to details, "max_output_tokens" to 12288,
        "output" to JSONArray().put(json("type" to "reasoning", "summary" to JSONArray()))
            .put(json("type" to "message", "status" to "incomplete", "role" to "assistant",
                "content" to JSONArray().put(json("type" to "output_text", "text" to "private partial reports")))),
        "usage" to json("input_tokens" to 1000, "output_tokens" to 12288,
            "output_tokens_details" to json("reasoning_tokens" to 0), "total_tokens" to 13288))

    @Test fun outputLimitIsPreservedAsFixedReasonWithoutAcceptingPartialOutput() {
        val result = parseDnaResponse(envelope(json("reason" to "max_output_tokens")).toString())
        assertEquals(DnaCallResult.Failed("RESPONSE_INCOMPLETE_MAX_OUTPUT_TOKENS", true), result)
        assertFalse(result.toString().contains("private"))
    }

    @Test fun contentFilterIsDistinctFromOutputLimitAndRetainsBillingUncertainty() {
        val result = parseDnaResponse(envelope(json("reason" to "content_filter")).toString())
        assertEquals(DnaCallResult.Failed("RESPONSE_INCOMPLETE_CONTENT_FILTER", true), result)
        assertFalse(result.toString().contains("private"))
    }

    @Test fun absentUnknownOrWrongTypedReasonBecomesUnknownWithoutProviderProse() {
        val variants = listOf(
            envelope().also { it.remove("incomplete_details") },
            envelope(), envelope(json()), envelope(json("reason" to null)),
            envelope(json("reason" to "private unexpected provider prose")),
            envelope(json("reason" to "MAX_OUTPUT_TOKENS")),
            envelope(json("reason" to 12288)), envelope(json("reason" to json("private" to true))),
            envelope(JSONArray().put(json("reason" to "max_output_tokens"))),
            envelope("private invalid details"))
        for (raw in variants) {
            val result = parseDnaResponse(raw.toString())
            assertEquals(DnaCallResult.Failed("RESPONSE_INCOMPLETE_UNKNOWN", true), result)
            assertFalse(result.toString().contains("private"))
        }
    }

    @Test fun malformedIncompleteEnvelopeStillUsesStrictJsonBounds() {
        val valid = envelope(json("reason" to "max_output_tokens")).toString()
        assertEquals(DnaCallResult.Failed("ENVELOPE_JSON_DUPLICATE"),
            parseDnaResponse(valid.replaceFirst("{", "{\"status\":\"incomplete\",")))
        assertEquals(DnaCallResult.Failed("ENVELOPE_JSON_SIZE"),
            parseDnaResponse(envelope(json("reason" to "x".repeat(128 * 1024))).toString()))
        assertEquals(DnaCallResult.Failed("ENVELOPE_ROOT_STATUS"),
            parseDnaResponse(envelope(json("reason" to "max_output_tokens")).put("status", "queued").toString()))
    }

    @Test fun oldAndNewFixedReasonsRoundTripWithoutRewritingSchemaOrConsumedMarkers() {
        val (plan, _) = dnaFixturePlan(DnaAnalysisMode.QUICK, 12)
        for (reason in listOf("RESPONSE_INCOMPLETE", "RESPONSE_INCOMPLETE_MAX_OUTPUT_TOKENS",
            "RESPONSE_INCOMPLETE_CONTENT_FILTER", "RESPONSE_INCOMPLETE_UNKNOWN")) {
            val record = StyleDnaRecord(plan, DnaStatus.UNCERTAIN,
                plan.steps.map { DnaStepState(it.id, "SUBMITTED") }, reason = reason)
            val text = record.encode()
            assertEquals(1, JSONObject(text).getInt("schema"))
            assertEquals(record, StyleDnaRecord.parse(text))
            assertEquals(text, StyleDnaRecord.parse(text).encode())
            assertEquals(1, record.submitted)
            assertNull(record.result)
        }
    }
}
