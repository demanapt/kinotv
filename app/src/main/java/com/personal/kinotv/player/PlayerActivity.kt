package com.personal.kinotv.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Message
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import com.personal.kinotv.AdBlocker
import com.personal.kinotv.CursorView
import com.personal.kinotv.KinoApp
import com.personal.kinotv.data.SiteClient

/**
 * Плеер сайта на весь экран. Сам плеер — сторонний iframe, поэтому внутри него управляем курсором.
 * Первое OK нажимает туда, где курсор (он стоит на кнопке ▶). Когда курсор спрятался (3 с без нажатий),
 * OK — пауза, любая стрелка — снова курсор. Стрелками плееры сайта не перематывают — только курсором.
 */
class PlayerActivity : Activity() {

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_TITLE = "title"

        fun start(context: Context, embedUrl: String, title: String) {
            context.startActivity(
                Intent(context, PlayerActivity::class.java)
                    .putExtra(EXTRA_URL, embedUrl)
                    .putExtra(EXTRA_TITLE, title)
            )
        }
    }

    private lateinit var webView: WebView
    private lateinit var fullscreenContainer: FrameLayout
    private lateinit var cursor: CursorView
    private lateinit var progress: ProgressBar

    /** Пока по плееру ни разу не нажали, он не получает клавиши — первое OK нажимает туда, где курсор. */
    private var tappedOnce = false

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private val inFullscreen get() = customView != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        webView = WebView(this)
        fullscreenContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
        }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        cursor = CursorView(this)

        // Курсор добавлен последним и всегда лежит поверх плеера, в том числе полноэкранного
        root.addView(webView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        root.addView(fullscreenContainer, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        root.addView(progress, FrameLayout.LayoutParams(MATCH_PARENT, (4 * resources.displayMetrics.density).toInt()))
        root.addView(cursor, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setContentView(root)

        cursor.onMove = { x, y -> sendHover(x, y) }
        cursor.autoHide = true
        setupWebView()

        val embed = intent.getStringExtra(EXTRA_URL)
        if (embed.isNullOrBlank()) {
            finish()
            return
        }
        intent.getStringExtra(EXTRA_TITLE)?.let { toast(it) }
        loadPlayer(embed)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.mediaPlaybackRequiresUserGesture = false
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.setSupportZoom(false)
        s.setSupportMultipleWindows(true)
        s.javaScriptCanOpenWindowsAutomatically = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        s.userAgentString = SiteClient.USER_AGENT

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.setBackgroundColor(Color.BLACK)
        webView.webViewClient = Client()
        webView.webChromeClient = ChromeClient()
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()
    }

    /**
     * Встраиваем плеер в страницу с адресом сайта: так плеер видит, что его открыли с сайта,
     * как если бы он стоял на обычной странице фильма.
     */
    private fun loadPlayer(embedUrl: String) {
        val src = embedUrl.replace("&", "&amp;").replace("\"", "&quot;")
        val html = """
            <!DOCTYPE html><html><head>
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <style>html,body{margin:0;height:100%;background:#000;overflow:hidden}
            iframe{border:0;width:100%;height:100%;display:block}</style>
            </head><body>
            <iframe src="$src" allow="autoplay; fullscreen; encrypted-media; picture-in-picture" allowfullscreen referrerpolicy="origin"></iframe>
            </body></html>
        """.trimIndent()
        webView.loadDataWithBaseURL(KinoApp.settings.siteUrl, html, "text/html", "utf-8", null)
    }

    private inner class Client : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
            if (AdBlocker.isBlocked(request.url)) AdBlocker.emptyResponse() else null

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val scheme = request.url.scheme
            if (scheme != "http" && scheme != "https") return true // intent://, market:// и т.п.
            // Сама страница плеера уже открыта: любой переход всей страницы — это реклама
            return request.isForMainFrame
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            progress.visibility = View.VISIBLE
        }

        override fun onPageFinished(view: WebView, url: String) {
            progress.visibility = View.GONE
        }
    }

    private inner class ChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            progress.progress = newProgress
            if (newProgress >= 100) progress.visibility = View.GONE
        }

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            if (customView != null) {
                callback.onCustomViewHidden()
                return
            }
            customView = view
            customViewCallback = callback
            fullscreenContainer.addView(view, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            fullscreenContainer.visibility = View.VISIBLE
        }

        override fun onHideCustomView() = exitFullscreen()

        // Всплывающие окна в плеере — всегда реклама
        override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message) = false

        override fun onPermissionRequest(request: PermissionRequest) {
            val allowed = request.resources.filter { it == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID }
            if (allowed.isEmpty()) request.deny() else request.grant(allowed.toTypedArray())
        }

        // Убираем серую заглушку с кнопкой Play, которую WebView рисует до старта видео
        override fun getDefaultVideoPoster(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    private fun exitFullscreen() {
        val view = customView ?: return
        fullscreenContainer.removeView(view)
        fullscreenContainer.visibility = View.GONE
        customView = null
        val callback = customViewCallback
        customViewCallback = null
        callback?.onCustomViewHidden()
    }

    // ---------- Пульт ----------

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val down = event.action == KeyEvent.ACTION_DOWN
        val up = event.action == KeyEvent.ACTION_UP
        when (val code = event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (down && event.repeatCount == 0) cursor.onDirectionKey(code, true)
                else if (up) cursor.onDirectionKey(code, false)
                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (up) {
                    if (!cursor.cursorVisible && tappedOnce) sendKeyToPage(KeyEvent.KEYCODE_SPACE)
                    else click()
                }
                return true
            }

            KeyEvent.KEYCODE_BACK -> {
                if (up) {
                    if (inFullscreen) exitFullscreen() else finish()
                }
                return true
            }

            KeyEvent.KEYCODE_MENU -> {
                if (up) {
                    webView.reload()
                    toast("Плеер перезагружен")
                }
                return true
            }

            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                if (up) sendKeyToPage(KeyEvent.KEYCODE_SPACE)
                return true
            }

            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_REWIND -> {
                if (down) sendKeyToPage(
                    if (code == KeyEvent.KEYCODE_MEDIA_REWIND) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
                )
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /** Куда отправлять нажатия: в полноэкранный плеер или в страницу. */
    private fun inputTarget(): View = customView ?: webView

    private fun click() {
        tappedOnce = true
        cursor.show()
        cursor.pulse()
        tapAt(cursor.cursorX, cursor.cursorY)
    }

    private fun tapAt(x: Float, y: Float) {
        val target = inputTarget()
        val downTime = SystemClock.uptimeMillis()
        val downEvent = touchEvent(downTime, downTime, MotionEvent.ACTION_DOWN, x, y)
        target.dispatchTouchEvent(downEvent)
        downEvent.recycle()
        target.postDelayed({
            val upEvent = touchEvent(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y)
            target.dispatchTouchEvent(upEvent)
            upEvent.recycle()
        }, 40)
    }

    private fun touchEvent(downTime: Long, time: Long, action: Int, x: Float, y: Float): MotionEvent {
        val props = MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER }
        val coords = MotionEvent.PointerCoords().apply { this.x = x; this.y = y; pressure = 1f; size = 1f }
        return MotionEvent.obtain(
            downTime, time, action, 1, arrayOf(props), arrayOf(coords),
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0
        )
    }

    /** Наведение мышью — чтобы плеер показывал свои кнопки, когда курсор над ним. */
    private fun sendHover(x: Float, y: Float) {
        val props = MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_MOUSE }
        val coords = MotionEvent.PointerCoords().apply { this.x = x; this.y = y }
        val now = SystemClock.uptimeMillis()
        val ev = MotionEvent.obtain(
            now, now, MotionEvent.ACTION_HOVER_MOVE, 1, arrayOf(props), arrayOf(coords),
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0
        )
        inputTarget().dispatchGenericMotionEvent(ev)
        ev.recycle()
    }

    /** Нажатие клавиши внутри плеера (пробел — пауза, стрелки — перемотка). */
    private fun sendKeyToPage(keyCode: Int) {
        val target = inputTarget()
        val now = SystemClock.uptimeMillis()
        target.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
        target.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    override fun onPause() {
        super.onPause()
        cursor.releaseAll()
        CookieManager.getInstance().flush()
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
