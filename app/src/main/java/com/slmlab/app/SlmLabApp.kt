package com.slmlab.app

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.slmlab.app.data.AppContainer
import com.slmlab.app.data.AppPaths
import com.slmlab.app.data.AssetCurriculumRepository
import com.slmlab.app.data.DataStoreProgressStore
import com.slmlab.app.python.ChaquopyRunner
import java.io.File

class SlmLabApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Start the embedded interpreter once per process.
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        // 2. Create the writable directory layout under filesDir.
        val paths = AppPaths(filesDir)
        paths.ensure()

        // 3. Copy bundled Python libraries and data files, overwriting them.
        copyAssetDir("pylib", paths.pylibDir)
        copyAssetDir("data", paths.dataDir)

        // 4. Point slm_runner at the copied files.
        Python.getInstance()
            .getModule("slm_runner")
            .callAttr(
                "configure",
                paths.pylibDir.absolutePath,
                paths.dataDir.absolutePath,
                paths.workDir.absolutePath,
            )

        // 5. Publish the dependency container.
        AppContainer.paths = paths
        AppContainer.repo = AssetCurriculumRepository(this)
        AppContainer.progress = DataStoreProgressStore(this, paths)
        AppContainer.runner = ChaquopyRunner()
    }

    /** Copies every file directly inside assets/[assetDir] into [target]. */
    private fun copyAssetDir(assetDir: String, target: File) {
        val names = assets.list(assetDir).orEmpty()
        for (name in names) {
            assets.open("$assetDir/$name").use { input ->
                File(target, name).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}
