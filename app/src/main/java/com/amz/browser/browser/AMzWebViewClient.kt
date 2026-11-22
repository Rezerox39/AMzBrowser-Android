package com.amz.browser.browser

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.amz.browser.shield.BlocklistManager
import java.io.ByteArrayInputStream
import java.net.URI

class AMzWebViewClient(
    private val blocklist: BlocklistManager,
    private val isAdShieldEnabled: () -> Boolean,
    private val isHttpsOnlyEnabled: () -> Boolean,
    private val onAdBlocked: () -> Unit,
    private val onRedirectBlocked: (String) -> Unit,
    private val onPageStartedCallback: (String) -> Unit,
    private val onPageFinishedCallback: (String, String, Int) -> Unit
) : WebViewClient() {

    private var currentMainHost: String? = null
    private var pageBlockedCount: Int = 0

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        if (!isAdShieldEnabled() || request == null) return null
        val url = request.url.toString()
        if (!request.isForMainFrame && blocklist.shouldBlock(url)) {
            pageBlockedCount++
            onAdBlocked()
            return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        if (request == null) return false
        val uri = request.url
        val scheme = uri.scheme?.lowercase().orEmpty()
        val urlString = uri.toString()

        if ((scheme == "intent" || scheme == "market") && !request.hasGesture()) {
            onRedirectBlocked(urlString)
            return true
        }

        if (isAdShieldEnabled() && request.isForMainFrame && !request.hasGesture() && request.isRedirect) {
            val nextHost = uri.host?.lowercase()
            if (currentMainHost != null && nextHost != null && nextHost != currentMainHost) {
                onRedirectBlocked(urlString)
                return true
            }
        }

        if (isHttpsOnlyEnabled() && scheme == "http" && uri.host != "localhost") {
            view?.loadUrl(urlString.replaceFirst("http://", "https://"))
            return true
        }

        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        pageBlockedCount = 0
        currentMainHost = url?.let { runCatching { URI(it).host?.lowercase() }.getOrNull() }
        if (url != null) onPageStartedCallback(url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (url != null) {
            onPageFinishedCallback(url, view?.title.orEmpty().ifBlank { url }, pageBlockedCount)
        }
    }
}
