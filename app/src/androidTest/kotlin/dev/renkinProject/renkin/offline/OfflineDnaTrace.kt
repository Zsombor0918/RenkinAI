package dev.renkinProject.renkin.offline

import android.os.FileObserver
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import dev.renkinProject.renkin.ui.project.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap

/** Synthetic instrumentation only: no image bytes, SQL parameters, prompts or credentials. */
internal class OfflineDnaTrace(private val fixture: String) {
    private val sequence = AtomicLong()
    fun event(name: String, vararg fields: Pair<String, Any?>) {
        val thread = Thread.currentThread()
        val value = JSONObject().put("fixture", fixture).put("seq", sequence.incrementAndGet())
            .put("elapsed_ns", SystemClock.elapsedRealtimeNanos()).put("pid", android.os.Process.myPid())
            .put("thread", thread.name).put("thread_id", thread.id)
            .put("main", Looper.myLooper() == Looper.getMainLooper()).put("event", name)
        fields.forEach { (key, item) -> value.put(key, item ?: JSONObject.NULL) }
        Log.i("OfflineDnaTrace", value.toString())
    }

    fun trial(name: String, state: ProjectTrialState) = event(name,
        "busy" to state.busy, "network" to state.network, "preparation" to (state.preparation != null),
        "revision" to state.preparation?.context?.revision, "metadata_version" to state.preparation?.context?.metadataVersion,
        "plan" to state.plan?.id, "error" to state.error, "records" to state.records.size)

    fun trials(backend: ProjectTrialBackend, scope: CoroutineScope): ProjectTrialBackend {
        val jobs = ConcurrentHashMap.newKeySet<Int>()
        fun observeJobs(boundary: String) {
            scope.coroutineContext.job.children.forEach { job ->
                val id = System.identityHashCode(job)
                if (jobs.add(id)) {
                    event("scope.job.observed", "job" to id, "boundary" to boundary)
                    job.invokeOnCompletion {
                        event("scope.job.completed.after_finally", "job" to id, "cancelled" to job.isCancelled)
                        trial("trial.job.completed.snapshot", backend.state(1).value)
                    }
                }
            }
        }
        // Observe setters inline; never advance Compose, launch controller work or poll a timer.
        scope.launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
            backend.state(1).collect { trial("trial.state.observed", it); observeJobs("state") }
        }
        return object : ProjectTrialBackend by backend {
            override fun bindContext(id: Long, mode: ProjectSetupMode, source: String?) {
                trial("trial.bind.enter", backend.state(id).value)
                backend.bindContext(id, mode, source)
                trial("trial.bind.return", backend.state(id).value)
            }
            override fun load(id: Long, mode: ProjectSetupMode, source: String?) {
                trial("trial.load.enter", backend.state(id).value)
                backend.load(id, mode, source)
                observeJobs("load.return")
                trial("trial.load.return", backend.state(id).value)
            }
            override fun prepare(id: Long, keys: List<String>, allowCovered: Boolean) {
                event("trial.prepare.dispatched", "targets" to keys.size)
                trial("trial.prepare.enter", backend.state(id).value)
                backend.prepare(id, keys, allowCovered)
                observeJobs("prepare.return")
                trial("trial.prepare.return", backend.state(id).value)
            }
            override fun release(id: Long) {
                trial("trial.release.enter", backend.state(id).value)
                backend.release(id)
                trial("trial.release.return", backend.state(id).value)
            }
        }
    }

    /** Observe actual atomic journal writes/renames, never their contents. */
    fun journal(root: File): AutoCloseable {
        root.mkdirs()
        val observers = mutableListOf<FileObserver>()
        val watched = mutableSetOf<String>()
        fun watch(directory: File) {
            synchronized(observers) {
                if (!watched.add(directory.absolutePath)) return
                val observer = object : FileObserver(directory, CREATE or CLOSE_WRITE or MOVED_TO or MOVED_FROM or DELETE) {
                    override fun onEvent(event: Int, path: String?) {
                        this@OfflineDnaTrace.event("journal.fs", "mask" to event,
                            "entry" to directory.relativeTo(root).resolve(path.orEmpty()).path)
                        if (path != null && event and (CREATE or MOVED_TO) != 0) {
                            val child = File(directory, path)
                            if (child.isDirectory) watch(child)
                        }
                    }
                }
                observers += observer
                observer.startWatching()
                directory.listFiles()?.filter { it.isDirectory }?.forEach(::watch)
            }
        }
        watch(root)
        return AutoCloseable { synchronized(observers) { observers.forEach { it.stopWatching() } } }
    }
}
