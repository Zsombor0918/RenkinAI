package dev.renkinProject.renkin.ai.trial

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.renkinProject.renkin.ai.AiCredentialStore
import dev.renkinProject.renkin.ai.ClientIconGenerationGateway
import dev.renkinProject.renkin.data.project.ProjectAiRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StyleTrialServices @Inject constructor(@ApplicationContext context: Context,
    private val credentials: AiCredentialStore, gateway: ClientIconGenerationGateway,
    repository: ProjectAiRepository) {
    internal val journal = StyleTrialJournal(context)
    internal val coordinator = StyleTrialCoordinator(journal, gateway, repository)
    internal fun hasKey(provider: String) = credentials.hasKey(provider)
}
