package com.personal.kinotv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.personal.kinotv.KinoApp
import com.personal.kinotv.data.Card
import com.personal.kinotv.data.EpisodeUpdate
import com.personal.kinotv.data.Sections
import com.personal.kinotv.ui.common.LocalAppActions
import com.personal.kinotv.ui.common.rememberPagedList
import com.personal.kinotv.ui.theme.KinoColors
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private const val BANNER = "banner"
private const val HISTORY = "history"
private const val EPISODES = "episodes"

internal data class HomeFocusTarget(val row: String, val index: Int = 0, val url: String? = null)

internal data class HomeBannerTitle(val card: Card, val episode: EpisodeUpdate? = null) {
    val poster: String get() = episode?.bigPoster?.takeIf { it.isNotBlank() } ?: card.poster
}

@Composable
fun HomeScreen() {
    val actions = LocalAppActions.current
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

    // Состояния живут дольше ленивых элементов: уход ряда за экран их не сбрасывает.
    val categories = Sections.homeRows.map { section ->
        rememberPagedList(section.slug) { page -> KinoApp.repository.category(section.slug, page) }
    }
    val categoryStates = Sections.homeRows.map { rememberLazyListState() }
    val historyState = rememberLazyListState()
    val episodesState = rememberLazyListState()
    val columnState = rememberLazyListState()
    val playFocus = remember { FocusRequester() }
    val detailsFocus = remember { FocusRequester() }

    var savedRow by rememberSaveable { mutableStateOf(BANNER) }
    var savedIndex by rememberSaveable { mutableIntStateOf(0) }
    var savedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingFocus by remember {
        mutableStateOf<HomeFocusTarget?>(HomeFocusTarget(savedRow, savedIndex, savedUrl))
    }
    var focusedTitle by remember { mutableStateOf<HomeBannerTitle?>(null) }
    var bannerHasFocus by remember { mutableStateOf(savedRow == BANNER) }
    val fallback = episodes.firstOrNull()?.let { HomeBannerTitle(it.card, it) }
        ?: if (!episodesLoading) {
            (history.firstOrNull() ?: categories.firstNotNullOfOrNull { it.items.firstOrNull() })
                ?.let { HomeBannerTitle(it) }
        } else {
            null
        }
    val requestedTitle = if (bannerHasFocus) fallback else focusedTitle ?: fallback
    var banner by remember { mutableStateOf<HomeBannerTitle?>(null) }
    LaunchedEffect(requestedTitle) {
        if (banner != null) delay(300)
        banner = requestedTitle
    }

    fun saveFocus(target: HomeFocusTarget) {
        savedRow = target.row
        savedIndex = target.index
        savedUrl = target.url
    }

    val rowKeys = buildList {
        if (history.isNotEmpty()) add(HISTORY)
        add(EPISODES)
        Sections.homeRows.forEach { add(it.slug) }
    }
    val navigableRows = buildList {
        if (history.isNotEmpty()) add(HISTORY)
        if (episodes.isNotEmpty() || episodesError != null) add(EPISODES)
        Sections.homeRows.forEachIndexed { index, section ->
            if (categories[index].items.isNotEmpty() || categories[index].error != null) add(section.slug)
        }
    }
    val firstFocusableRow = navigableRows.firstOrNull()
    val rowStates = buildMap {
        put(HISTORY, historyState)
        put(EPISODES, episodesState)
        Sections.homeRows.forEachIndexed { index, section -> put(section.slug, categoryStates[index]) }
    }

    fun moveToRow(row: String, index: Int, down: Boolean): Boolean {
        val position = navigableRows.indexOf(row)
        if (position < 0) return false
        val nextRow = navigableRows.getOrNull(position + if (down) 1 else -1) ?: return false
        val currentState = rowStates.getValue(row)
        val nextState = rowStates.getValue(nextRow)
        val currentItem = currentState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        val center = currentItem?.let { it.offset + it.size / 2 }
        val nearest = if (center != null) {
            nextState.layoutInfo.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
        } else {
            null
        }
        val nextIndex = nearest ?: (nextState.firstVisibleItemIndex + index - currentState.firstVisibleItemIndex)
        pendingFocus = HomeFocusTarget(nextRow, nextIndex.coerceAtLeast(0))
        return true
    }

    // После возврата догружаем страницы до сохранённого постера.
    val restoringCategory = Sections.homeRows.indexOfFirst { it.slug == pendingFocus?.row }
    val restoringList = categories.getOrNull(restoringCategory)
    LaunchedEffect(
        pendingFocus,
        restoringList?.items?.size,
        restoringList?.isLoading,
        restoringList?.error,
        restoringList?.endReached,
    ) {
        val target = pendingFocus ?: return@LaunchedEffect
        val list = restoringList ?: return@LaunchedEffect
        if (target.index >= list.items.size && list.items.none { it.url == target.url } &&
            !list.isLoading && list.error == null && !list.endReached
        ) {
            list.loadMore()
        }
    }

    LaunchedEffect(pendingFocus, rowKeys) {
        val target = pendingFocus ?: return@LaunchedEffect
        if (target.row == BANNER) return@LaunchedEffect
        val index = rowKeys.indexOf(target.row)
        if (index < 0) {
            pendingFocus = HomeFocusTarget(BANNER)
        } else if (columnState.layoutInfo.visibleItemsInfo.none { it.key == target.row }) {
            columnState.scrollToItem(index)
        }
    }

    LaunchedEffect(pendingFocus, banner != null, firstFocusableRow, episodesLoading) {
        val target = pendingFocus ?: return@LaunchedEffect
        if (target.row != BANNER) return@LaunchedEffect
        if (banner != null) {
            withFrameNanos { }
            if (target.index == 1) detailsFocus.requestFocus() else playFocus.requestFocus()
            pendingFocus = null
        } else if (!episodesLoading && firstFocusableRow != null) {
            pendingFocus = HomeFocusTarget(firstFocusableRow)
        }
    }

    Column(Modifier.fillMaxSize().background(KinoColors.Background)) {
        HomeBanner(
            title = banner,
            loading = episodesLoading,
            playFocus = playFocus,
            detailsFocus = detailsFocus,
            onButtonFocused = { index ->
                pendingFocus = null
                bannerHasFocus = true
                saveFocus(HomeFocusTarget(BANNER, index))
            },
            onDown = { firstFocusableRow?.let { pendingFocus = HomeFocusTarget(it) } },
            onPlay = { banner?.let { actions.play(it.card) } },
            onDetails = { banner?.let { actions.openDetails(it.card) } },
        )
        LazyColumn(
            state = columnState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (history.isNotEmpty()) {
                item(key = HISTORY) {
                    HomeRow(
                        title = "Продолжить просмотр",
                        cards = history,
                        state = historyState,
                        target = pendingFocus?.takeIf { it.row == HISTORY },
                        upFocus = playFocus.takeIf { banner != null },
                        onFocusHandled = { pendingFocus = null },
                        onEmpty = { pendingFocus = HomeFocusTarget(BANNER) },
                        onVerticalMove = { index, down -> moveToRow(HISTORY, index, down) },
                        onFocused = { index, card ->
                            pendingFocus = null
                            bannerHasFocus = false
                            focusedTitle = HomeBannerTitle(card)
                            saveFocus(HomeFocusTarget(HISTORY, index, card.url))
                        },
                        onClick = { actions.openDetails(it) },
                    )
                }
            }
            item(key = EPISODES) {
                HomeRow(
                    title = "Новые серии",
                    // Маленькие картинки этого блока мутные — берём большой постер
                    cards = episodes.map { it.card.copy(poster = it.bigPoster) },
                    state = episodesState,
                    target = pendingFocus?.takeIf { it.row == EPISODES },
                    upFocus = playFocus.takeIf { history.isEmpty() && banner != null },
                    loading = episodesLoading,
                    error = episodesError,
                    onRetry = {
                        pendingFocus = HomeFocusTarget(EPISODES)
                        episodesLoading = true
                        episodesError = null
                        episodesAttempt++
                    },
                    badge = { index -> episodes[index].episode },
                    onFocusHandled = { pendingFocus = null },
                    onEmpty = { pendingFocus = HomeFocusTarget(BANNER) },
                    onVerticalMove = { index, down -> moveToRow(EPISODES, index, down) },
                    onFocused = { index, card ->
                        pendingFocus = null
                        bannerHasFocus = false
                        focusedTitle = HomeBannerTitle(card, episodes[index])
                        saveFocus(HomeFocusTarget(EPISODES, index, card.url))
                    },
                    onClick = { actions.openDetails(it) },
                )
            }
            Sections.homeRows.forEachIndexed { sectionIndex, section ->
                item(key = section.slug) {
                    val list = categories[sectionIndex]
                    HomeRow(
                        title = section.title,
                        cards = list.items,
                        state = categoryStates[sectionIndex],
                        target = pendingFocus?.takeIf { it.row == section.slug },
                        upFocus = playFocus.takeIf { firstFocusableRow == section.slug && banner != null },
                        loading = list.isLoading,
                        error = list.error,
                        waitingForTarget = pendingFocus?.let { target ->
                            target.row == section.slug && target.index >= list.items.size &&
                                list.items.none { it.url == target.url } && !list.endReached && list.error == null
                        } == true,
                        onRetry = {
                            pendingFocus = HomeFocusTarget(section.slug, savedIndex, savedUrl)
                            list.retry()
                        },
                        onFocusHandled = { pendingFocus = null },
                        onEmpty = { pendingFocus = HomeFocusTarget(BANNER) },
                        onVerticalMove = { index, down -> moveToRow(section.slug, index, down) },
                        onFocused = { index, card ->
                            pendingFocus = null
                            bannerHasFocus = false
                            focusedTitle = HomeBannerTitle(card)
                            saveFocus(HomeFocusTarget(section.slug, index, card.url))
                            if (index >= list.items.size - 5 && list.error == null) list.loadMore()
                        },
                        onClick = { actions.openDetails(it) },
                    )
                }
            }
        }
    }
}
