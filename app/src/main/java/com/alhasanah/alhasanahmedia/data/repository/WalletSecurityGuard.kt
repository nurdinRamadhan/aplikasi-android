package com.alhasanah.alhasanahmedia.data.repository

import android.os.Build
import com.alhasanah.alhasanahmedia.BuildConfig
import java.io.File

class WalletSecurityGuard {
    fun assertSensitiveWalletOperationAllowed() {
        if (BuildConfig.DEBUG) return
        val blockedReason = when {
            isLikelyRooted() -> "Perangkat terindikasi tidak aman untuk transaksi dompet."
            isLikelyEmulator() -> "Transaksi dompet tidak diizinkan dari emulator."
            else -> null
        }
        if (blockedReason != null) {
            throw WalletApiException(403, blockedReason)
        }
    }

    private fun isLikelyRooted(): Boolean {
        val paths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/vendor/bin/su",
            "/su/bin/su",
            "/system/app/Superuser.apk",
            "/system/app/Magisk.apk"
        )
        return paths.any { File(it).exists() } ||
            Build.TAGS?.contains("test-keys", ignoreCase = true) == true
    }

    private fun isLikelyEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val product = Build.PRODUCT.lowercase()
        return fingerprint.startsWith("generic") ||
            "emulator" in fingerprint ||
            "sdk_gphone" in product ||
            "google_sdk" in product ||
            "emulator" in model ||
            "android sdk built for" in model ||
            manufacturer == "genymotion" ||
            (brand.startsWith("generic") && product.startsWith("generic"))
    }
}
