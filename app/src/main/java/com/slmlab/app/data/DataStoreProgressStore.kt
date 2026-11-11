package com.slmlab.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.slmlabDataStore: DataStore<Preferences> by preferencesDataStore(name = "slmlab")

/** Progress and settings in Preferences DataStore; saved lab code as files under filesDir/code. */
class DataStoreProgressStore(context: Context, private val paths: AppPaths) : ProgressStore {

    private val store: DataStore<Preferences> = context.applicationContext.slmlabDataStore

    /** Reads fall back to empty preferences if the stored file is unreadable. */
    private val prefs: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    override val completedLessons: Flow<Set<String>> =
        prefs.map { it[COMPLETED].orEmpty() }

    override val checkedItems: Flow<Set<String>> =
        prefs.map { it[CHECKED].orEmpty() }

    override val settings: Flow<AppSettings> = prefs.map { p ->
        AppSettings(
            themeMode = parseTheme(p[THEME]),
            editorFontSp = p[EDITOR_FONT] ?: AppSettings().editorFontSp,
        )
    }

    override suspend fun setLessonCompleted(lessonId: String, completed: Boolean) {
        store.edit { p ->
            val ids = p[COMPLETED].orEmpty().toMutableSet()
            if (completed) ids.add(lessonId) else ids.remove(lessonId)
            p[COMPLETED] = ids
        }
    }

    override suspend fun setChecked(lessonId: String, index: Int, checked: Boolean) {
        val key = ProgressStore.checkKey(lessonId, index)
        store.edit { p ->
            val keys = p[CHECKED].orEmpty().toMutableSet()
            if (checked) keys.add(key) else keys.remove(key)
            p[CHECKED] = keys
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { p -> p[THEME] = mode.name }
    }

    override suspend fun setEditorFontSp(sp: Int) {
        store.edit { p -> p[EDITOR_FONT] = sp }
    }

    override suspend fun savedCode(lessonId: String): String? = withContext(Dispatchers.IO) {
        codeFile(lessonId).takeIf { it.isFile }?.readText()
    }

    override suspend fun saveCode(lessonId: String, code: String) {
        withContext(Dispatchers.IO) {
            paths.codeDir.mkdirs()
            codeFile(lessonId).writeText(code)
        }
    }

    override suspend fun clearCode(lessonId: String) {
        withContext(Dispatchers.IO) {
            codeFile(lessonId).delete()
        }
    }

    override suspend fun resetAll() {
        store.edit { p -> p.clear() }
        withContext(Dispatchers.IO) {
            listOf(paths.codeDir, paths.workDir).forEach { dir ->
                dir.listFiles()?.forEach { it.deleteRecursively() }
            }
        }
    }

    private fun codeFile(lessonId: String): File = File(paths.codeDir, "$lessonId.py")

    private fun parseTheme(raw: String?): ThemeMode =
        raw?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM

    private companion object {
        val COMPLETED = stringSetPreferencesKey("completed")
        val CHECKED = stringSetPreferencesKey("checked")
        val THEME = stringPreferencesKey("theme")
        val EDITOR_FONT = intPreferencesKey("editor_font_sp")
    }
}
