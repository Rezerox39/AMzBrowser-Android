package com.amz.browser.browser

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.amz.browser.download.BrowserServices
import com.amz.browser.shield.ShieldInjector

object WebViewFactory {

    const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    @SuppressLint("SetJavaScriptEnabled")
    fun create(
        context: Context,
        isDarkTheme: Boolean,
        textZoomPercent: Int,
        desktopMode: Boolean,
        adShieldEnabled: Boolean,
        webViewClient: AMzWebViewClient,
        chromeClient: AMzChromeClient
    ): WebView {
        return WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                allowFileAccess = false
                allowContentAccess = false
                safeBrowsingEnabled = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = false
                textZoom = textZoomPercent
                useWideViewPort = desktopMode
                loadWithOverviewMode = desktopMode
                if (desktopMode) {
                    userAgentString = DESKTOP_USER_AGENT
                }
            }

            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, isDarkTheme)
            }

            if (adShieldEnabled) {
                ShieldInjector.attachDocumentStartShield(this)
            }

            this.webViewClient = webViewClient
            this.webChromeClient = chromeClient

            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                BrowserServices.downloadEngine.enqueue(
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType
                )
            }
        }
    }
}
