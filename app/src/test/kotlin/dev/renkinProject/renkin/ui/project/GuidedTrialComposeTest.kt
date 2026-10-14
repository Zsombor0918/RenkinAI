package dev.renkinProject.renkin.ui.project

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import dev.renkinProject.renkin.ai.AiSettings
import dev.renkinProject.renkin.ai.trial.*
import dev.renkinProject.renkin.apk.CoverageTarget
import dev.renkinProject.renkin.data.project.*
import dev.renkinProject.renkin.ui.theme.RenkinTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.UUID

/** Actual taps and state changes, with synthetic artwork and no external provider. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33], qualifiers = "w320dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GuidedTrialComposeTest {
    @get:Rule val compose = createComposeRule()
    private val key = "very.long.application.package/LauncherActivity"
    private val appName = "A recognizable application with a long name"
    private val png = projectTestPng(size = 1024)
    private val hash = sha256(png)
    private val context = TrialContext(1, 1, 2, "CREATE", null, null, "a".repeat(64), "b".repeat(64),
        referenceFingerprint(emptyList()), "initial:initial", AiSettings.OPENAI, AiSettings.MODELS.first(), "medium",
        styleFingerprint("saved outline", ""))
    private fun variant(createdAt: Long, review: String, current: Boolean = true): IconAssetVariant {
        val id = UUID.randomUUID().toString()
        val input = if (current) context else context.copy(revision = 2)
        return IconAssetVariant(id, 1, key, input.revision, hash, hash, reviewStatus = review,
            createdAt = createdAt, trialContextJson = TrialVariantEvidence(input, UUID.randomUUID().toString(), id,
                "launcher:$key:1", emptyList(), if (review == "ACCEPTED") input else null,
                if (review == "ACCEPTED") "launcher:$key:1" else null).encode())
    }
    private inner class Backend(variants: List<IconAssetVariant> = emptyList()) : ProjectTrialBackend {
        val flow = MutableStateFlow(ProjectTrialState(preparation = TrialPreparation(context,
            SavedProjectStyle(ProjectAiMetadata(1, "CREATE", activeRevision = 1, version = 2),
                StyleRevision(1, 1, specification = "saved outline"), emptyList()),
            listOf(TrialCandidate(CoverageTarget(key, appName), true, true, false, "eligible"))),
            variants = variants, hasKey = true))
        var submissions = 0
        var preparedKeys: List<String> = emptyList()
        var loads = 0
        var localPreparation = flow.value.preparation!!
        override fun state(id: Long) = flow
        override fun load(id: Long, mode: ProjectSetupMode, source: String?) {
            loads++
            if (!flow.value.network) flow.value = flow.value.copy(preparation = localPreparation)
        }
        override fun options(id: Long, provider: String, model: String, quality: String) {
            flow.value = flow.value.copy(preparation = flow.value.preparation!!.let { p ->
                p.copy(context = p.context.copy(provider = provider, model = model, quality = quality))
            }, plan = null, ready = false)
            localPreparation = flow.value.preparation!!
        }
        override fun prepare(id: Long, keys: List<String>, allowCovered: Boolean) {
            val p = flow.value.preparation!!
            StyleTrialTargetSelector.validateChoice(p.candidates, keys, allowCovered)
            preparedKeys = keys
            flow.value = flow.value.copy(plan = StyleTrialPlan(p.context, p.style.revision!!.specification,
                p.style.revision.userInstruction, emptyList(), keys.map { TrialInput(it, appName, hash, "launcher:$it:1") }))
        }
        override fun submit(id: Long, planId: String) { submissions++ }
        override fun cancelPreparation(id: Long) { flow.value = flow.value.copy(plan = null) }
        override fun stop(id: Long) {}
        override fun release(id: Long) {}
        override fun review(id: Long, variantId: String, accepted: Boolean) {
            flow.value = flow.value.copy(variants = flow.value.variants.map { row ->
                if (row.id != variantId) row else row.copy(reviewStatus = if (accepted) "ACCEPTED" else "REJECTED",
                    trialContextJson = TrialVariantEvidence.parse(row.trialContextJson!!).copy(reviewContext = flow.value.preparation!!.context,
                        reviewOriginalOrigin = "launcher:$key:1").encode())
            })
        }
        override fun approve(id: Long) { flow.value = flow.value.copy(ready = true) }
        override suspend fun preview(id: Long, hash: String) = png
    }
    private fun show(backend: Backend, guided: Boolean = false, fontScale: Float = 1f, onComplete: (() -> Unit)? = null) {
        compose.setContent { RenkinTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ProjectTrialPanel(1, "Project", ProjectSetupMode.valueOf(backend.localPreparation.context.mode),
                    backend.localPreparation.context.sourcePackage, true, backend, {}, {}, {},
                    onComplete = onComplete, guided = guided)
            }
        } }
    }
    private fun click(tag: String) {
        compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performClick()
    }

    @Test fun recognizableTargetsKeepComponentAndProviderOptionsInExplicitDetails() {
        val backend = Backend(); show(backend)
        compose.onNodeWithText(key).assertDoesNotExist()
        compose.onNodeWithTag("trial-model-${context.model}").assertDoesNotExist()
        click("trial-target-details-$key")
        compose.onNodeWithText(key).assertExists()
        click("trial-options-toggle")
        compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag("trial-model-${context.model}"))
        compose.onNodeWithTag("trial-model-${context.model}").assertExists()
        assertEquals(0, backend.submissions)
    }

    @Test fun latestResultUsesAppNameAndReadableReviewWithPreservedExpandableHistory() {
        val old = variant(100, "ACCEPTED")
        val latest = variant(200, "UNREVIEWED")
        val stale = variant(300, "REJECTED", current = false)
        val backend = Backend(listOf(old, latest, stale)); show(backend)
        compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag("trial-current-results"))
        compose.onNodeWithTag("trial-current-results").assertExists()
        compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag("trial-variant-${latest.id}"))
        compose.onNodeWithTag("trial-variant-${latest.id}").assert(hasAnyDescendant(hasText(appName)))
        compose.onNodeWithText("UNREVIEWED", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("trial-variant-${old.id}").assertDoesNotExist()
        click("trial-history-toggle")
        compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag("trial-variant-${old.id}"))
        compose.onNodeWithTag("trial-variant-${old.id}").assertExists()
        compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag("trial-accept-${stale.id}"))
        compose.onNodeWithTag("trial-accept-${stale.id}").assertIsNotEnabled()
        assertEquals(3, backend.flow.value.variants.size)
        assertEquals(0, backend.submissions)
    }

    @Test fun unreviewedPreviousVariantStillBlocksApprovalUntilExplicitReview() {
        val pending = variant(100, "UNREVIEWED")
        val accepted = variant(200, "ACCEPTED")
        val backend = Backend(listOf(pending, accepted)); show(backend)
        compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag("trial-approve-style"))
        compose.onNodeWithTag("trial-approve-style").assertIsNotEnabled()
        compose.onNodeWithTag("trial-approval-reason").assertExists()
        click("trial-history-toggle")
        click("trial-reject-${pending.id}")
        compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag("trial-approve-style"))
        compose.onNodeWithTag("trial-approve-style").assertIsEnabled().performClick()
        assertEquals(true, backend.flow.value.ready)
        assertEquals(2, backend.flow.value.variants.size)
        assertEquals(0, backend.submissions)
    }

    @Test fun changingProviderOptionsKeepsTheExplicitTrialTargetChoiceWithoutSubmitting() {
        val backend = Backend(); show(backend)
        click("trial-target-$key")
        compose.onNodeWithTag("trial-target-$key").assertIsOff()
        compose.runOnIdle { backend.options(1, AiSettings.OPENAI, context.model, "high") }
        compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag("trial-target-$key"))
        compose.onNodeWithTag("trial-target-$key").assertIsOff()
        assertEquals(0, backend.submissions)
    }

    @Test fun confirmedRequestCompletionRechecksChangedLocalInputsWithoutResubmitting() {
        val backend = Backend(); show(backend)
        compose.waitForIdle()
        val initialLoads = backend.loads
        compose.runOnIdle { backend.flow.value = backend.flow.value.copy(busy = true, network = true) }
        compose.waitForIdle()
        compose.runOnIdle {
            backend.localPreparation = backend.localPreparation.copy(context = context.copy(catalogStamp = "c".repeat(64)))
            backend.flow.value = backend.flow.value.copy(busy = false, network = false)
        }
        compose.waitUntil(10_000) { backend.flow.value.preparation?.context?.catalogStamp == "c".repeat(64) }
        assertEquals(initialLoads + 1, backend.loads)
        assertEquals(0, backend.submissions)
    }

    @Test fun currentVerificationErrorCannotExposePreviousReadyOrApproval() {
        val backend = Backend(listOf(variant(100, "ACCEPTED"))); show(backend)
        compose.runOnIdle { backend.flow.value = backend.flow.value.copy(ready = true, error = "unverified_original") }
        compose.onNodeWithTag("trial-ready").assertDoesNotExist()
        compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag("trial-approve-style"))
        compose.onNodeWithTag("trial-approve-style").assertIsNotEnabled()
        compose.onNodeWithTag("trial-approval-reason").assertExists()
        assertEquals(0, backend.submissions)
    }

    @Test @Config(qualifiers = "w320dp-h800dp-night")
    fun guidedSavedTargetsHaveNamesWithoutASecondSelectorAtLargeFontScale() {
        val backend = Backend(); show(backend, guided = true, fontScale = 1.5f)
        compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag("trial-target-$key"))
        compose.onNodeWithText(appName).assertExists()
        assertEquals(0, compose.onAllNodes(isToggleable()).fetchSemanticsNodes().size)
        click("trial-prepare")
        assertEquals(listOf(key), backend.preparedKeys)
        compose.onNodeWithTag("trial-frozen-confirmation").assertExists()
        compose.onNodeWithTag("trial-decline").performClick()
        assertEquals(0, backend.submissions)
    }

    @Test fun guidedCoveredTargetsRequireTheExistingExplicitOverrideBeforeLocalPreparation() {
        val backend = Backend()
        backend.localPreparation = backend.localPreparation.copy(context = context.copy(mode = "EXTEND",
            sourcePackage = "source.pack", sourceStamp = "c".repeat(64)), candidates = backend.localPreparation.candidates.map {
            it.copy(recommended = false, overrideRequired = true, reason = "covered_override")
        })
        backend.flow.value = backend.flow.value.copy(preparation = backend.localPreparation)
        show(backend, guided = true)
        compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag("trial-prepare"))
        compose.onNodeWithTag("trial-prepare").assertIsNotEnabled()
        click("trial-covered-override")
        click("trial-prepare")
        assertEquals(listOf(key), backend.preparedKeys)
        compose.onNodeWithTag("trial-decline").performClick()
        assertEquals(0, backend.submissions)
    }

    @Test fun projectOverviewAndApprovedCompletionRemainSeparateFromPaidGeneration() {
        val backend = Backend(listOf(variant(100, "ACCEPTED"))); var overviews = 0
        show(backend, guided = true, onComplete = { overviews++ })
        click("trial-overview")
        assertEquals(1, overviews)
        assertEquals(false, backend.flow.value.ready)
        compose.onNodeWithTag("trial-complete").assertDoesNotExist()
        click("trial-approve-style")
        click("trial-complete")
        assertEquals(2, overviews)
        assertEquals(true, backend.flow.value.ready)
        assertEquals(0, backend.submissions)
    }

    @Test fun savedCoveredTrialCanBeRejectedLocallyWhileRegenerationStillNeedsOverride() {
        val coveredContext = context.copy(mode = "EXTEND", sourcePackage = "source.pack", sourceStamp = "c".repeat(64))
        val saved = variant(100, "ACCEPTED").let { row -> row.copy(trialContextJson = TrialVariantEvidence.parse(row.trialContextJson!!)
            .copy(context = coveredContext, reviewContext = coveredContext).encode()) }
        val backend = Backend(listOf(saved))
        backend.localPreparation = backend.localPreparation.copy(context = coveredContext, candidates = backend.localPreparation.candidates.map {
            it.copy(recommended = false, overrideRequired = true, reason = "covered_override")
        })
        backend.flow.value = backend.flow.value.copy(preparation = backend.localPreparation)
        show(backend, guided = true)
        compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag("trial-reject-${saved.id}"))
        compose.onNodeWithTag("trial-reject-${saved.id}").assertIsEnabled().performClick()
        assertEquals("REJECTED", backend.flow.value.variants.single().reviewStatus)
        compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag("trial-regenerate-${saved.id}"))
        compose.onNodeWithTag("trial-regenerate-${saved.id}").assertIsNotEnabled()
        assertEquals(0, backend.submissions)
    }

    @Test fun latestFailedRequestHasAnActionableSummaryWithoutRemovingSavedResults() {
        val saved = variant(100, "ACCEPTED")
        val backend = Backend(listOf(saved))
        val plan = StyleTrialPlan(context, "saved outline", "", emptyList(), listOf(TrialInput(key, appName, hash, "launcher:$key:1")))
        backend.flow.value = backend.flow.value.copy(records = listOf(StyleTrialRecord(plan, 200, true,
            listOf(TrialItemRecord(plan.inputs.single().requestId, TrialStatus.FAILED, endpointCalls = 1, reason = "model_unavailable")))))
        show(backend, guided = true)
        compose.onNodeWithTag("trial-latest-failure").assertTextContains("model is unavailable", substring = true)
        compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag("trial-variant-${saved.id}"))
        compose.onNodeWithTag("trial-variant-${saved.id}").assertExists()
        assertEquals(1, backend.flow.value.variants.size)
        assertEquals(1, backend.flow.value.records.size)
        assertEquals(0, backend.submissions)
    }
}
