package dev.renkinProject.renkin.ai.trial

import dev.renkinProject.renkin.apk.*
import org.junit.Assert.*
import org.junit.Test

class StyleTrialTargetSelectorTest {
    private fun targets(n: Int) = (0 until n).map { CoverageTarget("p$it/Main", "App $it") }
    @Test fun createZeroOneTwoThreeAndManyAreRealDistinctStableTargets() {
        for (n in 0..8) {
            val t = targets(n); val c = StyleTrialTargetSelector.candidates("CREATE", t, t.map { it.key })
            val chosen = StyleTrialTargetSelector.recommend(c, kotlin.random.Random(42))
            assertEquals(minOf(3, n), chosen.size); assertEquals(chosen.distinct(), chosen)
            assertEquals(chosen, StyleTrialTargetSelector.recommend(c.reversed(), kotlin.random.Random(42)))
            if (chosen.isNotEmpty()) StyleTrialTargetSelector.validateChoice(c, chosen, false)
        }
    }
    @Test fun randomSuggestionsVaryWithoutIncludingProtectedApplications() {
        val t = targets(12) + CoverageTarget("protected/Main", "Protected", LocalIconState.CUSTOM_OR_MANUAL)
        val c = StyleTrialTargetSelector.candidates("CREATE", t, t.map { it.key })
        val choices = (0..12).map { StyleTrialTargetSelector.recommend(c, kotlin.random.Random(it)) }
        assertTrue(choices.distinct().size > 1)
        choices.forEach { StyleTrialTargetSelector.validateChoice(c, it, false); assertFalse("protected/Main" in it) }
    }
    @Test fun extendDistinguishesRealMissingBrokenCoveredAndUnknown() {
        val t = targets(5)
        val index = CoverageIndex(CoverageStamp("source", 1, 2, 3, false), CoverageIndexState.READY)
        val coverage = CoverageSnapshot(1, index, t.mapIndexed { i, target -> CoverageEntry(target,
            listOf(SourceCoverage.UNMAPPED, SourceCoverage.DECLARED_BROKEN, SourceCoverage.RESOLVABLE,
                SourceCoverage.UNKNOWN, SourceCoverage.SOURCE_UNAVAILABLE)[i], null, CoverageReason.NO_MAPPING) })
        val c = StyleTrialTargetSelector.candidates("EXTEND", t, t.map { it.key }, coverage)
        assertEquals(listOf("p0/Main", "p1/Main"), StyleTrialTargetSelector.recommend(c, kotlin.random.Random(42)))
        assertTrue(runCatching { StyleTrialTargetSelector.validateChoice(c, listOf("p2/Main"), false) }.isFailure)
        StyleTrialTargetSelector.validateChoice(c, listOf("p2/Main"), true)
        for (key in listOf("p3/Main", "p4/Main")) assertTrue(runCatching { StyleTrialTargetSelector.validateChoice(c, listOf(key), true) }.isFailure)
    }
    @Test fun lockManualAppliedUnknownUserAndDuplicateKeysFailClosed() {
        val t = listOf(CoverageTarget("lock/Main", "Lock", locked = true),
            CoverageTarget("manual/Main", "Manual", LocalIconState.CUSTOM_OR_MANUAL),
            CoverageTarget("applied/Main", "Applied", LocalIconState.OTHER_APPLIED),
            CoverageTarget("work/Main", "Work", ambiguousUser = true),
            CoverageTarget("unknown/Main", "Unknown", LocalIconState.UNKNOWN),
            CoverageTarget("dup/Main", "First"), CoverageTarget("dup/Main", "Second"))
        val c = StyleTrialTargetSelector.candidates("CREATE", t, t.map { it.key } + "removed/Main")
        assertTrue(c.none { it.eligible }); assertTrue(StyleTrialTargetSelector.recommend(c, kotlin.random.Random(42)).isEmpty())
        assertEquals(1, c.count { it.target.key == "dup/Main" })
    }
    @Test fun manualChoiceCanDifferFromSuggestionButCannotDuplicateOrExceedThree() {
        val t = targets(8); val c = StyleTrialTargetSelector.candidates("CREATE", t, t.map { it.key })
        StyleTrialTargetSelector.validateChoice(c, listOf("p1/Main", "p2/Main"), false)
        for (keys in listOf(emptyList(), listOf("p0/Main", "p0/Main"), t.take(4).map { it.key }, listOf("missing/Main")))
            assertTrue(runCatching { StyleTrialTargetSelector.validateChoice(c, keys, true) }.isFailure)
    }
    @Test fun fallbackMayBeRecommendedWithoutTreatingManualIconsAsMissing() {
        val t = listOf(CoverageTarget("fallback/Main", "Fallback", LocalIconState.FALLBACK))
        assertEquals(t.map { it.key }, StyleTrialTargetSelector.recommend(StyleTrialTargetSelector.candidates("CREATE", t, t.map { it.key })))
    }
}
