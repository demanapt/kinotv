package com.personal.kinotv.data

import android.content.Context

/** Настройки приложения. Ключ "home" совпадает с версией 1.0, чтобы сохранённый адрес сайта не потерялся. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var siteUrl: String
        get() = normalize(prefs.getString(KEY_HOME, null) ?: DEFAULT_SITE)
        set(value) = prefs.edit().putString(KEY_HOME, normalize(value)).apply()

    fun resetSiteUrl() = prefs.edit().remove(KEY_HOME).apply()

    companion object {
        const val DEFAULT_SITE = "https://uakinogo.io/"
        private const val KEY_HOME = "home"

        fun normalize(url: String): String {
            var u = url.trim()
            if (!u.startsWith("http")) u = "https://$u"
            return if (u.endsWith("/")) u else "$u/"
        }
    }
}
