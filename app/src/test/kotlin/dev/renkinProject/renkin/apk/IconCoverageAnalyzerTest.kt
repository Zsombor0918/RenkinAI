package dev.renkinProject.renkin.apk

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class IconCoverageAnalyzerTest {
    private val a = CoverageTarget("app/Main", "First")
    private val b = CoverageTarget("app/Other", "Second")
    private fun index(packageName: String = "pack.one", state: CoverageIndexState = CoverageIndexState.READY,
        mappings: Map<String, String> = emptyMap(), dynamic: Set<String> = emptySet()) =
        CoverageIndex(CoverageStamp(packageName, 1, 20, 2, false), state, mappings, dynamic)
    private class Reader(override val index: CoverageIndex,
        val render: (String) -> Boolean = { true }) : CoverageSourceReader {
        var current = true
        val calls = mutableListOf<String>()
        override fun isCurrent() = current
        override suspend fun canRender(drawableName: String): Boolean {
            calls += drawableName
            return render(drawableName)
        }
    }
    private suspend fun scan(reader: Reader, targets: List<CoverageTarget> = listOf(a, b)) =
        IconCoverageAnalyzer().analyze(12, targets, reader)

    @Test fun createInventoryKeepsZeroOneManyAndAlreadyEditedActivities() {
        assertTrue(uniqueCoverageTargets(emptyList()).isEmpty())
        assertEquals(listOf(a), uniqueCoverageTargets(listOf(a)))
        val edited = b.copy(local = LocalIconState.CUSTOM_OR_MANUAL)
        assertEquals(listOf(a, edited), uniqueCoverageTargets(listOf(a, edited)))
        assertTrue(edited.selectable)
        assertEquals(listOf(edited), filterCoverageTargets(listOf(a, edited), "app/other"))
    }
    @Test fun differentSourcesKeepPackageIdentityAndIndependentMappings() = runBlocking {
        val first = scan(Reader(index(mappings = mapOf(a.key to "icon"))))
        val second = scan(Reader(index("pack.two", mappings = mapOf(b.key to "icon"))))
        assertEquals("pack.one", first.index.stamp.packageName)
        assertEquals("pack.two", second.index.stamp.packageName)
        assertEquals(listOf(SourceCoverage.RESOLVABLE, SourceCoverage.UNMAPPED), first.entries.map { it.source })
        assertEquals(listOf(SourceCoverage.UNMAPPED, SourceCoverage.RESOLVABLE), second.entries.map { it.source })
    }
    @Test fun declaredMissingDrawableIsBrokenAndNotUnmapped() = runBlocking {
        val result = scan(Reader(index(mappings = mapOf(a.key to "absent"))) { false })
        assertEquals(SourceCoverage.DECLARED_BROKEN, result.entries[0].source)
        assertEquals(CoverageReason.DRAW_FAILED, result.entries[0].reason)
        assertEquals(SourceCoverage.UNMAPPED, result.entries[1].source)
        assertEquals(1, result.counts.broken)
        assertEquals(1, result.counts.unmapped)
        assertEquals(2, result.counts.actionable)
    }
    @Test fun loadAndDrawExceptionsAreContainedPerDrawable() = runBlocking {
        val reader = Reader(index(mappings = mapOf(a.key to "throws", b.key to "ok"))) {
            if (it == "throws") throw IllegalArgumentException("fixture draw failure") else true
        }
        val result = scan(reader)
        assertEquals(1, result.counts.broken)
        assertEquals(1, result.counts.covered)
    }
    @Test fun unprovenOrLoadingIndexIsUnknownAndNeverRecommended() = runBlocking {
        for (state in listOf(CoverageIndexState.UNKNOWN, CoverageIndexState.LOADING)) {
            val reader = Reader(index(state = state))
            val result = scan(reader)
            assertEquals(2, result.counts.unknown)
            assertEquals(0, result.counts.unmapped)
            assertEquals(0, result.counts.actionable)
            assertTrue(reader.calls.isEmpty())
        }
    }
    @Test fun validZeroMappingIsDifferentFromMissingSource() = runBlocking {
        val empty = scan(Reader(index()))
        val missing = scan(Reader(index(state = CoverageIndexState.UNAVAILABLE)))
        assertEquals(2, empty.counts.unmapped)
        assertEquals(2, missing.counts.unavailable)
        assertEquals(0, missing.counts.actionable)
        assertEquals(0, missing.counts.unmapped)
    }
    @Test fun sameDrawableIsActuallyProbedOncePerSnapshot() = runBlocking {
        val reader = Reader(index(mappings = mapOf(a.key to "shared", b.key to "shared")))
        val result = scan(reader)
        assertEquals(listOf("shared"), reader.calls)
        assertEquals(2, result.counts.covered)
        assertEquals(2, result.counts.declared)
    }
    @Test fun calendarAndClockAreUnprovenWithoutFalseMissingLabels() = runBlocking {
        val reader = Reader(index(mappings = mapOf(a.key to "clock"), dynamic = setOf(a.key, b.key)))
        val result = scan(reader)
        assertEquals(2, result.counts.unknown)
        assertTrue(result.entries.all { it.reason == CoverageReason.DYNAMIC_UNPROVEN })
        assertTrue(reader.calls.isEmpty())
        assertEquals(0, result.counts.actionable)
    }
    @Test fun localManualFallbackAndLocksDoNotChangeSourceFacts() = runBlocking {
        val local = listOf(a.copy(local = LocalIconState.CUSTOM_OR_MANUAL), b.copy(local = LocalIconState.FALLBACK),
            CoverageTarget("c/Main", "Locked", locked = true),
            CoverageTarget("d/Main", "Applied", local = LocalIconState.OTHER_APPLIED),
            CoverageTarget("e/Main", "Uncertain", local = LocalIconState.UNKNOWN))
        val result = scan(Reader(index()), local)
        assertEquals(5, result.counts.unmapped)
        assertEquals(2, result.counts.locallyHandled)
        assertEquals(1, result.counts.locked)
        assertEquals(1, result.counts.actionable)
        assertEquals(b.key, result.entries.single { it.actionable }.target.key)
        assertTrue(result.entries[0].target.selectable) // explicit manual override is allowed
        assertFalse(result.entries[2].target.selectable)
    }
    @Test fun coveredAndUnknownAreNotAutomaticallyRecommendedButCoveredOverrideIsAllowed() = runBlocking {
        val result = scan(Reader(index(mappings = mapOf(a.key to "icon"), dynamic = setOf(b.key))))
        assertEquals(0, result.counts.actionable)
        assertTrue(result.entries[0].target.selectable)
        assertEquals(SourceCoverage.RESOLVABLE, result.entries[0].source)
    }
    @Test fun duplicateUserComponentIsOneUnsupportedTargetAndNeverDrawnOrSelected() = runBlocking {
        val reader = Reader(index(mappings = mapOf(a.key to "icon")))
        val result = scan(reader, listOf(a, a.copy(name = "Work copy"), b))
        assertEquals(2, result.counts.total)
        assertEquals(CoverageReason.USER_IDENTITY, result.entries[0].reason)
        assertFalse(result.entries[0].target.selectable)
        assertTrue(reader.calls.isEmpty())
        assertEquals(1, result.counts.actionable)
    }
    @Test fun progressCountsUniqueComponentsAndSearchDoesNotMixActivityIdentity() = runBlocking {
        val progress = mutableListOf<Pair<Int, Int>>()
        val result = IconCoverageAnalyzer().analyze(12, listOf(a, b), Reader(index())) { done, total -> progress += done to total }
        assertEquals(listOf(1 to 2, 2 to 2), progress)
        assertEquals(listOf(a), filterCoverageTargets(result.entries.map { it.target }, "FIRST"))
        assertEquals(12L, result.profileId)
    }
    @Test fun uninstallOrUpdateDuringDrawInvalidatesEvenEarlierCoveredRows() = runBlocking {
        val reader = Reader(index(mappings = mapOf(a.key to "icon", b.key to "other")))
        val result = IconCoverageAnalyzer().analyze(12, listOf(a, b), reader) { done, _ -> if (done == 1) reader.current = false }
        assertEquals(2, result.counts.unavailable)
        assertEquals(0, result.counts.covered)
        assertEquals(0, result.counts.actionable)
    }
    @Test fun rescanHasNoCrossScanDrawableCacheOrProfileLeak() = runBlocking {
        val first = scan(Reader(index(mappings = mapOf(a.key to "icon"))))
        val changed = Reader(index(mappings = mapOf(a.key to "icon")).copy(stamp = first.index.stamp.copy(versionCode = 2, indexRevision = 3))) { false }
        val result = IconCoverageAnalyzer().analyze(99, listOf(a), changed)
        assertEquals(99L, result.profileId)
        assertEquals(1, result.counts.broken)
        assertEquals(listOf("icon"), changed.calls)
        assertNotEquals(first.index.stamp, result.index.stamp)
    }
    @Test fun cancellationIsNotConvertedToBrokenOrPublishedAsCompleted() = runBlocking {
        try {
            scan(Reader(index(mappings = mapOf(a.key to "cancel"))) { throw CancellationException("leave screen") })
            fail("Cancelled inspection must not publish a snapshot")
        } catch (_: CancellationException) { /* explicit expected cancellation */ }
    }
    @Test fun sourceCountersPartitionTotalWithoutUsingLocalIconPresence() = runBlocking {
        val targets = listOf(a, b, CoverageTarget("c/A", "Broken"), CoverageTarget("d/A", "Dynamic"))
        val result = scan(Reader(index(mappings = mapOf(a.key to "ok", "c/A" to "bad"), dynamic = setOf("d/A"))) { it == "ok" }, targets)
        val c = result.counts
        assertEquals(c.total, c.covered + c.broken + c.unmapped + c.unknown + c.unavailable)
        assertEquals(2, c.declared)
        assertEquals(2, c.actionable)
    }
}
