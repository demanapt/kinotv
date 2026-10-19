package com.personal.kinotv.data

import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Загружает страницы сайта. Адрес берётся из настроек при каждом запросе — на случай смены зеркала. */
class SiteClient(cacheDir: File?, private val baseUrl: () -> String) {

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .apply { if (cacheDir != null) cache(Cache(File(cacheDir, "http"), 30L * 1024 * 1024)) }
        .build()

    fun base(): String = baseUrl()

    /** path — относительный путь ("anime/page/2/") или полный адрес. */
    fun get(path: String): Document {
        val url = if (path.startsWith("http")) path else base() + path.removePrefix("/")
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "ru-RU,ru;q=0.9,uk;q=0.8")
            .build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Сайт ответил ${resp.code}")
            val body = resp.body?.string() ?: throw IOException("Пустой ответ")
            return Jsoup.parse(body, resp.request.url.toString())
        }
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
    }
}
