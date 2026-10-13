package dev.renkinProject.renkin.ai.style

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.renkinProject.renkin.ai.AiCredentialStore
import dev.renkinProject.renkin.ai.AiSettings
import javax.inject.Inject
import javax.inject.Singleton

/** Shared transport/files, not another VM, profile session, icon store or database. */
@Singleton
class StyleAnalysisServices @Inject constructor(@ApplicationContext context: Context,
    private val credentials: AiCredentialStore) {
    internal val journal = StyleAnalysisJournal(context)
    internal val sampler = ReferenceSampler()
    internal val dnaJournal = StyleDnaJournal(context)
    internal val dnaCoordinator = StyleDnaCoordinator(dnaJournal, OpenAiStyleDnaGateway(credentials))
    internal val coordinator = StyleAnalysisCoordinator(journal, OpenAiStyleAnalysisGateway(credentials))
    internal fun hasOpenAiKey() = credentials.hasKey(AiSettings.OPENAI)
}
