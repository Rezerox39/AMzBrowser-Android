package com.amz.browser

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.PrintWriter
import java.io.StringWriter

object CrashReporter {

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val report = buildString {
                append("AMz Browser crash report\n")
                append("Thread: ").append(thread.name).append('\n')
                append("Time: ").append(System.currentTimeMillis()).append('\n')
                append(sw.toString())
            }
            runCatching { writeReport(context, report) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun writeReport(context: Context, report: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "amz_crash.txt")
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/OpenCode")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(report.toByteArray(Charsets.UTF_8))
        }
    }
}
