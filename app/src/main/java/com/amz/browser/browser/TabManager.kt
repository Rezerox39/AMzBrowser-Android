package com.amz.browser.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import com.amz.browser.data.PersistedTab
import com.amz.browser.shield.BlocklistManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Start Page",
    val url: String = "",
    val isPrivate: Boolean = false,
    val desktopMode: Boolean = false,
    val readerMode: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val progress: Int = 0,
    val thumbnail: Bitmap? = null,
    val webView: WebView? = null
)

class TabManagerViewModel : ViewModel() {

    private val _tabs = MutableStateFlow(listOf(BrowserTab()))
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(_tabs.value.first().id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    // Restoration must happen at most once. Without this guard, writing the
    // restored tabs triggers saveTabs -> DataStore re-emission -> restore again,
    // which produces an unbounded full-page recomposition loop.
    private var restoredOnce = false

    fun restoreTabsIfFresh(saved: List<PersistedTab>) {
        if (restoredOnce || saved.isEmpty()) return
        if (_tabs.value.size == 1 && _tabs.value.first().url.isEmpty() && _tabs.value.first().webView == null) {
            restoredOnce = true
            val restored = saved.map { BrowserTab(id = it.id, title = it.title, url = it.url) }
            _tabs.value = restored
            _activeTabId.value = restored.last().id
        }
    }

    fun ensureWebView(
        tabId: String,
        context: Context,
        isDarkTheme: Boolean,
        textZoom: Int,
        isAdShieldEnabled: () -> Boolean,
        isHttpsOnlyEnabled: () -> Boolean,
        onAdBlocked: () -> Unit,
        onPopupBlocked: () -> Unit,
        onRedirectBlocked: (String) -> Unit,
        onPageFinished: (tabId: String, url: String, title: String, blockedCount: Int, isPrivate: Boolean) -> Unit,
        onRequestFileChooser: (ValueCallback<Array<Uri>>, Array<String>) -> Unit
    ): WebView {
        val existing = _tabs.value.find { it.id == tabId }?.webView
        if (existing != null) return existing

        val tab = _tabs.value.find { it.id == tabId }
        val blocklist = BlocklistManager.getInstance(context)

        val client = AMzWebViewClient(
            blocklist = blocklist,
            isAdShieldEnabled = isAdShieldEnabled,
            isHttpsOnlyEnabled = isHttpsOnlyEnabled,
            onAdBlocked = onAdBlocked,
            onRedirectBlocked = onRedirectBlocked,
            onPageStartedCallback = { url ->
                updateTab(tabId) { it.copy(url = url, progress = 15) }
            },
            onPageFinishedCallback = { url, title, blocked ->
                val wv = _tabs.value.find { it.id == tabId }?.webView
                updateTab(tabId) {
                    it.copy(
                        url = url,
                        title = title,
                        progress = 100,
                        canGoBack = wv?.canGoBack() == true,
                        canGoForward = wv?.canGoForward() == true
                    )
                }
                onPageFinished(tabId, url, title, blocked, tab?.isPrivate == true)
            }
        )

        val chrome = AMzChromeClient(
            isAdShieldEnabled = isAdShieldEnabled,
            onProgressChangedCallback = { progress ->
                updateTab(tabId) { it.copy(progress = progress) }
            },
            onPopupBlocked = onPopupBlocked,
            onOpenNewTabRequest = { msg ->
                val newId = openNewTab(isPrivate = tab?.isPrivate == true)
                val transport = msg.obj as? WebView.WebViewTransport
                val newWv = ensureWebView(
                    tabId = newId,
                    context = context,
                    isDarkTheme = isDarkTheme,
                    textZoom = textZoom,
                    isAdShieldEnabled = isAdShieldEnabled,
                    isHttpsOnlyEnabled = isHttpsOnlyEnabled,
                    onAdBlocked = onAdBlocked,
                    onPopupBlocked = onPopupBlocked,
                    onRedirectBlocked = onRedirectBlocked,
                    onPageFinished = onPageFinished,
                    onRequestFileChooser = onRequestFileChooser
                )
                transport?.webView = newWv
                msg.sendToTarget()
            },
            onShowCustomFullscreen = { _, _ -> },
            onHideCustomFullscreen = {},
            onRequestFileChooser = onRequestFileChooser
        )

        val created = WebViewFactory.create(
            context = context,
            isDarkTheme = isDarkTheme,
            textZoomPercent = textZoom,
            desktopMode = tab?.desktopMode == true,
            adShieldEnabled = isAdShieldEnabled(),
            webViewClient = client,
            chromeClient = chrome
        )

        updateTab(tabId) { it.copy(webView = created) }
        if (!tab?.url.isNullOrBlank()) {
            created.loadUrl(tab!!.url)
        }
        return created
    }

    fun captureThumbnail(tabId: String) {
        val wv = _tabs.value.find { it.id == tabId }?.webView ?: return
        if (wv.width <= 0 || wv.height <= 0) return
        runCatching {
            val bmp = Bitmap.createBitmap(
                (wv.width / 3).coerceAtLeast(120),
                (wv.height / 3).coerceAtLeast(160),
                Bitmap.Config.RGB_565
            )
            val canvas = Canvas(bmp)
            canvas.scale(bmp.width.toFloat() / wv.width, bmp.height.toFloat() / wv.height)
            wv.draw(canvas)
            updateTab(tabId) { it.copy(thumbnail = bmp) }
        }
    }

    fun updateTab(id: String, transform: (BrowserTab) -> BrowserTab) {
        _tabs.value = _tabs.value.map { if (it.id == id) transform(it) else it }
    }

    fun openNewTab(isPrivate: Boolean = false): String {
        val next = BrowserTab(
            title = if (isPrivate) "Private Tab" else "Start Page",
            isPrivate = isPrivate
        )
        _tabs.value = _tabs.value + next
        _activeTabId.value = next.id
        return next.id
    }

    fun selectTab(id: String) {
        captureThumbnail(_activeTabId.value)
        if (_tabs.value.any { it.id == id }) {
            _activeTabId.value = id
        }
    }

    fun closeTab(id: String) {
        val target = _tabs.value.find { it.id == id }
        target?.webView?.destroy()

        val remaining = _tabs.value.filterNot { it.id == id }
        if (target?.isPrivate == true && remaining.none { it.isPrivate }) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
        }

        if (remaining.isEmpty()) {
            val fresh = BrowserTab()
            _tabs.value = listOf(fresh)
            _activeTabId.value = fresh.id
        } else {
            _tabs.value = remaining
            if (_activeTabId.value == id) {
                _activeTabId.value = remaining.last().id
            }
        }
    }

    fun toPersistedTabs(): List<PersistedTab> =
        _tabs.value.filterNot { it.isPrivate }.map { PersistedTab(it.id, it.title, it.url) }

    override fun onCleared() {
        _tabs.value.forEach { it.webView?.destroy() }
        super.onCleared()
    }
}
