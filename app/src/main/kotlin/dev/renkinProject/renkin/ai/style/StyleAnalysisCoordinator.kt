package dev.renkinProject.renkin.ai.style

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Only the caller's explicit confirmed action enters this method. The durable claim is
 * authoritative even across coordinator reconstruction, cancellation and repeat UUIDs. */
internal class StyleAnalysisCoordinator(private val journal: StyleAnalysisJournal, private val gateway: StyleAnalysisGateway) {
    suspend fun submit(request: StyleAnalysisRequest, confirmed: Boolean,
        contextCurrent: suspend (AnalysisContext) -> Boolean): AnalysisRecord? = withContext(Dispatchers.IO) {
        if (!confirmed) return@withContext null
        request.validate()
        if (!contextCurrent(request.context)) return@withContext null
        if (!journal.claim(request)) return@withContext journal.read(request.context.profileId, request.requestId)
        try {
            val outcome = gateway.analyze(request)
            // Save validated result before publishing any UI completion, including an inactive owner.
            withContext(NonCancellable) { journal.finish(request.context.profileId, request.requestId, outcome) }
        } catch (cancel: CancellationException) {
            withContext(NonCancellable) { journal.interrupted(request.context.profileId, request.requestId) }; throw cancel
        } catch (failure: Exception) {
            withContext(NonCancellable) { journal.interrupted(request.context.profileId, request.requestId) }; throw failure
        } finally { journal.release(request.context.profileId, request.requestId) }
    }
}
