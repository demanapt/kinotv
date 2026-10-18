package com.personal.kinotv

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
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
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast

class MainActivity : Activity() {

    companion object {
        private const val DEFAULT_HOME = "https://uakinogo.io/"
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

        private const val KEY_HOME = "home"
        private const val KEY_LAST_URL = "last_url"
        private const val KEY_DESKTOP = "desktop"
        private const val KEY_PAGE_WIDTH = "page_width"
        private const val KEY_CURSOR_SPEED = "cursor_speed"
        private const val KEY_NAV = "nav_mode"
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var webView: WebView
    private lateinit var fullscreenContainer: FrameLayout
    private lateinit var cursor: CursorView
    private lateinit var progress: ProgressBar
    private lateinit var updater: Updater

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var defaultUserAgent = ""
    private var okLongPressed = false
    private var lastBackPress = 0L
    private var lastNavKey = 0L

    /** true — курсор (для плеера), false — навигация стрелками по элементам сайта, как в Netflix. */
    private var cursorActive = false
    private val navScript by lazy { assets.open("nav.js").bufferedReader().use { it.readText() } }

    private val homeUrl get() = prefs.getString(KEY_HOME, DEFAULT_HOME) ?: DEFAULT_HOME
    private val homeHost get() = Uri.parse(homeUrl).host.orEmpty().removePrefix("www.")
    private val inFullscreen get() = customView != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        prefs = getSharedPreferences("settings", MODE_PRIVATE)
        updater = Updater(this)

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        webView = WebView(this)
        fullscreenContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
        }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }
        cursor = CursorView(this)

        // Порядок важен: курсор добавлен последним и всегда лежит поверх плеера
        root.addView(webView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        root.addView(fullscreenContainer, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        root.addView(progress, FrameLayout.LayoutParams(MATCH_PARENT, (4 * resources.displayMetrics.density).toInt()))
        root.addView(cursor, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setContentView(root)

        cursor.speedFactor = prefs.getFloat(KEY_CURSOR_SPEED, 1f)
        cursor.onMove = { x, y -> sendHover(x, y) }
        cursor.onEdgeScroll = { dx, dy -> webView.evaluateJavascript("window.scrollBy($dx,$dy)", null) }

        setupWebView()
        setCursorActive(!prefs.getBoolean(KEY_NAV, true))

        if (intent?.action == Updater.ACTION_INSTALL_STATUS) {
            updater.handleInstallStatus(intent)
        }
        val last = prefs.getString(KEY_LAST_URL, null)
        webView.loadUrl(if (last != null && isSiteUrl(Uri.parse(last))) last else homeUrl)

        updater.check(silent = true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Updater.ACTION_INSTALL_STATUS) updater.handleInstallStatus(intent)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val s = webView.settings
        defaultUserAgent = s.userAgentString
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.mediaPlaybackRequiresUserGesture = false
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.setSupportZoom(false)
        s.builtInZoomControls = false
        s.displayZoomControls = false
        s.setSupportMultipleWindows(true)
        s.javaScriptCanOpenWindowsAutomatically = false
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        applyUserAgent()

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.setBackgroundColor(Color.BLACK)
        webView.webViewClient = Client()
        webView.webChromeClient = ChromeClient()
        // Фокус нужен, чтобы при нажатии на поле поиска появлялась клавиатура
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()
    }

    private fun applyUserAgent() {
        webView.settings.userAgentString =
            if (prefs.getBoolean(KEY_DESKTOP, true)) DESKTOP_UA else defaultUserAgent
    }

    /** Разрешаем переходы только внутри сайта (и его зеркал), чтобы реклама не уводила на чужие страницы. */
    private fun isSiteUrl(uri: Uri): Boolean {
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return false
        val home = homeHost
        return host == home || host.endsWith(".$home") || host.contains("kinogo")
    }

    private inner class Client : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
            if (AdBlocker.isBlocked(request.url)) AdBlocker.emptyResponse() else null

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url
            if (url.scheme != "http" && url.scheme != "https") return true // intent://, market:// и т.п.
            if (!request.isForMainFrame) return false // плееры в iframe не трогаем
            if (isSiteUrl(url)) return false
            toast("Заблокирован переход на ${url.host}")
            return true
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            progress.visibility = View.VISIBLE
        }

        override fun onPageCommitVisible(view: WebView, url: String) {
            SiteTweaks.apply(view, prefs.getInt(KEY_PAGE_WIDTH, 0))
            view.evaluateJavascript(navScript, null)
        }

        override fun onPageFinished(view: WebView, url: String) {
            progress.visibility = View.GONE
            SiteTweaks.apply(view, prefs.getInt(KEY_PAGE_WIDTH, 0))
            view.evaluateJavascript(navScript, null)
            if (isSiteUrl(Uri.parse(url))) prefs.edit().putString(KEY_LAST_URL, url).apply()
        }
    }

    private inner class ChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            progress.progress = newProgress
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
            setCursorActive(true)
            cursor.autoHide = true
        }

        override fun onHideCustomView() = exitFullscreen()

        // Ссылки target=_blank и window.open: открываем внутри приложения, если это наш сайт
        override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
            if (!isUserGesture) return false
            val temp = WebView(this@MainActivity)
            temp.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                    if (isSiteUrl(r.url)) webView.loadUrl(r.url.toString())
                    else toast("Заблокировано всплывающее окно")
                    v.destroy()
                    return true
                }
            }
            (resultMsg.obj as WebView.WebViewTransport).webView = temp
            resultMsg.sendToTarget()
            return true
        }

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
        cursor.autoHide = false
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
                // В полноэкранном плеере при спрятанном курсоре ←/→ перематывают видео
                val seekKey = code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT
                if (inFullscreen && !cursor.cursorVisible && seekKey) {
                    if (down) sendKeyToPage(code)
                    return true
                }
                if (!cursorActive) {
                    val now = SystemClock.uptimeMillis()
                    if (down && now - lastNavKey > 110) {
                        lastNavKey = now
                        navMove(code)
                    }
                    return true
                }
                if (down && event.repeatCount == 0) cursor.onDirectionKey(code, true)
                else if (up) cursor.onDirectionKey(code, false)
                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (down) {
                    if (event.repeatCount == 0) okLongPressed = false
                    else if (!okLongPressed && (event.isLongPress || event.repeatCount >= 3)) {
                        okLongPressed = true
                        showMenu()
                    }
                } else if (up && !okLongPressed) {
                    if (inFullscreen && !cursor.cursorVisible) sendKeyToPage(KeyEvent.KEYCODE_SPACE) // пауза/продолжить
                    else if (!cursorActive) navClick()
                    else click()
                }
                return true
            }

            KeyEvent.KEYCODE_BACK -> {
                if (up) handleBack()
                return true
            }

            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SETTINGS -> {
                if (up) showMenu()
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
        cursor.show()
        cursor.pulse()
        tapAt(cursor.cursorX, cursor.cursorY)
    }

    private fun setCursorActive(active: Boolean) {
        cursorActive = active
        cursor.releaseAll()
        cursor.visibility = if (active) View.VISIBLE else View.GONE
        if (active) cursor.show()
    }

    private fun dirName(code: Int) = when (code) {
        KeyEvent.KEYCODE_DPAD_UP -> "up"
        KeyEvent.KEYCODE_DPAD_DOWN -> "down"
        KeyEvent.KEYCODE_DPAD_LEFT -> "left"
        else -> "right"
    }

    /** Шаг навигации по элементам страницы. Если скрипт ещё не загрузился — подгружаем и повторяем. */
    private fun navMove(code: Int, retry: Boolean = true) {
        val dir = dirName(code)
        webView.evaluateJavascript("(window.__ktv?__ktv.move('$dir'):'missing')") { result ->
            if (result?.trim('"') == "missing" && retry) {
                webView.evaluateJavascript(navScript) { navMove(code, retry = false) }
            }
        }
    }

    private fun navClick() {
        webView.evaluateJavascript("(window.__ktv?__ktv.click():'missing')") { raw ->
            val result = raw?.trim('"') ?: return@evaluateJavascript
            if (result == "missing") {
                webView.evaluateJavascript(navScript, null)
                return@evaluateJavascript
            }
            val kind = result.substringBefore(':', "")
            val parts = result.substringAfter(':', "").split(',').mapNotNull { it.toFloatOrNull() }
            if (parts.size != 3 || parts[2] <= 0f) return@evaluateJavascript
            // Перевод CSS-пикселей страницы в пиксели экрана
            val factor = webView.width / parts[2]
            val x = parts[0] * factor
            val y = parts[1] * factor
            if (kind == "player") {
                // В плеер (чужой iframe) можно попасть только курсором — включаем его в центре плеера
                setCursorActive(true)
                cursor.moveTo(x, y)
                cursor.pulse()
                toast("Курсор для плеера. «Назад» — вернуться к навигации стрелками")
            }
            tapAt(x, y)
        }
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

    /** Наведение мышью — чтобы раскрывались меню сайта, срабатывающие при наведении. */
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

    /** Нажатие клавиши внутри страницы/плеера (пробел — пауза, стрелки — перемотка). */
    private fun sendKeyToPage(keyCode: Int) {
        val target = inputTarget()
        val now = SystemClock.uptimeMillis()
        target.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
        target.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
    }

    private fun handleBack() {
        when {
            inFullscreen -> exitFullscreen()
            cursorActive && prefs.getBoolean(KEY_NAV, true) -> setCursorActive(false)
            webView.canGoBack() -> webView.goBack()
            SystemClock.uptimeMillis() - lastBackPress < 2000 -> finish()
            else -> {
                lastBackPress = SystemClock.uptimeMillis()
                toast("Нажми «Назад» ещё раз, чтобы выйти")
            }
        }
    }

    // ---------- Меню (кнопка ☰ на пульте или долгое нажатие OK) ----------

    private fun showMenu() {
        cursor.releaseAll()
        val desktop = prefs.getBoolean(KEY_DESKTOP, true)
        val items = arrayOf(
            "🏠  Главная",
            "🔄  Обновить страницу",
            "🎮  Управление: ${if (prefs.getBoolean(KEY_NAV, true)) "стрелками по сайту" else "только курсор"}",
            "🖥  Режим сайта: ${if (desktop) "как на ПК" else "как на телефоне"}",
            "🔍  Размер страницы: ${pageWidthName(prefs.getInt(KEY_PAGE_WIDTH, 0))}",
            "🖱  Скорость курсора: ${speedName(prefs.getFloat(KEY_CURSOR_SPEED, 1f))}",
            "🌐  Адрес сайта: $homeHost",
            "⬇️  Проверить обновления",
            "🧹  Очистить кэш",
            "ℹ️  О приложении",
            "🚪  Выход",
        )
        dialog().setItems(items) { _, which ->
            when (which) {
                0 -> webView.loadUrl(homeUrl)
                1 -> webView.reload()
                2 -> {
                    val nav = !prefs.getBoolean(KEY_NAV, true)
                    prefs.edit().putBoolean(KEY_NAV, nav).apply()
                    if (!inFullscreen) setCursorActive(!nav)
                }
                3 -> {
                    prefs.edit().putBoolean(KEY_DESKTOP, !desktop).apply()
                    applyUserAgent()
                    webView.reload()
                }
                4 -> choosePageWidth()
                5 -> chooseCursorSpeed()
                6 -> editHome()
                7 -> updater.check(silent = false)
                8 -> {
                    webView.clearCache(true)
                    toast("Кэш очищен")
                }
                9 -> showAbout()
                10 -> finish()
            }
        }.show()
    }

    private val pageWidths = intArrayOf(0, 1280, 1600)
    private fun pageWidthName(w: Int) = when (w) {
        1280 -> "средний"
        1600 -> "мелкий"
        else -> "крупный (как задумал сайт)"
    }

    private fun choosePageWidth() {
        val names = pageWidths.map { pageWidthName(it) }.toTypedArray()
        val current = pageWidths.indexOf(prefs.getInt(KEY_PAGE_WIDTH, 0)).coerceAtLeast(0)
        dialog().setTitle("Размер страницы").setSingleChoiceItems(names, current) { d, which ->
            prefs.edit().putInt(KEY_PAGE_WIDTH, pageWidths[which]).apply()
            d.dismiss()
            webView.reload()
        }.show()
    }

    private val speeds = floatArrayOf(0.6f, 1f, 1.6f)
    private fun speedName(s: Float) = when {
        s < 0.9f -> "медленно"
        s > 1.2f -> "быстро"
        else -> "нормально"
    }

    private fun chooseCursorSpeed() {
        val names = speeds.map { speedName(it) }.toTypedArray()
        val current = speeds.indexOfFirst { speedName(it) == speedName(cursor.speedFactor) }.coerceAtLeast(0)
        dialog().setTitle("Скорость курсора").setSingleChoiceItems(names, current) { d, which ->
            prefs.edit().putFloat(KEY_CURSOR_SPEED, speeds[which]).apply()
            cursor.speedFactor = speeds[which]
            d.dismiss()
        }.show()
    }

    /** Если сайт переедет на новый адрес — его можно поменять без обновления приложения. */
    private fun editHome() {
        val input = EditText(this).apply {
            setText(homeUrl)
            setSelection(text.length)
            setSingleLine()
        }
        dialog().setTitle("Адрес сайта").setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                var url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    if (!url.startsWith("http")) url = "https://$url"
                    prefs.edit().putString(KEY_HOME, url).remove(KEY_LAST_URL).apply()
                    webView.loadUrl(url)
                }
            }
            .setNegativeButton("Отмена", null)
            .setNeutralButton("По умолчанию") { _, _ ->
                prefs.edit().remove(KEY_HOME).remove(KEY_LAST_URL).apply()
                webView.loadUrl(DEFAULT_HOME)
            }
            .show()
    }

    private fun showAbout() {
        @Suppress("DEPRECATION")
        val version = packageManager.getPackageInfo(packageName, 0).versionName
        val webViewVersion = if (Build.VERSION.SDK_INT >= 26) {
            WebView.getCurrentWebViewPackage()?.let { "${it.packageName} ${it.versionName}" } ?: "неизвестно"
        } else "неизвестно"
        dialog().setTitle("KinoTV $version")
            .setMessage(
                "Движок WebView: $webViewVersion\n\n" +
                    "Управление:\n" +
                    "• Стрелки — переход между постерами и кнопками, OK — открыть\n" +
                    "• OK на плеере включает курсор, «Назад» возвращает навигацию стрелками\n" +
                    "• Курсор у края экрана — прокрутка страницы\n" +
                    "• Долгое OK или ☰ — это меню\n" +
                    "• В полноэкранном видео, когда курсор спрятался: ←/→ — перемотка, OK — пауза. " +
                    "Нажми ↑ или ↓, чтобы снова появился курсор"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun dialog() = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    override fun onPause() {
        super.onPause()
        cursor.releaseAll()
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
