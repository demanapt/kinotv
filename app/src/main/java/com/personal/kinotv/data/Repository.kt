package com.personal.kinotv.data

import android.os.SystemClock
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest

/**
 * Единая точка получения данных для экранов. Все функции — suspend и сами уходят в фоновый поток.
 * Результаты кэшируются в памяти на 10 минут, чтобы возврат «Назад» не грузил всё заново.
 */
class Repository(private val client: SiteClient, private val diskDir: File? = null) {

    private val json = Json { ignoreUnknownKeys = true }

    private class Entry(val time: Long, val value: Any)
    private val cache = LruCache<String, Entry>(64)

    private suspend fun <T : Any> cached(key: String, load: () -> T): T = withContext(Dispatchers.IO) {
        val hit = cache.get(key)
        @Suppress("UNCHECKED_CAST")
        if (hit != null && SystemClock.elapsedRealtime() - hit.time < TTL_MS) return@withContext hit.value as T
        load().also { cache.put(key, Entry(SystemClock.elapsedRealtime(), it)) }
    }

    /** Страница раздела или жанра (page начинается с 1). */
    suspend fun category(slug: String, page: Int = 1): Page = cached("cat:$slug:$page") {
        Parsers.parsePage(client.get(if (page <= 1) "$slug/" else "$slug/page/$page/"))
    }

    /** Поиск по названию (page начинается с 1). */
    suspend fun search(query: String, page: Int = 1): Page {
        val q = query.trim()
        if (q.length < 2) return Page(emptyList(), false)
        val enc = URLEncoder.encode(q, "UTF-8").replace("+", "%20")
        return cached("search:$q:$page") {
            Parsers.parsePage(client.get(if (page <= 1) "search/$enc/" else "search/$enc/page/$page/"))
        }
    }

    /** Новые серии с главной — с большими постерами для баннера. */
    suspend fun newEpisodes(): List<EpisodeUpdate> = cached("lastSeries") {
        Parsers.parseLastSeries(client.get(""))
    }

    /**
     * Полная информация о тайтле и его плееры. Свежая копия сохраняется на приставку, а если сайт
     * недоступен — отдаём сохранённую. fresh = true — мимо кэша в памяти (нужно перед запуском плеера:
     * ссылки на плееры одноразовые и быстро протухают).
     */
    suspend fun details(url: String, fresh: Boolean = false): Details {
        if (fresh) cache.remove("details:$url")
        return try {
            cached("details:$url") {
                Parsers.parseDetails(client.get(url), url).also { saveToDisk(url, it) }
            }
        } catch (e: Exception) {
            cachedDetails(url) ?: throw e
        }
    }

    /** Сохранённая на приставке карточка — для мгновенного показа, пока грузится свежая. */
    fun cachedDetails(url: String): Details? = runCatching {
        diskFile(url)?.takeIf { it.exists() }?.readText()?.let { json.decodeFromString(Details.serializer(), it) }
    }.getOrNull()

    private fun saveToDisk(url: String, details: Details) {
        runCatching { diskFile(url)?.writeText(json.encodeToString(Details.serializer(), details)) }
    }

    private fun diskFile(url: String): File? {
        val dir = diskDir ?: return null
        dir.mkdirs()
        val hash = MessageDigest.getInstance("MD5").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(dir, "$hash.json")
    }

    fun clearMemoryCache() = cache.evictAll()

    private companion object {
        const val TTL_MS = 10 * 60 * 1000L
    }
}
