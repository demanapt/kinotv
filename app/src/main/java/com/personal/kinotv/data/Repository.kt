package com.personal.kinotv.data

import android.os.SystemClock
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

/**
 * Единая точка получения данных для экранов. Все функции — suspend и сами уходят в фоновый поток.
 * Результаты кэшируются в памяти на 10 минут, чтобы возврат «Назад» не грузил всё заново.
 */
class Repository(private val client: SiteClient) {

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

    /** Полная информация о тайтле и его плееры. */
    suspend fun details(url: String): Details = cached("details:$url") {
        Parsers.parseDetails(client.get(url), url)
    }

    fun clearMemoryCache() = cache.evictAll()

    private companion object {
        const val TTL_MS = 10 * 60 * 1000L
    }
}
