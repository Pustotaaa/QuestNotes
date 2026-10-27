package com.questnotes.app

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

/**
 * Обёртка над HTML-прототипом «Квест-заметки».
 * Страница лежит в assets/index.html и открывается с локального https-адреса
 * через WebViewAssetLoader, поэтому localStorage работает как в браузере
 * и данные хранятся во внутренней памяти приложения.
 */
class MainActivity : ComponentActivity() {

    private lateinit var root: FrameLayout
    private lateinit var web: WebView
    private var insetsJs = ""

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mediaPlaybackRequiresUserGesture = true
            settings.textZoom = 100
            addJavascriptInterface(Bridge(), "QNAndroid")
            webViewClient = object : WebViewClientCompat() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    if (request.url.host == WebViewAssetLoader.DEFAULT_DOMAIN) return false
                    // внешние ссылки открываем в браузере, а не внутри приложения
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    } catch (_: ActivityNotFoundException) {
                    }
                    return true
                }

                override fun onPageFinished(view: WebView, url: String) {
                    applyInsets()
                }
            }
        }

        root = FrameLayout(this).apply { addView(web) }
        setContentView(root)

        // Системные панели рисуются поверх страницы; их размеры передаём в CSS как --sa-*.
        // Клавиатура уменьшает область WebView, чтобы поля ввода не прятались под ней.
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(0, 0, 0, ime.bottom)
            val d = resources.displayMetrics.density
            val bottom = if (ime.bottom > 0) 0f else bars.bottom / d
            insetsJs = "(function(s){s.setProperty('--sa-top','${bars.top / d}px');" +
                "s.setProperty('--sa-right','${bars.right / d}px');" +
                "s.setProperty('--sa-bottom','${bottom}px');" +
                "s.setProperty('--sa-left','${bars.left / d}px')})(document.documentElement.style)"
            applyInsets()
            WindowInsetsCompat.CONSUMED
        }

        // «Назад» сначала закрывает окна внутри страницы, и только потом выходит из приложения.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("(window.qnBack&&qnBack())?'yes':'no'") { result ->
                    if (result != "\"yes\"") {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })

        // всё состояние приложения живёт в localStorage, поэтому страницу просто открываем заново
        web.loadUrl("https://${WebViewAssetLoader.DEFAULT_DOMAIN}/assets/index.html")
    }

    private fun applyInsets() {
        if (insetsJs.isNotEmpty()) web.evaluateJavascript(insetsJs, null)
    }

    override fun onPause() {
        web.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
    }

    override fun onDestroy() {
        root.removeView(web)
        web.destroy()
        super.onDestroy()
    }

    private inner class Bridge {
        /** Тёмные иконки статус-бара на светлой теме страницы и светлые на тёмной. */
        @JavascriptInterface
        fun setBars(lightBackground: Boolean) {
            runOnUiThread {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = lightBackground
                    isAppearanceLightNavigationBars = lightBackground
                }
            }
        }
    }
}
