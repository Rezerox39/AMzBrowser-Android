package com.amz.browser

import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.amz.browser.download.BrowserServices
import com.amz.browser.download.SwiftDownloadEngine
import com.amz.browser.shield.BlocklistManager
import com.amz.browser.ui.screens.AMzBrowserRoot
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        pendingFileCallback?.onReceiveValue(uris.toTypedArray())
        pendingFileCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        CrashReporter.install(applicationContext)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (BrowserServices.downloadEngine !is SwiftDownloadEngine) {
            BrowserServices.downloadEngine = SwiftDownloadEngine(applicationContext)
        }

        val blocklist = BlocklistManager.getInstance(applicationContext)
        lifecycleScope.launch {
            blocklist.initializeAndRefreshIfNeeded()
        }

        val initialUrl = intent?.dataString

        setContent {
            AMzBrowserRoot(
                initialUrl = initialUrl,
                onRequestFileChooser = { callback, mimeTypes ->
                    pendingFileCallback?.onReceiveValue(null)
                    pendingFileCallback = callback
                    filePickerLauncher.launch(mimeTypes.ifEmpty { arrayOf("*/*") })
                }
            )
        }
    }
}
