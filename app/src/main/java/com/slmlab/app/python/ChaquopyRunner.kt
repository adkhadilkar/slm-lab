package com.slmlab.app.python

import com.chaquo.python.Python
import com.slmlab.app.data.PyListener
import com.slmlab.app.data.PyRunner
import com.slmlab.app.data.RunEvent
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

/** Runs lab code on the embedded Chaquopy interpreter (DESIGN.md section 8). */
class ChaquopyRunner : PyRunner {

    private val running = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = running.asStateFlow()

    override fun run(code: String): Flow<RunEvent> = callbackFlow {
        if (!running.compareAndSet(false, true)) {
            // Another run owns the interpreter. Do not stop it from here.
            trySend(RunEvent.Finished(false, "Already running"))
            close()
            awaitClose()
        } else {
            val finished = AtomicBoolean(false)
            val finishedEmitted = AtomicBoolean(false)
            val listener = PyListener { event ->
                if (event is RunEvent.Finished) finishedEmitted.set(true)
                trySend(event)
            }

            val thread = Thread({
                try {
                    Python.getInstance().getModule(MODULE).callAttr("run", code, listener)
                } catch (t: Throwable) {
                    if (!finishedEmitted.get()) {
                        trySend(RunEvent.Finished(false, t.message ?: t.javaClass.simpleName))
                    }
                } finally {
                    finished.set(true)
                    running.value = false
                    close()
                }
            }, "py-run")
            thread.isDaemon = true
            thread.start()

            awaitClose {
                if (!finished.get()) stop()
            }
        }
    }

    override fun stop() {
        if (!running.value) return
        runCatching {
            Python.getInstance().getModule(MODULE).callAttr("stop")
        }
    }

    private companion object {
        const val MODULE = "slm_runner"
    }
}
