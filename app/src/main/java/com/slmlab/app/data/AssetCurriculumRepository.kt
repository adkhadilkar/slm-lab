package com.slmlab.app.data

import android.content.Context
import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Reads curriculum, lesson, lab and glossary content from the APK assets (DESIGN.md 3.4). */
class AssetCurriculumRepository(context: Context) : CurriculumRepository {

    private val assets: AssetManager = context.applicationContext.assets
    private val curriculumLock = Mutex()
    private val glossaryLock = Mutex()
    private var cachedCurriculum: Curriculum? = null
    private var cachedGlossary: List<GlossaryTerm>? = null

    override suspend fun curriculum(): Curriculum = curriculumLock.withLock {
        cachedCurriculum ?: withContext(Dispatchers.IO) {
            parseCurriculum(readAsset(CURRICULUM_ASSET))
        }.also { cachedCurriculum = it }
    }

    override suspend fun lessonMarkdown(contentAsset: String): String =
        withContext(Dispatchers.IO) { readAsset(contentAsset) }

    override suspend fun labSource(labAsset: String): String =
        withContext(Dispatchers.IO) { readAsset(labAsset) }

    override suspend fun glossary(): List<GlossaryTerm> = glossaryLock.withLock {
        cachedGlossary ?: withContext(Dispatchers.IO) {
            parseGlossary(readAsset(GLOSSARY_ASSET))
        }.also { cachedGlossary = it }
    }

    private fun readAsset(path: String): String =
        assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun parseCurriculum(json: String): Curriculum {
        val phases = JSONObject(json).getJSONArray("phases")
        return Curriculum(
            phases = (0 until phases.length()).map { p -> parsePhase(phases.getJSONObject(p)) },
        )
    }

    private fun parsePhase(o: JSONObject): Phase {
        val lessons = o.getJSONArray("lessons")
        return Phase(
            id = o.getString("id"),
            title = o.getString("title"),
            subtitle = o.getString("subtitle"),
            weeks = o.getString("weeks"),
            lessons = (0 until lessons.length()).map { i -> parseLesson(lessons.getJSONObject(i)) },
        )
    }

    private fun parseLesson(o: JSONObject): LessonMeta {
        val checklist = o.getJSONArray("checklist")
        return LessonMeta(
            id = o.getString("id"),
            title = o.getString("title"),
            summary = o.getString("summary"),
            minutes = o.getInt("minutes"),
            kind = LessonKind.valueOf(o.getString("kind")),
            contentAsset = o.getString("content"),
            labAsset = if (o.isNull("lab")) null else o.getString("lab"),
            checklist = (0 until checklist.length()).map { i -> checklist.getString(i) },
        )
    }

    private fun parseGlossary(json: String): List<GlossaryTerm> {
        val terms = JSONArray(json)
        return (0 until terms.length())
            .map { i ->
                val o = terms.getJSONObject(i)
                GlossaryTerm(term = o.getString("term"), definition = o.getString("definition"))
            }
            .sortedBy { it.term.lowercase() }
    }

    private companion object {
        const val CURRICULUM_ASSET = "curriculum.json"
        const val GLOSSARY_ASSET = "glossary.json"
    }
}
