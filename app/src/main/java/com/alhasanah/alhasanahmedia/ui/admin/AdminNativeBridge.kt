package com.alhasanah.alhasanahmedia.ui.admin

import android.webkit.JavascriptInterface

class AdminNativeBridge {
    @JavascriptInterface
    fun isNativeApp(): Boolean = true

    @JavascriptInterface
    fun platform(): String = "android"

    @JavascriptInterface
    fun appName(): String = "Alhasanah Media"
}
