package com.personal.kinotv.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.personal.kinotv.data.Card
import com.personal.kinotv.data.Page
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Список с подгрузкой страниц. Экран вызывает loadMore(), когда фокус подходит к концу списка
 * (например, за 5 элементов до конца).
 */
@Stable
class PagedList(private val scope: CoroutineScope, private val load: suspend (page: Int) -> Page) {
    var items by mutableStateOf<List<Card>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var endReached by mutableStateOf(false)
        private set

    private var nextPage = 1

    fun loadMore() {
        if (isLoading || endReached) return
        isLoading = true
        error = null
        scope.launch {
            try {
                val page = load(nextPage)
                val known = items.mapTo(HashSet()) { it.url }
                items = items + page.items.filter { it.url !in known }
                nextPage++
                endReached = !page.hasNext || page.items.isEmpty()
            } catch (e: Exception) {
                error = e.message ?: "Ошибка загрузки"
            } finally {
                isLoading = false
            }
        }
    }

    fun retry() = loadMore()
}

/** Создаёт PagedList и сразу грузит первую страницу. При смене key начинает заново. */
@Composable
fun rememberPagedList(key: Any?, load: suspend (page: Int) -> Page): PagedList {
    val scope = rememberCoroutineScope()
    return remember(key) { PagedList(scope, load).also { it.loadMore() } }
}
