package com.personal.kinotv.player

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONObject

/**
 * Связь с плеером сайта. Страница плеера открывается напрямую (не в iframe), поэтому в неё можно
 * встроить скрипт player_hook.js через evaluateJavascript, а ответы получать через JavascriptInterface —
 * это работает в WebView любой версии, в том числе старой на Mi Box.
 * Скрипт сообщает время и серию, приложение отправляет команды пульта.
 */
class PlayerBridge(private val webView: WebView, private val listener: Listener) {

    interface Listener {
        fun onProgress(time: Double, duration: Double, paused: Boolean, label: String?)
        fun onResumed(text: String)
    }

    private val main = Handler(Looper.getMainLooper())
    private var script: String = ""

    /** Плеер уже прислал время — значит, команды до него дойдут. */
    var connected = false
        private set

    @SuppressLint("JavascriptInterface", "AddJavascriptInterface")
    fun install(script: String) {
        this.script = script
        webView.addJavascriptInterface(Receiver(), "KinoNative")
    }

    /** Вызывать после загрузки страницы плеера: скрипт сам не встроится повторно. */
    fun inject() {
        if (script.isNotEmpty()) webView.evaluateJavascript(script, null)
    }

    fun reset() {
        connected = false
    }

    private inner class Receiver {
        // Вызывается из JS в фоновом потоке WebView — переносим в главный
        @JavascriptInterface
        fun post(message: String) {
            main.post { handle(message) }
        }
    }

    private fun handle(message: String) {
        val json = runCatching { JSONObject(message) }.getOrNull() ?: return
        when (json.optString("type")) {
            "progress" -> {
                connected = true
                listener.onProgress(
                    json.optDouble("time"),
                    json.optDouble("duration"),
                    json.optBoolean("paused"),
                    json.optString("label").takeIf { it.isNotBlank() && it != "null" },
                )
            }
            "resume" -> listener.onResumed(json.optString("text"))
            "dump" -> Log.i(TAG, "Плеер: " + json.optJSONArray("items"))
            "clicked" -> Log.i(TAG, "Нажатие кнопки плеера: " + json.optBoolean("ok"))
            "hello" -> Log.i(TAG, "Скрипт встроен: $json")
        }
    }

    fun toggle() = send("""{"cmd":"toggle"}""")
    fun seek(deltaSeconds: Int) = send("""{"cmd":"seek","delta":$deltaSeconds}""")
    fun seekTo(seconds: Double) = send("""{"cmd":"seekTo","time":$seconds}""")
    fun next() = send("""{"cmd":"next"}""")
    fun prev() = send("""{"cmd":"prev"}""")
    fun dump() = send("""{"cmd":"dump"}""")

    private fun send(json: String) {
        webView.evaluateJavascript("window.__kinoCmd && window.__kinoCmd($json)", null)
    }

    private companion object {
        const val TAG = "KinoPlayer"
    }
}
