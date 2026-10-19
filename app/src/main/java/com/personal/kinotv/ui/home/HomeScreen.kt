package com.personal.kinotv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.personal.kinotv.KinoApp
import com.personal.kinotv.data.Card
import com.personal.kinotv.data.EpisodeUpdate
import com.personal.kinotv.data.Sections
import com.personal.kinotv.ui.common.LocalAppActions
import com.personal.kinotv.ui.common.LocalTabsFocused
import com.personal.kinotv.ui.common.rememberPagedList
import com.personal.kinotv.ui.theme.KinoColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val HISTORY = "history"
private const val EPISODES = "episodes"

private data class HomeFocusTarget(val row: String, val index: Int = 0, val url: String? = null)

private data class HomeContentRow(
    val key: String,
    val title: String,
    val cards: List<Card>,
    val state: LazyListState,
    val loading: Boolean = false,
    val error: String? = null,
    val canLoadMore: Boolean = false,
    val loadMore: () -> Unit = {},
    val retry: () -> Unit = {},
    val badge: (Int) -> String? = { null },
) {
    val focusable: Boolean get() = cards.isNotEmpty() || error != null
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen() {
    val actions = LocalAppActions.current
    val tabsFocused = LocalTabsFocused.current
    val scope = rememberCoroutineScope()
    val history by KinoApp.library.history.collectAsState()
    var episodes by remember { mutableStateOf<List<EpisodeUpdate>>(emptyList()) }
    var episodesLoading by remember { mutableStateOf(true) }
    var episodesError by remember { mutableStateOf<String?>(null) }
    var episodesAttempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(episodesAttempt) {
        episodesLoading = true
        episodesError = null
        try {
            episodes = KinoApp.repository.newEpisodes()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            episodesError = e.message ?: "Ошибка загрузки"
        } finally {
            episodesLoading = false
        }
    }

    // Состояния рядов не зависят от их присутствия в LazyColumn.
    val categories = Sections.homeRows.map { section ->
        rememberPagedList(section.slug) { page -> KinoApp.repository.category(section.slug, page) }
    }
    val categoryStates = Sections.homeRows.map { rememberLazyListState() }
    val historyState = rememberLazyListState()
    val episodesState = rememberLazyListState()
    val columnState = rememberLazyListState()
    val rowFocus = remember {
        (listOf(HISTORY, EPISODES) + Sections.homeRows.map { it.slug })
            .associateWith { HomeRowFocus() }
    }
    var savedRow by rememberSaveable { mutableStateOf<String?>(null) }
    var savedIndex by rememberSaveable { mutableIntStateOf(0) }
    var savedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    // Только вход на экран и повтор загрузки; нажатия стрелок сюда не попадают.
    var restoreTarget by remember {
        mutableStateOf<HomeFocusTarget?>(
            HomeFocusTarget(savedRow ?: if (history.isNotEmpty()) HISTORY else EPISODES, savedIndex, savedUrl),
        )
    }
    var rowNavigation by remember { mutableStateOf<Job?>(null) }

    val episodeCards = remember(episodes) {
        episodes.map { it.card.copy(poster = it.bigPoster.ifBlank { it.card.poster }) }
    }
    val rows = buildList {
        if (history.isNotEmpty()) {
            add(HomeContentRow(HISTORY, "Продолжить просмотр", history, historyState))
        }
        add(
            HomeContentRow(
                EPISODES, "Новые серии", episodeCards, episodesState,
                loading = episodesLoading,
                error = episodesError,
                retry = {
                    episodesLoading = true
                    episodesError = null
                    episodesAttempt++
                },
                badge = { episodes[it].episode },
            ),
        )
        Sections.homeRows.forEachIndexed { index, section ->
            val list = categories[index]
            add(
                HomeContentRow(
                    section.slug, section.title, list.items, categoryStates[index],
                    loading = list.isLoading,
                    error = list.error,
                    canLoadMore = !list.endReached,
                    loadMore = { list.loadMore() },
                    retry = { list.retry() },
                ),
            )
        }
    }
    val focusableRows = rows.filter { it.focusable }
    val restoringRow = rows.firstOrNull { it.key == restoreTarget?.row }

    LaunchedEffect(
        restoreTarget,
        restoringRow?.cards,
        restoringRow?.loading,
        restoringRow?.error,
        restoringRow?.canLoadMore,
        focusableRows.firstOrNull()?.key,
    ) {
        val target = restoreTarget ?: return@LaunchedEffect
        if (tabsFocused.value) {
            restoreTarget = null
            return@LaunchedEffect
        }
        val row = restoringRow
        if (row == null || (!row.focusable && !row.loading && !row.canLoadMore)) {
            focusableRows.firstOrNull()?.let { restoreTarget = HomeFocusTarget(it.key) }
            return@LaunchedEffect
        }
        val foundIndex = row.cards.indexOfFirst { it.url == target.url }
        if (foundIndex < 0 && target.index >= row.cards.size && row.canLoadMore && row.error == null) {
            if (!row.loading) row.loadMore()
            return@LaunchedEffect
        }
        if (!row.focusable) return@LaunchedEffect

        val focus = rowFocus.getValue(row.key)
        val index = if (foundIndex >= 0) {
            foundIndex
        } else {
            target.index.coerceIn(0, row.cards.lastIndex.coerceAtLeast(0))
        }
        focus.index = index
        focus.url = row.cards.getOrNull(index)?.url
        columnState.scrollToItem(rows.indexOf(row))
        if (row.cards.isNotEmpty()) {
            row.state.scrollToItem(index)
            snapshotFlow { row.state.layoutInfo.visibleItemsInfo.any { it.key == focus.url } }.first { it }
        }
        withFrameNanos { }
        if (!tabsFocused.value) {
            if (row.cards.isNotEmpty()) focus.poster.requestFocus() else focus.group.requestFocus()
        }
        restoreTarget = null
    }

    fun keepRowVisible(key: String) {
        val item = columnState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
        val layout = columnState.layoutInfo
        if (item != null && item.offset >= layout.viewportStartOffset &&
            item.offset + item.size <= layout.viewportEndOffset
        ) {
            return
        }
        scope.launch { columnState.scrollToItem(rows.indexOfFirst { it.key == key }) }
    }


    LazyColumn(
        state = columnState,
        modifier = Modifier.fillMaxSize().background(KinoColors.Background),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(rows, key = { it.key }) { row ->
            val focus = rowFocus.getValue(row.key)
            val rowIndex = focusableRows.indexOfFirst { it.key == row.key }
            val previous = focusableRows.getOrNull(rowIndex - 1)?.key
            val next = focusableRows.getOrNull(rowIndex + 1)?.key
            HomeRow(
                title = row.title,
                cards = row.cards,
                state = row.state,
                focus = focus,
                onRowFocused = {
                    restoreTarget = null
                    savedRow = row.key
                    savedIndex = focus.index
                    savedUrl = focus.url
                    keepRowVisible(row.key)
                },
                onFocused = { index, card ->
                    focus.index = index
                    focus.url = card.url
                    savedRow = row.key
                    savedIndex = index
                    savedUrl = card.url
                    if (index >= row.cards.size - 5 && row.error == null) row.loadMore()
                },
                onClick = { actions.openDetails(it) },
                loading = row.loading,
                error = row.error,
                onRetry = {
                    restoreTarget = HomeFocusTarget(row.key, focus.index, focus.url)
                    row.retry()
                },
                badge = row.badge,
            )
        }
    }
}
