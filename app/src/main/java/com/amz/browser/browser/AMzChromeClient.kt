package com.amz.browser.browser

import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class AMzChromeClient(
    private val isAdShieldEnabled: () -> Boolean,
    private val onProgressChangedCallback: (Int) -> Unit,
    private val onPopupBlocked: () -> Unit,
    private val onOpenNewTabRequest: (Message) -> Unit,
    private val onShowCustomFullscreen: (View, CustomViewCallback) -> Unit,
    private val onHideCustomFullscreen: () -> Unit,
    private val onRequestFileChooser: (ValueCallback<Array<Uri>>, Array<String>) -> Unit
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        onProgressChangedCallback(newProgress)
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        if (isAdShieldEnabled() && !isUserGesture) {
            onPopupBlocked()
            return false
        }
        if (resultMsg != null) {
            onOpenNewTabRequest(resultMsg)
            return true
        }
        return false
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view != null && callback != null) {
            onShowCustomFullscreen(view, callback)
        }
    }

    override fun onHideCustomView() {
        onHideCustomFullscreen()
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        if (filePathCallback == null) return false
        onRequestFileChooser(filePathCallback, fileChooserParams?.acceptTypes ?: emptyArray())
        return true
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        request?.deny()
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        callback?.invoke(origin, false, false)
    }
}
