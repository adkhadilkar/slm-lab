package com.slmlab.app.data

import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Contract file: interfaces shared by all Kotlin code. Do not change without updating DESIGN.md. */

/** Reads bundled content from Android assets. All functions are safe to call from the main thread. */
interface CurriculumRepository {
    /** Parsed assets/curriculum.json. Cached after the first call. */
    suspend fun curriculum(): Curriculum

    /** Text of a lesson markdown asset, e.g. "lessons/p1-01.md". */
    suspend fun lessonMarkdown(contentAsset: String): String

    /** Text of a lab source asset, e.g. "labs/lab01_bigram.py". */
    suspend fun labSource(labAsset: String): String

    /** Parsed assets/glossary.json, sorted by term (case-insensitive). */
    suspend fun glossary(): List<GlossaryTerm>
}

/** Progress, settings and saved lab code. */
interface ProgressStore {
    /** Ids of completed lessons. */
    val completedLessons: Flow<Set<String>>

    /** Ticked checklist items as "<lessonId>#<index>". */
    val checkedItems: Flow<Set<String>>

    val settings: Flow<AppSettings>

    suspend fun setLessonCompleted(lessonId: String, completed: Boolean)
    suspend fun setChecked(lessonId: String, index: Int, checked: Boolean)

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setEditorFontSp(sp: Int)

    /** The learner's edited code for a lab, or null if they have not edited it. */
    suspend fun savedCode(lessonId: String): String?
    suspend fun saveCode(lessonId: String, code: String)
    suspend fun clearCode(lessonId: String)

    /** Clears progress, settings, saved code and trained weights. */
    suspend fun resetAll()

    companion object {
        fun checkKey(lessonId: String, index: Int): String = "$lessonId#$index"
    }
}

/** Runs Python source on the embedded interpreter. Only one run at a time. */
interface PyRunner {
    /**
     * Cold flow: collecting it starts the run. Emits Output and Metric events and ends
     * with exactly one Finished event, after which the flow completes. Cancelling the
     * collector stops the run.
     */
    fun run(code: String): Flow<RunEvent>

    /** Asks the running code to stop. No-op when nothing is running. */
    fun stop()

    val isRunning: StateFlow<Boolean>
}

/**
 * Passed to Python (slm_runner.run). Python calls these three methods by name, so the
 * method names and parameter types must not change.
 */
class PyListener(private val emit: (RunEvent) -> Unit) {
    fun onOutput(text: String) {
        emit(RunEvent.Output(text))
    }

    fun onMetric(name: String, step: Int, value: Double) {
        emit(RunEvent.Metric(name, step, value.toFloat()))
    }

    fun onFinished(ok: Boolean, error: String?) {
        emit(RunEvent.Finished(ok, if (ok || error.isNullOrEmpty()) null else error))
    }
}

/** Directories under filesDir. */
class AppPaths(filesDir: File) {
    val pylibDir: File = File(filesDir, "pylib")
    val dataDir: File = File(filesDir, "data")
    val workDir: File = File(filesDir, "work")
    val codeDir: File = File(filesDir, "code")

    fun ensure() {
        listOf(pylibDir, dataDir, workDir, codeDir).forEach { it.mkdirs() }
    }
}

/** Manual dependency container. Fields are set once in SlmLabApp.onCreate. */
object AppContainer {
    lateinit var paths: AppPaths
    lateinit var repo: CurriculumRepository
    lateinit var progress: ProgressStore
    lateinit var runner: PyRunner
}
