package com.personal.kinotv.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** Избранное, «Продолжить просмотр» и места остановки. Хранится в JSON-файле на приставке. */
class Library(private val file: File) {

    @Serializable
    private data class Data(
        val favorites: List<Card> = emptyList(),
        val history: List<Card> = emptyList(),
        val progress: Map<String, WatchProgress> = emptyMap(),
    )

    private val json = Json { ignoreUnknownKeys = true }
    private var data = load()

    private val _favorites = MutableStateFlow(data.favorites)
    private val _history = MutableStateFlow(data.history)
    val favorites: StateFlow<List<Card>> = _favorites.asStateFlow()
    val history: StateFlow<List<Card>> = _history.asStateFlow()
    private val _progress = MutableStateFlow(data.progress)

    /** Где остановился просмотр, по адресу страницы тайтла. */
    val progress: StateFlow<Map<String, WatchProgress>> = _progress.asStateFlow()

    fun progressFor(url: String): WatchProgress? = data.progress[url]

    @Synchronized
    fun saveProgress(url: String, progress: WatchProgress) {
        // Храним только последние тайтлы, чтобы файл не рос бесконечно
        val map = (data.progress + (url to progress)).entries
            .sortedByDescending { it.value.updatedAt }
            .take(MAX_PROGRESS)
            .associate { it.key to it.value }
        update(data.copy(progress = map))
    }

    fun isFavorite(url: String) = data.favorites.any { it.url == url }

    @Synchronized
    fun toggleFavorite(card: Card) {
        val list = data.favorites
        update(data.copy(favorites = if (list.any { it.url == card.url }) list.filterNot { it.url == card.url } else listOf(card) + list))
    }

    /** Поднимает тайтл в начало «Продолжить просмотр». */
    @Synchronized
    fun addToHistory(card: Card) {
        update(data.copy(history = (listOf(card) + data.history.filterNot { it.url == card.url }).take(MAX_HISTORY)))
    }

    @Synchronized
    fun removeFromHistory(url: String) = update(data.copy(history = data.history.filterNot { it.url == url }))

    private fun update(new: Data) {
        data = new
        _favorites.value = new.favorites
        _history.value = new.history
        _progress.value = new.progress
        runCatching { file.writeText(json.encodeToString(Data.serializer(), new)) }
    }

    private fun load(): Data =
        runCatching { json.decodeFromString(Data.serializer(), file.readText()) }.getOrDefault(Data())

    private companion object {
        const val MAX_HISTORY = 40
        const val MAX_PROGRESS = 200
    }
}
