package com.slmlab.app.data

/** Shared data types. Contract file: do not change without updating DESIGN.md. */

enum class LessonKind { ON_DEVICE, GUIDED }

data class LessonMeta(
    val id: String,
    val title: String,
    val summary: String,
    val minutes: Int,
    val kind: LessonKind,
    /** Asset path of the lesson markdown, e.g. "lessons/p1-01.md". */
    val contentAsset: String,
    /** Asset path of the lab source, e.g. "labs/lab01_bigram.py", or null. */
    val labAsset: String?,
    val checklist: List<String>,
)

data class Phase(
    val id: String,
    val title: String,
    val subtitle: String,
    val weeks: String,
    val lessons: List<LessonMeta>,
)

data class Curriculum(val phases: List<Phase>) {
    /** All lessons in path order. */
    val allLessons: List<LessonMeta> get() = phases.flatMap { it.lessons }

    fun lesson(id: String): LessonMeta? = allLessons.firstOrNull { it.id == id }

    /** The lesson after [id] in path order, or null if [id] is last or unknown. */
    fun nextAfter(id: String): LessonMeta? {
        val all = allLessons
        val i = all.indexOfFirst { it.id == id }
        return if (i >= 0 && i + 1 < all.size) all[i + 1] else null
    }
}

data class GlossaryTerm(val term: String, val definition: String)

/** One point of a metric series drawn by LossChart. */
data class MetricPoint(val step: Int, val value: Float)

/** Events emitted while Python code runs. */
sealed interface RunEvent {
    data class Output(val text: String) : RunEvent
    data class Metric(val name: String, val step: Int, val value: Float) : RunEvent
    /** Always the last event of a run. [error] is null when [ok] is true. */
    data class Finished(val ok: Boolean, val error: String?) : RunEvent
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val editorFontSp: Int = 13,
)
