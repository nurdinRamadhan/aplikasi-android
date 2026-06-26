package com.alhasanah.alhasanahmedia.ui.admin

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeader
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderSize
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminPanelScreen(
    navController: NavController,
    initialUrl: String = ADMIN_PANEL_URL,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = filePathCallback
        filePathCallback = null
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            ?: if (result.resultCode == Activity.RESULT_OK) {
                cameraUri?.let { arrayOf(it) }
            } else {
                null
            }
        cameraUri = null
        callback?.onReceiveValue(uris)
    }
    val webView = remember {
        AdminWebViewPreloader.acquire(context, initialUrl).apply {
            setBackgroundColor(backgroundColor.toArgb())
        }
    }

    LaunchedEffect(initialUrl) {
        AdminWebViewPreloader.preload(context, initialUrl)
    }

    DisposableEffect(view, backgroundColor, surfaceColor) {
        val window = (view.context as? Activity)?.window
        val previousStatus = window?.statusBarColor
        val previousNavigation = window?.navigationBarColor
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousLightStatus = controller?.isAppearanceLightStatusBars
        val previousLightNavigation = controller?.isAppearanceLightNavigationBars

        window?.statusBarColor = surfaceColor.toArgb()
        window?.navigationBarColor = backgroundColor.toArgb()
        controller?.isAppearanceLightStatusBars = !isColorDark(surfaceColor)
        controller?.isAppearanceLightNavigationBars = !isColorDark(backgroundColor)

        onDispose {
            if (previousStatus != null) window.statusBarColor = previousStatus
            if (previousNavigation != null) window.navigationBarColor = previousNavigation
            if (previousLightStatus != null) controller?.isAppearanceLightStatusBars = previousLightStatus
            if (previousLightNavigation != null) controller?.isAppearanceLightNavigationBars = previousLightNavigation
            (webView.parent as? ViewGroup)?.removeView(webView)
        }
    }

    BackHandler {
        if (webView.canGoBack()) {
            webView.goBack()
            canGoBack = webView.canGoBack()
        } else {
            navController.popBackStack()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(Modifier.fillMaxSize()) {
            AppPageHeader(
                title = "ADMIN PANEL",
                subtitle = "Dashboard pengelolaan pesantren",
                isDark = isColorDark(surfaceColor),
                onBack = { navController.popBackStack() },
                size = AppPageHeaderSize.Compact
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                AdminWebView(
                    webView = webView,
                    onProgress = {
                        progress = it
                        if (it >= 0.92f) isLoading = false
                    },
                    onLoadingChanged = {
                        isLoading = it
                        canGoBack = webView.canGoBack()
                    },
                    onCanGoBackChanged = { canGoBack = it },
                    onShowFileChooser = { callback, params ->
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = callback
                        val captureIntent = createCameraCaptureIntent(context)?.also {
                            cameraUri = it.getParcelableExtra(MediaStore.EXTRA_OUTPUT)
                        }
                        val contentIntent = params.createIntent().apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        val chooser = Intent.createChooser(contentIntent, "Pilih file").apply {
                            captureIntent?.let {
                                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(it))
                            }
                        }
                        runCatching { fileChooserLauncher.launch(chooser) }
                            .onFailure {
                                filePathCallback = null
                                cameraUri = null
                                callback.onReceiveValue(null)
                            }
                        true
                    }
                )

                if (isLoading) {
                    AdminSkeleton(
                        progress = progress,
                        primary = primary,
                        surface = surfaceColor,
                        background = backgroundColor,
                        content = onSurface
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AdminWebView(
    webView: WebView,
    onProgress: (Float) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit,
    onShowFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            webView.apply {
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgress((newProgress / 100f).coerceIn(0f, 1f))
                        if (newProgress >= 92) onLoadingChanged(false)
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        return if (filePathCallback != null && fileChooserParams != null) {
                            onShowFileChooser(filePathCallback, fileChooserParams)
                        } else {
                            false
                        }
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val uri = request?.url ?: return false
                        return handleUrl(view, uri)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onLoadingChanged(true)
                        onProgress(0.08f)
                        onCanGoBackChanged(view?.canGoBack() == true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript(nativeDetectionScript(), null)
                        onProgress(1f)
                        onLoadingChanged(false)
                        onCanGoBackChanged(view?.canGoBack() == true)
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: android.net.http.SslError?
                    ) {
                        handler?.cancel()
                        onLoadingChanged(false)
                    }
                }
                onProgress(progress / 100f)
                onLoadingChanged(progress < 92)
                onCanGoBackChanged(canGoBack())
            }
        },
        update = {
            it.evaluateJavascript(nativeDetectionScript(), null)
            onCanGoBackChanged(it.canGoBack())
        }
    )
}

private fun handleUrl(view: WebView?, uri: Uri): Boolean {
    return when (uri.scheme) {
        "http", "https" -> {
            view?.loadUrl(uri.toString())
            true
        }
        else -> true
    }
}

private fun nativeDetectionScript(): String =
    """
        (function() {
          var viewport = document.querySelector('meta[name="viewport"]');
          if (!viewport) {
            viewport = document.createElement('meta');
            viewport.name = 'viewport';
            document.head && document.head.appendChild(viewport);
          }
          viewport.setAttribute('content', 'width=1200, initial-scale=1, minimum-scale=0.25, maximum-scale=3, viewport-fit=cover');
          window.__ALHASANAH_NATIVE__ = true;
          window.AlhasanahNative = {
            isNativeApp: true,
            platform: 'android',
            appName: 'Alhasanah Media'
          };
          document.documentElement.setAttribute('data-alhasanah-native', 'android');
          document.documentElement.setAttribute('data-alhasanah-native-viewport', 'desktop');
          document.body && document.body.setAttribute('data-alhasanah-native', 'android');
          window.dispatchEvent(new Event('resize'));
        })();
    """.trimIndent()

private fun createCameraCaptureIntent(context: android.content.Context): Intent? {
    val imageFile = runCatching { createTempImageFile(context) }.getOrNull() ?: return null
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
    return Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
        putExtra(MediaStore.EXTRA_OUTPUT, uri)
        clipData = ClipData.newUri(context.contentResolver, "Admin upload photo", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
}

private fun createTempImageFile(context: android.content.Context): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir = File(context.cacheDir, Environment.DIRECTORY_PICTURES).apply { mkdirs() }
    return File.createTempFile("admin_upload_${timestamp}_", ".jpg", storageDir)
}

@Composable
private fun AdminSkeleton(
    progress: Float,
    primary: Color,
    surface: Color,
    background: Color,
    content: Color
) {
    val shimmer = rememberInfiniteTransition(label = "adminSkeleton")
    val alpha by shimmer.animateFloat(
        initialValue = 0.34f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(880, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "adminSkeletonAlpha"
    )
    val brush = Brush.horizontalGradient(
        colors = listOf(
            primary.copy(alpha = 0.08f),
            primary.copy(alpha = 0.18f * alpha),
            content.copy(alpha = 0.08f)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = primary,
            trackColor = primary.copy(alpha = 0.14f)
        )
        Spacer(Modifier.height(18.dp))
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            brush = brush,
            surface = surface
        )
        Spacer(Modifier.height(16.dp))
        repeat(3) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp),
                brush = brush,
                surface = surface
            )
            Spacer(Modifier.height(12.dp))
        }
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) {
                SkeletonBlock(
                    modifier = Modifier
                        .weight(1f)
                        .height(104.dp)
                        .padding(horizontal = 4.dp),
                    brush = brush,
                    surface = surface
                )
            }
        }
        Spacer(Modifier.weight(1f))
        SkeletonBlock(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 176.dp, height = 12.dp)
                .alpha(0.72f),
            brush = brush,
            surface = surface
        )
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier,
    brush: Brush,
    surface: Color
) {
    Box(
        modifier = modifier
            .background(surface, RoundedCornerShape(8.dp))
            .background(brush, RoundedCornerShape(8.dp))
    )
}

private fun isColorDark(color: Color): Boolean {
    val luminance = (0.299 * color.red) + (0.587 * color.green) + (0.114 * color.blue)
    return luminance < 0.5
}
