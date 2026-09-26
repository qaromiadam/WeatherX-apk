package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val webViewCache = java.io.File(cacheDir, "WebView")
            if (webViewCache.exists()) {
                webViewCache.deleteRecursively()
            }
            val chromiumCache = java.io.File(cacheDir, "org.chromium.android_webview")
            if (chromiumCache.exists()) {
                chromiumCache.deleteRecursively()
            }
        } catch (_: Exception) {}
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = androidx.compose.ui.graphics.Color(0xFF07090E)
                ) { innerPadding ->
                    WeatherScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    fun isNetworkConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun setDynamicLayoutDirection(isRtl: Boolean) {
        runOnUiThread {
            window.decorView.layoutDirection = if (isRtl) {
                View.LAYOUT_DIRECTION_RTL
            } else {
                View.LAYOUT_DIRECTION_LTR
            }
        }
    }
}

class WebAppInterface(private val activity: MainActivity) {
    @JavascriptInterface
    fun isNetworkAvailable(): Boolean {
        return activity.isNetworkConnected()
    }

    @JavascriptInterface
    fun setLayoutDirectionRtl(isRtl: Boolean) {
        activity.setDynamicLayoutDirection(isRtl)
    }

    @JavascriptInterface
    fun onLanguageChanged(lang: String) {
        activity.setDynamicLayoutDirection(lang == "ar")
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WeatherScreen(modifier: Modifier = Modifier) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    BackHandler(enabled = webViewInstance != null) {
        webViewInstance?.evaluateJavascript(
            """
            (function() {
                var blockingOverlay = document.getElementById('fullscreenOfflineOverlay');
                if (blockingOverlay && !blockingOverlay.classList.contains('hidden')) {
                    return true;
                }
                var fsWind = document.getElementById('fullscreen-wind-map-view');
                if (fsWind && !fsWind.classList.contains('hidden')) {
                    var closeWind = document.getElementById('close-fs-wind-map-btn');
                    if (closeWind) closeWind.click();
                    return true;
                }
                var onboarding = document.getElementById('onboarding-fullscreen-view');
                if (onboarding && !onboarding.classList.contains('hidden')) {
                    return true;
                }
                var overlay = document.getElementById('detailOverlay');
                var settings = document.getElementById('settingsModal');
                if (overlay && !overlay.classList.contains('hidden')) {
                    var backBtn = document.getElementById('overlaySingleBackBtn');
                    if (backBtn) backBtn.click();
                    return true;
                }
                if (settings && !settings.classList.contains('hidden')) {
                    var closeBtn = document.getElementById('closeSettingsBackBtn');
                    if (closeBtn) closeBtn.click();
                    return true;
                }
                return false;
            })();
            """.trimIndent()
        ) { result ->
            if (result != "true") {
                if (webViewInstance?.canGoBack() == true) {
                    webViewInstance?.goBack()
                }
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.parseColor("#07090E"))
                webViewClient = object : WebViewClient() {
                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        return true
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        return super.onConsoleMessage(consoleMessage)
                    }
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    allowFileAccess = true
                    allowContentAccess = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                if (context is MainActivity) {
                    addJavascriptInterface(WebAppInterface(context), "AndroidInterface")
                }

                clearCache(true)
                loadUrl("file:///android_asset/index.html")
                webViewInstance = this
            }
        }
    )
}
