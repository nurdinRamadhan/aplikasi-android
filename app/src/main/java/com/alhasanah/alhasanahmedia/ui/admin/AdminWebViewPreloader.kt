package com.alhasanah.alhasanahmedia.ui.admin

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebStorage
import androidx.annotation.MainThread
import androidx.core.view.doOnDetach

const val ADMIN_PANEL_URL = "https://alhasanah-media.vercel.app"

object AdminWebViewPreloader {
    private var webView: WebView? = null
    private var loadedUrl: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    @MainThread
    fun preload(context: Context, url: String = ADMIN_PANEL_URL): WebView {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "Admin WebView must be created on the main thread."
        }

        val appContext = context.applicationContext
        val cached = webView
        if (cached != null) {
            if (loadedUrl != url) {
                cached.loadUrl(url)
                loadedUrl = url
            }
            return cached
        }

        CookieManager.getInstance().setAcceptCookie(true)
        val created = WebView(appContext).apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            setBackgroundColor(Color.TRANSPARENT)
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = false
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            overScrollMode = WebView.OVER_SCROLL_IF_CONTENT_SCROLLS
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                loadsImagesAutomatically = true
                offscreenPreRaster = true
                mediaPlaybackRequiresUserGesture = false
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                textZoom = 100
                allowFileAccess = false
                allowContentAccess = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                userAgentString = DESKTOP_USER_AGENT
            }
            addJavascriptInterface(AdminNativeBridge(), "AlhasanahNativeBridge")
        }

        created.doOnDetach {
            (it.parent as? ViewGroup)?.removeView(it)
        }
        created.loadUrl(url)
        loadedUrl = url
        webView = created
        return created
    }

    @MainThread
    fun acquire(context: Context, url: String = ADMIN_PANEL_URL): WebView {
        val view = preload(context, url)
        (view.parent as? ViewGroup)?.removeView(view)
        return view
    }

    @MainThread
    fun clearSession() {
        webView?.apply {
            stopLoading()
            clearHistory()
            clearFormData()
            clearSslPreferences()
            loadUrl("about:blank")
        }
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        WebView.clearClientCertPreferences(null)
        loadedUrl = null
        webView = null
    }
}

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36 AlhasanahMediaNative/1.0"
