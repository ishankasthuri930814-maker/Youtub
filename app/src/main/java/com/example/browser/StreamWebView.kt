package com.example.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

class AndroidMediaBridge(
    private val onMediaState: (Boolean, String, String) -> Unit
) {
    @JavascriptInterface
    fun onMediaStateChanged(isPlaying: Boolean, title: String, url: String) {
        onMediaState(isPlaying, title, url)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StreamWebView(
    url: String,
    modifier: Modifier = Modifier,
    isDesktopMode: Boolean = false,
    isAdBlockEnabled: Boolean = true,
    onUrlChanged: (String) -> Unit = {},
    onTitleChanged: (String) -> Unit = {},
    onProgressChanged: (Int) -> Unit = {},
    onAdBlocked: () -> Unit = {},
    onMediaStateChanged: (Boolean, String, String) -> Unit = { _, _, _ -> },
    onWebViewCreated: (WebView) -> Unit = {}
) {
    val context = LocalContext.current

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            addJavascriptInterface(
                AndroidMediaBridge { playing, title, currentUrl ->
                    onMediaStateChanged(playing, title, currentUrl)
                },
                "AndroidMediaBridge"
            )
        }
    }

    DisposableEffect(isDesktopMode) {
        val desktopAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        val mobileAgent = WebSettings.getDefaultUserAgent(context)
        webView.settings.userAgentString = if (isDesktopMode) desktopAgent else mobileAgent
        onDispose { }
    }

    LaunchedEffect(webView) {
        onWebViewCreated(webView)
    }

    LaunchedEffect(url) {
        if (webView.url != url && url.isNotBlank()) {
            webView.loadUrl(url)
        }
    }

    AndroidView(
        factory = {
            webView.apply {
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val requestUrl = request?.url?.toString() ?: return false
                        if (requestUrl.startsWith("http://") || requestUrl.startsWith("https://")) {
                            return false
                        }
                        return true
                    }

                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        val requestUrl = request?.url?.toString() ?: return null
                        if (isAdBlockEnabled && AdBlockEngine.isAdUrl(requestUrl)) {
                            onAdBlocked()
                            return AdBlockEngine.createEmptyResponse()
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { onUrlChanged(it) }
                        view?.evaluateJavascript(AdBlockEngine.AD_BLOCK_AND_BACKGROUND_JS, null)
                    }

                    override fun onLoadResource(view: WebView?, url: String?) {
                        super.onLoadResource(view, url)
                        if (isAdBlockEnabled && url != null && AdBlockEngine.isAdUrl(url)) {
                            onAdBlocked()
                        } else {
                            view?.evaluateJavascript(AdBlockEngine.AD_BLOCK_AND_BACKGROUND_JS, null)
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        url?.let {
                            onUrlChanged(it)
                            onTitleChanged(view?.title ?: "StreamTube")
                        }
                        // Inject ad-blocker & background playback fix
                        view?.evaluateJavascript(AdBlockEngine.AD_BLOCK_AND_BACKGROUND_JS, null)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                        if (newProgress > 60) {
                            view?.evaluateJavascript(AdBlockEngine.AD_BLOCK_AND_BACKGROUND_JS, null)
                        }
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { onTitleChanged(it) }
                    }
                }

                if (url.isNotBlank()) {
                    loadUrl(url)
                }
            }
        },
        modifier = modifier
    )
}
