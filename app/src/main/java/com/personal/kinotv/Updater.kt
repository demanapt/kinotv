package com.personal.kinotv

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.ProgressBar
import android.util.Log
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Обновления через GitHub Releases. Каждый релиз содержит KinoTV.apk и version.json:
 * {"versionCode": 2, "versionName": "1.1", "apkUrl": "...", "notes": "что нового"}
 */
class Updater(private val activity: Activity) {

    companion object {
        const val UPDATE_URL = "https://github.com/demanapt/kinotv/releases/latest/download/version.json"
        const val ACTION_INSTALL_STATUS = "com.personal.kinotv.INSTALL_STATUS"
        private const val TAG = "KinoUpdate"
    }

    private val main = Handler(Looper.getMainLooper())

    fun check(silent: Boolean) {
        if (!silent) toast("Проверяю обновления…")
        thread {
            try {
                Log.i(TAG, "Проверка: $UPDATE_URL")
                val json = JSONObject(download(UPDATE_URL).toString(Charsets.UTF_8))
                Log.i(TAG, "Ответ: $json")
                val code = json.getInt("versionCode")
                val name = json.optString("versionName", code.toString())
                val apkUrl = json.getString("apkUrl")
                val notes = json.optString("notes", "")
                main.post {
                    if (activity.isFinishing) return@post
                    if (code > currentVersionCode()) offer(name, notes, apkUrl)
                    else if (!silent) toast("У тебя последняя версия (${currentVersionName()})")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Ошибка проверки обновлений", e)
                if (!silent) main.post { toast("Не удалось проверить обновления: ${e.message}") }
            }
        }
    }

    private fun offer(name: String, notes: String, apkUrl: String) {
        val message = buildString {
            append("Доступна версия $name (сейчас ${currentVersionName()}).")
            if (notes.isNotBlank()) append("\n\nЧто нового:\n").append(notes)
        }
        AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Обновление KinoTV")
            .setMessage(message)
            .setPositiveButton("Обновить") { _, _ -> if (ensureInstallAllowed()) downloadAndInstall(apkUrl) }
            .setNegativeButton("Позже", null)
            .show()
    }

    /** Android 8+ требует один раз разрешить приложению устанавливать APK. */
    private fun ensureInstallAllowed(): Boolean {
        if (Build.VERSION.SDK_INT < 26 || activity.packageManager.canRequestPackageInstalls()) return true
        AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Нужно разрешение")
            .setMessage(
                "Один раз разреши KinoTV устанавливать обновления: в открывшихся настройках включи " +
                    "«Разрешить из этого источника», вернись назад и снова нажми «Проверить обновления» в меню."
            )
            .setPositiveButton("Открыть настройки") { _, _ ->
                try {
                    activity.startActivity(
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
                    )
                } catch (e: ActivityNotFoundException) {
                    toast("Открой вручную: Настройки → Безопасность → Неизвестные источники → KinoTV")
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
        return false
    }

    private fun downloadAndInstall(apkUrl: String) {
        val bar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            setPadding(48, 32, 48, 32)
        }
        val dialog = AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Скачиваю обновление…")
            .setView(bar)
            .setCancelable(false)
            .show()
        thread {
            try {
                val file = File(activity.cacheDir, "update.apk")
                downloadTo(apkUrl, file) { percent -> main.post { bar.progress = percent } }
                install(file)
                main.post { dialog.dismiss() }
            } catch (e: Exception) {
                main.post {
                    dialog.dismiss()
                    toast("Ошибка загрузки обновления: ${e.message}")
                }
            }
        }
    }

    private fun install(apk: File) {
        val installer = activity.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("kinotv.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val intent = Intent(activity, MainActivity::class.java).setAction(ACTION_INSTALL_STATUS)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            val pending = PendingIntent.getActivity(activity, sessionId, intent, flags)
            session.commit(pending.intentSender)
        }
    }

    /** Вызывается из MainActivity, когда система сообщает о ходе установки. */
    fun handleInstallStatus(intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, Int.MIN_VALUE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
                activity.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PackageInstaller.STATUS_SUCCESS -> toast("Обновление установлено")
            PackageInstaller.STATUS_FAILURE_ABORTED -> toast("Установка отменена")
            else -> toast("Не удалось установить: ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}")
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            Log.i(TAG, "GET $url")
            connectTimeout = 15000
            readTimeout = 30000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "KinoTV-Updater")
            if (responseCode !in 200..299) throw IllegalStateException("HTTP $responseCode")
        }

    private fun download(url: String): ByteArray {
        val conn = open(url)
        try {
            return conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadTo(url: String, file: File, onProgress: (Int) -> Unit) {
        val conn = open(url)
        try {
            val total = conn.contentLength.toLong()
            var done = 0L
            var lastPercent = -1
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) {
                            val p = (done * 100 / total).toInt()
                            if (p != lastPercent) { lastPercent = p; onProgress(p) }
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    @Suppress("DEPRECATION")
    private fun currentVersionCode(): Long {
        val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    }

    private fun currentVersionName(): String =
        activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "?"

    private fun toast(text: String) = Toast.makeText(activity, text, Toast.LENGTH_LONG).show()
}
