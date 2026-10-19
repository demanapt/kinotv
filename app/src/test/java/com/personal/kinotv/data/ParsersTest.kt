package com.personal.kinotv.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParsersTest {

    private fun fixture(name: String, url: String = "https://uakinogo.io/"): Document {
        val stream = javaClass.classLoader!!.getResourceAsStream("fixtures/$name")!!
        return stream.use { Jsoup.parse(it, "UTF-8", url) }
    }

    @Test
    fun categoryCards() {
        val page = Parsers.parsePage(fixture("category.html", "https://uakinogo.io/anime/"))
        assertTrue("карточек: ${page.items.size}", page.items.size >= 10)
        assertTrue(page.hasNext)
        page.items.forEach {
            assertTrue(it.url, it.url.startsWith("https://uakinogo.io/") && it.url.endsWith(".html"))
            assertTrue(it.title, it.title.isNotBlank() && !it.title.endsWith(")") || "сезон" in it.title)
            assertTrue(it.poster, it.poster.startsWith("https://uakinogo.io/uploads/"))
        }
        val first = page.items.first()
        assertEquals("2025", first.year)
        assertNotNull(first.excerpt)
        assertNotNull(first.rating)
    }

    @Test
    fun searchCardsAndPagination() {
        val page = Parsers.parsePage(fixture("search.html"))
        assertEquals(20, page.items.size)
        assertTrue(page.hasNext)
        assertTrue(page.items.first().title.startsWith("Наруто"))
    }

    @Test
    fun homeLastSeries() {
        val updates = Parsers.parseLastSeries(fixture("home.html"))
        assertTrue("обновлений: ${updates.size}", updates.isNotEmpty())
        assertEquals(updates.size, updates.distinctBy { it.card.url }.size)
        updates.forEach {
            assertTrue(it.bigPoster, it.bigPoster.contains("/uploads/"))
            assertTrue(it.card.title.isNotBlank())
        }
        assertTrue(updates.any { it.episode != null && it.season != null })
    }

    @Test
    fun movieDetails() {
        val url = "https://uakinogo.io/filmy/118084-serdce-zverja.html"
        val d = Parsers.parseDetails(fixture("movie.html", url), url)
        assertEquals("Сердце зверя", d.title)
        assertEquals("Heart of the Beast", d.originalTitle)
        assertEquals("2026", d.year)
        assertEquals("США", d.country)
        assertTrue(d.genres.contains("Драма"))
        assertTrue(d.description!!.length > 50)
        assertTrue(d.actors.contains("Брэд Питт"))
        assertEquals(4.2f, d.rating!!, 0.01f)
        assertTrue(d.poster.contains("/uploads/mini/fullstory/"))
        assertEquals(2, d.players.size)
        assertTrue(d.players.all { it.url.startsWith("https://") })
        assertTrue(d.related.isNotEmpty())
        assertFalse(d.isSeries)
    }

    @Test
    fun seriesDetails() {
        val url = "https://uakinogo.io/anime-serialy/56855-bezumnye-bajki-devichej-obschagi.html"
        val d = Parsers.parseDetails(fixture("series.html", url), url)
        assertTrue(d.title.isNotBlank())
        assertTrue(d.isSeries)
        assertTrue(d.players.isNotEmpty())
        assertTrue(d.poster.isNotEmpty())
    }

    @Test
    fun splitYear() {
        assertEquals("Сердце зверя" to "2026", Parsers.splitYear("Сердце зверя (2026) "))
        assertEquals("Наруто (1-5 сезон)" to null, Parsers.splitYear("Наруто (1-5 сезон)"))
    }
}
