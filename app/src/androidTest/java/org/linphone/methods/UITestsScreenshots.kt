package org.linphone.methods

import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.view.View
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.Screenshot
import java.io.File
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.linphone.core.tools.Log

object UITestsScreenshots {

    var defaultPath = File(getExternalStoragePublicDirectory(DIRECTORY_PICTURES), "")
    var screenshotComparison = true

    fun definePath(testClass: String, testFunction: String, startTime: String) {
        defaultPath = File(
            File(
                getExternalStoragePublicDirectory(DIRECTORY_PICTURES),
                "linphone_uitests"
            ).absolutePath,
            testClass + File.separator + testFunction + File.separator + startTime
        )
    }

    private fun screenshot(screenShotName: String, view: View? = null) {
        Log.i("[UITests] Taking screenshot of '$screenShotName'")
        val screenCapture = if (view == null) Screenshot.capture() else Screenshot.capture(view)
        val processors = setOf(MyScreenCaptureProcessor())
        try {
            screenCapture.apply {
                name = screenShotName
                process(processors)
            }
            Log.i("[UITests] Screenshot taken")
        } catch (ex: IOException) {
            Log.e("[UITests] Could not take a screenshot", ex)
        }
    }

    fun takeScreenshot(
        name: String,
        variant: String? = null,
        delay: Double = 0.5,
        view: View? = null,
        line: Int = Throwable().stackTrace[1].lineNumber
    ) {
        if (!screenshotComparison) return
        if (name.contains(".") || variant?.contains(".") == true) {
            throw Exception("[UITests] \".\" character is forbidden for takeScreenshot methods arguments name and variant")
        }
        runBlocking { delay((delay * 1000).toLong()) }
        screenshot(line.toString() + ".$name" + if (variant != null) ".$variant" else "", view)
    }
}

class MyScreenCaptureProcessor : BasicScreenCaptureProcessor() {

    init {
        this.mDefaultScreenshotPath = UITestsScreenshots.defaultPath
    }

    override fun getFilename(prefix: String): String = prefix
}
