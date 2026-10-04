package dev.renkinProject.renkin.ai

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AiBatchRunnerTest {
    private fun session(vararg names: String) = AiBatchSession(7, AiSettings(), names.map { AiBatchItem(it, it) })
    @Test fun successIsRecordedAndNeverGeneratedAgain() = runBlocking {
        val calls = mutableListOf<String>(); val writes = mutableListOf<AiBatchSession>(); val applied = mutableListOf<String>()
        val runner = AiBatchRunner(session("a", "b"), { writes += it }, { AiBatchTarget.MISSING }, { null },
            { calls += it.key; "img_1_1.png" }, { applied += it.key; AiBatchTarget.MISSING }, { true })
        runner.run(); runner.run()
        assertEquals(listOf("a", "b"), calls); assertEquals(calls, applied)
        assertEquals(2, runner.state.value.done)
        assertTrue(writes.any { it.items[0].status == AiBatchStatus.REQUESTING })
        assertTrue(writes.any { it.items[0].status == AiBatchStatus.GENERATED && it.items[0].fileName != null })
        assertTrue(runner.state.value.items.all { it.status == AiBatchStatus.SUCCEEDED })
    }
    @Test fun rateLimitStopsNextCallAndRetryDoesNotRepeatCompletedApps() = runBlocking {
        val calls = mutableListOf<String>(); var limited = true
        val runner = AiBatchRunner(session("done", "rate", "later"), {}, { AiBatchTarget.MISSING }, { null }, {
            calls += it.key
            if (it.key == "rate" && limited) throw IOException("API rate limit reached (HTTP 429)")
            "img_1_1.png"
        }, { AiBatchTarget.MISSING }, { true })
        runner.run()
        assertEquals(listOf("done", "rate"), calls)
        assertEquals(AiBatchStatus.FAILED, runner.state.value.items[1].status)
        limited = false; runner.retry("rate"); runner.run("rate")
        assertEquals(listOf("done", "rate", "rate"), calls)
        assertEquals(AiBatchStatus.PENDING, runner.state.value.items[2].status)
        runner.run()
        assertEquals(listOf("done", "rate", "rate", "later"), calls)
    }
    @Test fun filledLockedRemovedAndWrongProfileNeverGenerate() = runBlocking {
        var calls = 0
        val runner = AiBatchRunner(session("filled", "locked", "profile", "later"), {}, {
            when(it) { "filled" -> AiBatchTarget.FILLED; "locked" -> AiBatchTarget.UNAVAILABLE; else -> AiBatchTarget.PROFILE_CHANGED }
        }, { null }, { calls++; "img_1_1.png" }, { AiBatchTarget.MISSING }, { true })
        runner.run()
        assertEquals(0, calls); assertEquals(2, runner.state.value.done)
        assertEquals(AiBatchStatus.PENDING, runner.state.value.items[3].status)
    }
    @Test fun pauseCompletesCurrentSaveButStartsNoNextRequest() = runBlocking {
        val calls = mutableListOf<String>(); lateinit var runner: AiBatchRunner
        runner = AiBatchRunner(session("a", "b"), {}, { AiBatchTarget.MISSING }, { null }, {
            calls += it.key; runner.pause(); "img_1_1.png"
        }, { AiBatchTarget.MISSING }, { true })
        runner.run()
        assertEquals(listOf("a"), calls)
        assertEquals(AiBatchStatus.SUCCEEDED, runner.state.value.items[0].status)
        assertEquals(AiBatchStatus.PENDING, runner.state.value.items[1].status)
    }
    @Test fun corruptImageFailsOnlyThatApp() = runBlocking {
        val runner = AiBatchRunner(session("corrupt", "good"), {}, { AiBatchTarget.MISSING }, { null }, {
            if (it.key == "corrupt") throw IllegalArgumentException("Invalid PNG") else "img_1_1.png"
        }, { AiBatchTarget.MISSING }, { true })
        runner.run()
        assertEquals(listOf(AiBatchStatus.FAILED, AiBatchStatus.SUCCEEDED), runner.state.value.items.map { it.status })
    }
    @Test fun cachedPngIsAppliedWithoutNetworkingEvenWhenOffline() = runBlocking {
        var calls = 0
        val runner = AiBatchRunner(session("a"), {}, { AiBatchTarget.MISSING }, { "img_1_1.png" },
            { calls++; "unexpected" }, { AiBatchTarget.MISSING }, { false })
        runner.run()
        assertEquals(0, calls); assertEquals(AiBatchStatus.SUCCEEDED, runner.state.value.items[0].status)
    }
    @Test fun interruptedRequestRequiresExplicitRetryUnlessPngWasSaved() = runBlocking {
        val old = session("uncertain", "saved").copy(items = session("uncertain", "saved").items.map { it.copy(status = AiBatchStatus.REQUESTING) })
        var calls = 0
        val runner = AiBatchRunner(old, {}, { AiBatchTarget.MISSING }, { if (it.key == "saved") "img_1_1.png" else null },
            { calls++; "bad" }, { AiBatchTarget.MISSING }, { true })
        runner.recoverInterrupted(); runner.run()
        assertEquals(0, calls)
        assertEquals(AiBatchStatus.FAILED, runner.state.value.items[0].status)
        assertEquals(AiBatchStatus.SUCCEEDED, runner.state.value.items[1].status)
    }
    @Test fun cancellingInFlightPreservesEarlierSuccessAndMarksBillingUncertain() = runBlocking {
        val entered = CompletableDeferred<Unit>(); val block = CompletableDeferred<Unit>()
        val runner = AiBatchRunner(session("done", "inflight", "pending"), {}, { AiBatchTarget.MISSING }, { null }, {
            if (it.key == "inflight") { entered.complete(Unit); block.await() }
            "img_1_1.png"
        }, { AiBatchTarget.MISSING }, { true })
        val job = launch { runner.run() }; entered.await(); job.cancelAndJoin(); runner.cancelRemaining()
        assertEquals(listOf(AiBatchStatus.SUCCEEDED, AiBatchStatus.FAILED, AiBatchStatus.SKIPPED), runner.state.value.items.map { it.status })
        assertTrue(runner.state.value.items[1].error!!.contains("billed"))
    }
    @Test fun journalRoundTripKeepsSnapshotInstructionsStatusesAndFile() {
        val initial = session("a").copy(settings = AiSettings(style = "white outline", referenceNames = listOf("img_1_1.png")),
            items = listOf(AiBatchItem("a", "App", "Keep the unusual leaf", AiBatchStatus.GENERATED, "img_2_2.png")))
        assertEquals(initial, decodeBatch(encodeBatch(initial)))
        assertFalse(encodeBatch(initial).toString().contains("Bearer"))
    }
    @Test fun individualInstructionCannotRemoveOutputContract() {
        val prompt = iconPrompt("Shared 24px white outline", 2, "Use a leaf symbol")
        assertTrue(prompt.contains("Shared 24px white outline")); assertTrue(prompt.contains("Use a leaf symbol"))
        assertTrue(prompt.indexOf("APP-SPECIFIC INSTRUCTION") < prompt.indexOf("OUTPUT CONTRACT"))
        assertTrue(prompt.contains("genuine transparent alpha")); assertTrue(prompt.contains("No text"))
        assertThrows(IllegalArgumentException::class.java) { AiSettings(appInstruction = "a".repeat(2001)).validate() }
    }
}
