package com.personal.kinotv.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Разбор HTML сайта (движок DLE). Подписи полей встречаются и на русском, и на украинском,
 * поэтому сравниваем с несколькими вариантами.
 */
object Parsers {

    private val YEAR_LABELS = setOf("год выпуска", "рік випуску", "год", "рік")
    private val COUNTRY_LABELS = setOf("страна", "країна")
    private val GENRE_LABELS = setOf("жанр")
    private val ORIGINAL_LABELS = setOf("зарубіжна назва", "оригинальное название", "оригінальна назва", "название")
    private val TITLE_YEAR = Regex("""\s*\((\d{4})\)\s*$""")
    private val RATING = Regex("""([\d.,]+)\s*/\s*5""")

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Карточки из раздела, поиска или главной. */
    fun parseCards(doc: Document): List<Card> =
        doc.select("article.shortStory").mapNotNull { parseCard(it) }

    private fun parseCard(el: Element): Card? {
        val link = el.selectFirst(".hTitle a[href]") ?: el.selectFirst("a[href]") ?: return null
        val (title, titleYear) = splitYear(link.text())
        val info = infoFields(el)
        return Card(
            url = link.absUrl("href"),
            title = title,
            poster = imageUrl(el.selectFirst(".sPoster img")),
            year = info.firstValue(YEAR_LABELS) ?: titleYear,
            genre = info.firstValue(GENRE_LABELS),
            excerpt = el.selectFirst("p.excerpt")?.text()?.takeIf { it.isNotBlank() },
            rating = parseRating(el.selectFirst(".ratingStats")?.text()),
        )
    }

    /** Есть ли следующая страница: в блоке pagiNation есть ссылка на номер больше текущего. */
    fun parseHasNextPage(doc: Document): Boolean {
        val nav = doc.selectFirst(".pagiNation") ?: return false
        val current = nav.select("span").mapNotNull { it.text().trim().toIntOrNull() }.firstOrNull() ?: 1
        return nav.select("a").any { (it.text().trim().toIntOrNull() ?: 0) > current }
    }

    fun parsePage(doc: Document) = Page(parseCards(doc), parseHasNextPage(doc))

    /** Новые серии (блоки lastSeries есть на любой странице). Одна серия в разных озвучках — одна запись. */
    fun parseLastSeries(doc: Document): List<EpisodeUpdate> =
        doc.select("a.lastSeries[href]").mapNotNull { a ->
            val img = a.selectFirst("img.lastSeries-poster")
            val titleEl = a.selectFirst(".lastSeries-title") ?: return@mapNotNull null
            val big = img?.absUrl("data-val").orEmpty()
            val small = imageUrl(img)
            EpisodeUpdate(
                card = Card(
                    url = a.absUrl("href"),
                    title = titleEl.ownText().trim(),
                    poster = small.ifEmpty { big },
                    year = titleEl.selectFirst("span")?.text()?.trim(),
                ),
                bigPoster = big.ifEmpty { small },
                season = a.selectFirst(".lastSeries-season")?.text(),
                episode = a.selectFirst(".lastSeries-episode")?.text(),
                translation = a.selectFirst(".lastSeries-translation")?.text(),
                category = a.selectFirst(".lastSeries-category")?.text(),
            )
        }.distinctBy { it.card.url }

    /** Страница фильма или сериала. */
    fun parseDetails(doc: Document, url: String): Details {
        val ld = findLdJson(doc)
        val story = doc.selectFirst(".fullStory") ?: doc.body()
        val info = infoFields(story)
        val (h1Title, h1Year) = splitYear(story.selectFirst("h1")?.text() ?: ld?.str("name").orEmpty())
        val ldType = ld?.str("@type").orEmpty()

        val players = doc.select("ul.js-player-tabs li[data-src]").mapIndexedNotNull { i, li ->
            val src = li.absUrl("data-src").ifEmpty { li.attr("data-src") }
            if (src.isBlank()) null else PlayerSource(li.text().ifBlank { "Плеер ${i + 1}" }, src)
        }.ifEmpty {
            doc.select(".sectionPlayer iframe").mapNotNull { f ->
                val src = f.absUrl("data-src").ifEmpty { f.absUrl("src") }
                if (src.isBlank()) null else PlayerSource("Смотреть", src)
            }
        }

        val related = doc.select(".viewMore .relatedItem a[href]").map { a ->
            val (title, year) = splitYear(a.selectFirst("span")?.text() ?: a.attr("title"))
            Card(
                url = a.absUrl("href"),
                title = title,
                poster = imageUrl(a.selectFirst("img")),
                year = year ?: splitYear(a.attr("title")).second,
            )
        }

        val ratingObj = ld?.get("aggregateRating") as? JsonObject
        return Details(
            url = url,
            title = h1Title,
            originalTitle = ld?.str("alternateName")?.takeIf { it.isNotBlank() && it != h1Title }
                ?: info.firstValue(ORIGINAL_LABELS),
            year = info.firstValue(YEAR_LABELS) ?: h1Year ?: ld?.str("dateCreated"),
            country = info.firstValue(COUNTRY_LABELS),
            genres = ld?.strList("genre")?.map { it.replaceFirstChar(Char::titlecase) }
                ?: info.firstValue(GENRE_LABELS)?.split("/", ",")?.map { it.trim() }.orEmpty(),
            description = ld?.str("description")?.takeIf { it.isNotBlank() }
                ?: story.selectFirst("p.excerpt, .fullText, .fDesc")?.text(),
            actors = ld?.names("actor").orEmpty(),
            directors = ld?.names("director").orEmpty(),
            rating = ratingObj?.str("ratingValue")?.replace(',', '.')?.toFloatOrNull()
                ?: parseRating(story.selectFirst(".ratingStats")?.text()),
            ratingVotes = ratingObj?.str("ratingCount")?.toIntOrNull(),
            poster = imageUrl(story.selectFirst(".sPoster img")),
            players = players,
            related = related,
            isSeries = ldType.startsWith("TV") || "serial" in url || "сезон" in h1Title.lowercase(),
        )
    }

    // ---------- вспомогательное ----------

    /** Название без года в скобках + год отдельно. */
    fun splitYear(raw: String): Pair<String, String?> {
        val text = raw.trim()
        val m = TITLE_YEAR.find(text) ?: return text to null
        return text.removeRange(m.range).trim() to m.groupValues[1]
    }

    private fun parseRating(text: String?): Float? =
        text?.let { RATING.find(it)?.groupValues?.get(1)?.replace(',', '.')?.toFloatOrNull() }

    /** Картинки грузятся лениво: настоящий адрес в data-src, а в src — заглушка data:. */
    private fun imageUrl(img: Element?): String {
        if (img == null) return ""
        val lazy = img.absUrl("data-src")
        if (lazy.isNotEmpty()) return lazy
        val src = img.attr("src")
        return if (src.startsWith("data:")) "" else img.absUrl("src")
    }

    /** Поля из блока .sInfo: «<b>Подпись:</b> значение». */
    private fun infoFields(root: Element): Map<String, String> =
        root.select(".sInfo > span").mapNotNull { span ->
            val label = span.selectFirst("b")?.text()?.trim()?.trimEnd(':')?.lowercase() ?: return@mapNotNull null
            val value = span.text().removePrefix(span.selectFirst("b")!!.text()).trim()
            if (value.isBlank() || value.equals("Неизвестно", true) || value.equals("Невідомо", true)) null
            else label to value
        }.toMap()

    private fun Map<String, String>.firstValue(labels: Set<String>) = labels.firstNotNullOfOrNull { this[it] }

    private fun findLdJson(doc: Document): JsonObject? =
        doc.select("script[type=application/ld+json]").firstNotNullOfOrNull { script ->
            val el = runCatching { json.parseToJsonElement(script.data()) }.getOrNull()
            val candidates = when (el) {
                is JsonArray -> el.filterIsInstance<JsonObject>()
                is JsonObject -> listOf(el)
                else -> emptyList()
            }
            candidates.firstOrNull { it.str("@type") in setOf("Movie", "TVSeries", "TVEpisode", "TVSeason", "VideoObject") }
        }

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.strList(key: String): List<String>? = when (val v = this[key]) {
        is JsonArray -> v.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        is JsonPrimitive -> v.contentOrNull?.let { listOf(it) }
        else -> null
    }

    private fun JsonObject.names(key: String): List<String> {
        fun name(e: JsonElement): String? = when (e) {
            is JsonObject -> e.str("name")
            is JsonPrimitive -> e.contentOrNull
            else -> null
        }
        return when (val v = this[key]) {
            is JsonArray -> v.mapNotNull(::name)
            null -> emptyList()
            else -> listOfNotNull(name(v))
        }
    }
}
