package com.personal.kinotv.data

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Проверка разбора на живом сайте. Запускается только вручную:
 * ./gradlew testReleaseUnitTest -Plive=true --tests '*LiveSiteTest*'
 */
class LiveSiteTest {

    private val client = SiteClient(null) { AppSettings.DEFAULT_SITE }

    @Test
    fun liveSite() {
        assumeTrue(System.getProperty("live") == "true")
        val home = client.get("")
        val updates = Parsers.parseLastSeries(home)
        println("Новые серии: ${updates.size}, пример: ${updates.firstOrNull()?.card?.title}")
        assertTrue(updates.isNotEmpty())

        for (slug in Sections.homeRows.map { it.slug }) {
            val page = Parsers.parsePage(client.get("$slug/"))
            println("$slug: ${page.items.size} карточек, дальше=${page.hasNext}")
            assertTrue(slug, page.items.isNotEmpty())
        }

        val page2 = Parsers.parsePage(client.get("anime/page/2/"))
        assertTrue(page2.items.isNotEmpty())

        val search = Parsers.parsePage(client.get("search/%D0%BD%D0%B0%D1%80%D1%83%D1%82%D0%BE/"))
        println("Поиск «наруто»: ${search.items.size}")
        assertTrue(search.items.isNotEmpty())

        val first = Parsers.parsePage(client.get("serialy/")).items.first()
        val d = Parsers.parseDetails(client.get(first.url), first.url)
        println("Детали: ${d.title} / ${d.originalTitle} / ${d.year} / плееров ${d.players.size} / похожих ${d.related.size}")
        assertTrue(d.players.isNotEmpty())
    }
}
