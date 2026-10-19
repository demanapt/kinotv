package com.personal.kinotv.data

import kotlinx.serialization.Serializable

/** Карточка тайтла в списках: разделы, поиск, «Похожие», избранное. */
@Serializable
data class Card(
    val url: String,
    val title: String,
    val poster: String,
    val year: String? = null,
    val genre: String? = null,
    val excerpt: String? = null,
    /** Рейтинг сайта по 5-балльной шкале. */
    val rating: Float? = null,
)

/** Страница списка и признак, что дальше есть ещё. */
data class Page(val items: List<Card>, val hasNext: Boolean)

/** Вкладка плеера на странице тайтла («Смотреть онлайн», «Плеер 1»…). */
data class PlayerSource(val label: String, val url: String)

/** Новая серия из блока «последние обновления» — с большим постером для баннера. */
data class EpisodeUpdate(
    val card: Card,
    /** Большой постер (вертикальный, хорошего качества). */
    val bigPoster: String,
    val season: String? = null,
    val episode: String? = null,
    val translation: String? = null,
    val category: String? = null,
)

data class Details(
    val url: String,
    val title: String,
    val originalTitle: String? = null,
    val year: String? = null,
    val country: String? = null,
    val genres: List<String> = emptyList(),
    val description: String? = null,
    val actors: List<String> = emptyList(),
    val directors: List<String> = emptyList(),
    val rating: Float? = null,
    val ratingVotes: Int? = null,
    val poster: String,
    val players: List<PlayerSource> = emptyList(),
    val related: List<Card> = emptyList(),
    val isSeries: Boolean = false,
) {
    fun toCard() = Card(url = url, title = title, poster = poster, year = year, genre = genres.firstOrNull(), excerpt = description, rating = rating)
}

/** Раздел сайта: адрес вида /{slug}/page/{n}/ */
data class Section(val slug: String, val title: String)

object Sections {
    val NEW = Section("novinki", "Новинки")
    val MOVIES = Section("filmy", "Фильмы")
    val SERIES = Section("serialy", "Сериалы")
    val ANIME = Section("anime", "Аниме")
    val ANIME_SERIES = Section("anime-serialy", "Аниме-сериалы")
    val CARTOONS = Section("multfilmy", "Мультфильмы")
    val CARTOON_SERIES = Section("multserialy", "Мультсериалы")
    val FOREIGN_SERIES = Section("zarubezhnye-serialy", "Зарубежные сериалы")

    /** Ряды на главной (после «Продолжить просмотр» и «Новые серии»). */
    val homeRows = listOf(NEW, MOVIES, SERIES, ANIME, ANIME_SERIES, CARTOONS)

    /** Жанры — фильтры в разделах. */
    val genres = listOf(
        Section("boevik", "Боевик"), Section("komedija", "Комедия"), Section("drama", "Драма"),
        Section("fantastika", "Фантастика"), Section("fjentezi", "Фэнтези"), Section("triller", "Триллер"),
        Section("uzhasy", "Ужасы"), Section("prikljuchenija", "Приключения"), Section("kriminal", "Криминал"),
        Section("detektiv", "Детектив"), Section("melodrama", "Мелодрама"), Section("istoricheskie", "Исторические"),
        Section("voennyj", "Военный"), Section("dokumentalnye", "Документальные"), Section("detskij", "Детский"),
    )
}
