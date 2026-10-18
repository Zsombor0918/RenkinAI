# Phase 0 retirement inventory

Baseline: `7db94f1eaa008e9c9b71d2af775f66c59f45d107`. Owner authorization: October18,2026 Phase0–3 request and supplied product vision. This checkpoint implements **Phase0 only**. Montages, new analysis and montage Trials remain unimplemented.

## Dependency classification

| Class | Retained boundary and reason |
| --- | --- |
| A | Original editor/mapping/modifiers, pack/build/export, APK/signing remain unchanged. |
| B | AI1/AI2/Global Batch; secure image clients; project/assets/Room21, migrations19–21, backup/profile handling; original resolver and Trial controller/coordinator/journal remain active. |
| C | Coverage reader, ReferenceSampler, ReferenceImages, PackStyleEvidence, LocalStyleReferences and private PNG assets/preview are reused local prerequisites. Phase0 retains the working manual/free style fallback. |
| D | StyleProfileV1/StyleDnaV1 codecs, SavedStyleDna, ReviewedAnalysisEvidence, StyleDnaPlan version/fingerprint reconstruction, StyleDnaCompiler/derived recipe readers, analysis journals/epochs/consumed markers remain. BackupManager calls invalidateAll; ProfileManager calls invalidateProfile. Historical TrialV1/V2 effective text remains immutable. |
| E | Legacy/Quick/Standard provider transports, coordinators, services, owned controller, analysis backend/UI, runtime local synthesis and outbound schema constructors have no active callers after removal. |

## Removed production files

- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysis.kt`: 390 lines.
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectAnalysisBackend.kt`: 56 lines.
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleDnaPanel.kt`: 415 lines.
- `app/src/main/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleAnalysisPanel.kt`: 307 lines.
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisServices.kt`: 20 lines.
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisCoordinator.kt`: 27 lines.
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaCoordinator.kt`: 58 lines.
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/OpenAiStyleAnalysisGateway.kt`: 156 lines.
- `app/src/main/kotlin/dev/renkinProject/renkin/ai/style/OpenAiStyleDnaGateway.kt`: 153 lines.

The nine removed runtime files contained1582 lines. Additional obsolete synthesis and outbound schema builders were removed from retained files; their encoders/strict readers were not removed.150 unreferenced localized strings were removed. The shared PNG thumbnail and owner-lock port were extracted before deleting analysis UI/controller dependencies. There is no replacement cloud pipeline in this checkpoint.

The active DNA family/type picker was also removed. Historical styles continue to use their already-saved selected family and bound reference assets. Frozen plans retain previous bytes/versions and consumed identities.

## Test identities removed or replaced

Source comparison finds111 old JUnit method identities removed/replaced and7 replacement identities, a net reduction of104. Baseline hosted XML contains1026 cases; expected new inventory is922, to be confirmed by actual Linux XML. This is deliberate feature retirement, not a successful-test claim.

Whole retired classes tested obsolete transports/controller/analysis UI. Mixed classes retain saved-data, backup, source/owner, immutable-request, HTTP/image ordering and consent/replay protections. Four coordinator-backed journal cases became direct archived-journal tests; two obsolete cloud-consent-only cases were removed because active Trial consent/replay remains covered. The archived-DNA-to-image HTTP journey now seeds saved data directly instead of making an obsolete analysis request.

### `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisGatewayTest.kt`

- `analysisDescribesColorUsageAndGeometryWithoutCopyingSampleIdentities`
- `authenticationModelRateLimitsAndServerFailuresAreTypedAndNeverRetry`
- `cancellationStopsOneRealHttpCallWithoutReconnectingOrRetrying`
- `invalidUtf8OversizedBodyTimeoutAndRedirectAreNotReplayed`
- `malformedInputsOversizedImagesDuplicatesUnknownModelAndNoKeyCauseZeroPosts`
- `quotaCodeIsAllowlistedWithoutRetainingSensitiveErrorProse`
- `realResponsesAdapterSendsBoundedOrderedImagesStrictTextSchemaAndNoLocalIdentity`
- `refusalMalformedSchemaIncompleteAndMissingUsageDoNotInventSuccessOrCost`
- `safeGoldenLowHighComparisonChangesOnlyDetailAndProductionRejectsHighWithoutConsentChange`
- `serviceRetryAfterZeroCannotReplayOneBillableAnalysisBody`

### `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisJournalTest.kt`

- `cancellationAndColdRecoveryAreUncertainAndNeverAutomaticallyResend`
- `concurrentDoubleTapAndReconstructedCoordinatorSubmitExactlyOnce`
- `durableRequestingPrecedesCallAndResultReloadDoesNotRebillSameConsent`
- `explicitRetryNeedsFreshRequestIdentityAndFreshConfirmation`
- `markerFailureAndCorruptedRecordFailClosedBeforeAnyPost`
- `noConsentAndStaleContextWriteNoFilesAndCallNoGateway`

### `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisSchemaTest.kt`

- `incompleteRefusedTruncatedAndUnexpectedToolOutputNeverBecomeStyle`
- `strictSupportedSchemaRequiresAllKeysAndNoAdditionalProperties`
- `wireSchemaConstrainsEveryListToTheSameSixItemsAsTheStrictLocalParser`

### `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaGatewayTest.kt`

- `allRejectionsAndRetryAfter503HaveExactlyOneSubmissionAndNoFallback`
- `boundedBodyFaultsAndUnavailableCredentialNeverImplyAValidStyleOrAutomaticRetry`
- `completedEnvelopeAllowsMetadataReasoningOrderAndSplitTextButRejectsToolsAndBadJson`
- `documentedIncompleteAndFailedResponsesHaveDistinctSafeCodesWithoutReplay`
- `frozenReportSchemaMatchesNonemptyTextExactCountAndAllowedBindings`
- `generationUsefulPromptIsVersionedAndKeepsV1PlansAndEvidenceReadable`
- `realWireHasIndividuallyLabeledImagesStrictReportsAndNoLocalIdentity`
- `refusalMalformedPartialOversizeAndTimeoutDoNotResubmit`
- `reportFaultsUseFixedCategoriesWhileReorderingAndUnknownRemainValid`
- `returnedReportFormatSchemaExceedsOldEnvelopeDepthButSixReportsAreValid`
- `scopedSchemaDepthAndFixedEnvelopeReasonsKeepEveryRejectionAndOneShotMarker`
- `synthesisUploadsVerifiedReportsAndMeasuredTextWithZeroImageParts`

### `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaIncompleteReasonTest.kt`

- `absentUnknownOrWrongTypedReasonBecomesUnknownWithoutProviderProse`
- `contentFilterIsDistinctFromOutputLimitAndRetainsBillingUncertainty`
- `malformedIncompleteEnvelopeStillUsesStrictJsonBounds`
- `oldAndNewFixedReasonsRoundTripWithoutRewritingSchemaOrConsumedMarkers`
- `outputLimitIsPreservedAsFixedReasonWithoutAcceptingPartialOutput`

### `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaOfflineHttpTest.kt`

- `disconnectTimeoutAndExplicitCancelRemainUncertainAndConsumedAfterNewRepository`
- `httpFailuresCannotRewriteSavedStylesOrRetryEvenWithRetryAfterZero`
- `malformedRefusedAndBoundedBodiesFailThroughActualHttpAndKeepSavedRoomState`
- `projectSourceAndSavedStyleChangesInvalidateConsentBeforeAnyNetwork`
- `quickAndStandardIncompleteReasonsRetainConsumptionAndManualStyleAfterDiskReopen`
- `quickTwelveAndStandardTwentyFourPersistAcrossClosedRoomAndContinueWithoutReplay`
- `thirdStandardFailureKeepsExactlyTwelveImmutableReportsAndNeverStartsFourthOrSynthesis`
- `verboseTwelveReportsAtEveryObservationBoundStillUseOneUnchangedBudgetedPost`

### `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleDnaWorkflowTest.kt`

- `completedHttpReportSchemaAndSynthesisFailuresKeepDistinctStagesAndConsumedRights`
- `partialSixImageFailureRetainsOnlyCompletedReportsAndCannotResumeOrReplay`
- `quickHasOneSubmissionAndLocalSynthesisWithoutSilentSecondPass`
- `sixOfflineGoldenPackCasesKeepPoliciesFamiliesAndHeldOutSourceSymbolsSeparate`
- `staleContextAndMarkerFailureSendNothingAndFreshConsentIsRequired`
- `standardHasFourSixImageReadsThenOneIndependentSynthesisWithDurableSteps`
- `thirdStandardBatchCountFailureRetainsTwelveReportsAndSpecificReasonWithoutReplay`

### `app/src/test/kotlin/dev/renkinProject/renkin/data/project/StyleDnaPersistenceTest.kt`

- `actualOwnedStandardFlowNeedsConsentAndExplicitLocalSaveAndStopsOnSourceChange`
- `quickAndStandardUseRealResponsesWireThenReviewSaveAndReloadWithoutReplay`
- `validFullEnvelopeWithInvalidReportKeepsFirstStepConsumedWithoutSaveOrReplay`

### `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/GuidedProjectRoomJourneyComposeTest.kt`

- `firstQuickScanWaitsForVerifiedSourceAndRecreationNeverSubmits`

### `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleAnalysisTest.kt`

- `cancellingConfirmedDnaKeepsUncertainHistoryAndCannotReplayAfterRecreation`
- `changedMetadataSelectionOrCatalogCannotApplyAnOlderResult`
- `controllerRecreationAndDiscardNeverResendAndKeepActiveStyle`
- `corruptSharedHistoryFailsClosedAcrossPacksButReportsHistoryInsteadOfDiskSpace`
- `delayedRefreshCannotReplaceSubmittedProgressOrRestoreAConsumedPlan`
- `dnaHttpAuthSchemaAndTimeoutKeepConsumedMarkersAndNeverReplay`
- `firstQuickScanWithoutSetupStampReportsPrerequisiteAndRetryIsLocal`
- `freshCatalogIsRecheckedBetweenUseAndStyleSave`
- `prepareAndReopenAreLocalAndMissingKeyDoesNotBlockManualSave`
- `profileSwitchDuringCloudResponseKeepsCandidateOwnedAndBlocksForeignUse`
- `quickPreparationDiagnosesStaleEmptyAndStorageWithoutLeakingExceptionText`
- `readyQuickAndStandardPlansSurvivePreviewReleaseAndKeyRefreshButCannotSubmitToAnotherRoute`
- `refreshOverlappingLocalFreezeCannotLoseTheNewPlanOrRestoreBusyState`
- `rejectedAnalysisKeepsSafeReasonAndNeedsFreshPreparationUuidAndExplicitConfirmation`
- `restoreEpochBlocksCandidatesEvenWhenProfileAndMetadataIdsMatch`
- `sameProfileModeOrChosenSourceChangeBlocksConsentUseAndSave`
- `significantManualOverrideIsUserOriginButRetainsAnalysisEvidence`
- `sourceIoAndPermissionFailuresExposeOnlyFixedStageCodesAndRetryFreely`
- `sourceOnlyChangeRequiresExplicitReviewAndAnotherUpdateBlocksSaveAgain`
- `staleDnaHistoryDoesNotInvalidateACurrentLegacyCandidate`
- `staleSavedRevisionAndMoreThanFiveRefsFailWithoutOverwritingManualStyle`
- `trustedContextAliasPreparesBothModesAcrossPacksWithoutChangingHistoryOrRoom`
- `useChangesOnlyDraftAndExplicitSavePreservesInstructionAndImmutableHistory`

### `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrialsTest.kt`

- `dnaFamilyReferenceMismatchBlocksAndChangedChoiceInvalidatesFrozenOriginalFirstRequest`

### `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleAnalysisComposeTest.kt`

- `cancellationUncertaintyAndBackKeepManualStyleAndNeverResubmit`
- `changedDraftRequiresFreshPreparationBeforePaidConfirmation`
- `createNeedsNoPackAndManualSaveWithoutKeyStillWorks`
- `dirtyDraftKeepThenUsePreservesInstructionAndSaveIsSeparate`
- `emptyOwnReferencesShowFallbackAndDoNotSend`
- `extendShowsTwelveLocalSamplesAndAdvancedLimitsAndOrdersFiveGenerationRefs`
- `readAndSaveFailuresRemainVisibleWithManualDraftKept`
- `realConsentDeclineThenConfirmSubmitsOnceAndResultNeverAutosaves`
- `restoredResultAndRecompositionKeepCandidateWithoutAnotherPostOrSave`
- `sameProfileCandidateFromDifferentModeOrSourceCannotBeConfirmedOrUsed`
- `sourceAndPreparationStalenessAreVisibleAndOnlySourceCanBeReviewed`

### `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleGuidedFlowComposeTest.kt`

- `delayedReloadOfTheSameDnaCandidateKeepsItsEditedGrammar`
- `failedDnaSaveKeepsCandidateAndNeverAdvancesOrClaimsPersistentSuccess`
- `freshProjectDefaultsToFreeReferencesAndAnalysisRequiresExplicitMethodSelection`
- `guidedLegacyCandidateSavesOnceWhileCurrentFailureRemainsSeparateFromHistory`
- `immutableDnaSaveFinishesStyleWithoutASecondManualSave`
- `pendingDnaSaveCannotClaimSavedOrEnableNextStep`
- `resavingDnaBlocksParentActionsUntilTheNewRevisionIsDurable`
- `sourceFreeManualCreateSavesAndContinuesWithoutAKeyOrAnySubmission`

### `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleTrialJourneyComposeTest.kt`

- `extendTargetsThenTwelveSampleAnalysisUseSaveAndReopenReachesTrialsWithFiveRefs`
- `rejectedAnalysisExplainsConsumedConsentAndManualStyleStillReachesTrials`

### `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/StyleDnaComposeTest.kt`

- `changedOptionsExplainWhyFrozenPlanNeedsFreshPreparation`
- `changingFamilySelectsOnlyItsActualReferenceImagesBeforeReviewAndSave`
- `freshPlanIsReadyWhileFailedOrUncertainHistoryStaysUnderDetails`
- `historyFailurePreservesRequestGateAndDisplaysOnlyBoundedDiagnostic`
- `missingExtendStampExplainsDisabledScanAndManualPathStaysAvailable`
- `previewSamplesWithoutCurrentPlanKeepAmbiguousOldFailureOnlyInDetails`
- `recordedIncompleteReasonsExplainOnlyFixedCauseWithoutChangingHistoryOrSubmitting`
- `restorationAndDarkNarrowUiNeverSubmitAndDeepRemainsDisabled`
- `settingsKeyChangesRefreshOnResumeWithoutRepreparingOrSubmitting`
- `sourceChangeWhileConfirmationOpenDisablesOldFrozenConsent`
- `standardConsentListsFiveStepsAndSavingRequiresSeparateHumanReview`

## Replacement identities

- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisJournalTest.kt`: `historicalInterruptedAndColdRecordsCannotBeClaimedAgain`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisJournalTest.kt`: `historicalJournalClaimsReserveAnIdentityBeforeReopening`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisJournalTest.kt`: `historicalRequestMarkerAndSavedResultReloadCannotReclaimConsent`
- `app/src/test/kotlin/dev/renkinProject/renkin/ai/style/StyleAnalysisJournalTest.kt`: `markerFailureAndCorruptedHistoricalRecordFailClosed`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/OwnedStyleTrialsTest.kt`: `historicalSavedFamilyUsesItsBoundReferencesAtTheOriginalFirstRequestBoundary`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleGuidedFlowComposeTest.kt`: `freshManualStyleHasNoRetiredAnalysisModesAndSavesWithoutAKey`
- `app/src/test/kotlin/dev/renkinProject/renkin/ui/project/ProjectStyleGuidedFlowComposeTest.kt`: `historicalSavedTextAndOrderedAssetsSurviveRestoreAndPendingManualSave`

## Shared protections retained

Existing tests cover original-first PNG bytes and ordered hashes at the real multipart boundary; V1 golden bytes/fingerprints and V2 prompt hashes; request endpoint accounting, no replay, cancellation and uncertainty; selected source/target changes, queued-load retention and decline; real Room/schema/migration/full/shared backup; independent manual CREATE and EXTEND Save; editor/assets, history, profiles, modifier and export behavior. No safety assertion is disabled to reduce runtime.

The Android harness deliberately retires Quick/Standard/incomplete-analysis journeys. Its active replacement is2 write/preparation journeys (free reference Save and explicit Trial decline) plus1 independent-process restart. The runner still rejects physical devices, mismatched test counts, missing evidence, wrong source SHA and invalid counters. Unit HTTP tests still exercise actual loopback image requests; Android performs no provider submission.

## Remaining product work

Phase1 must replace the fallback1–5-reference interaction with20–30 source icons and durable detailed montages. Phase2 must add one separately consented montage analysis. Phase3 must add stable random eligible targets, actual original+montage payload/version and logical Back navigation. No montage dimensions, new analysis prompt or montage payload is claimed here because none is implemented yet. No live/provider/phone/key/signing action was performed.
