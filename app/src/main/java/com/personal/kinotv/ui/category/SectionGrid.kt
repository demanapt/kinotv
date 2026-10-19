package com.personal.kinotv.ui.category

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.personal.kinotv.KinoApp
import com.personal.kinotv.ui.common.Dimens
import com.personal.kinotv.ui.common.ErrorBox
import com.personal.kinotv.ui.common.LoadingBox
import com.personal.kinotv.ui.common.LocalAppActions
import com.personal.kinotv.ui.common.PosterCard
import com.personal.kinotv.ui.common.rememberPagedList
import com.personal.kinotv.ui.theme.KinoColors

private const val COLUMNS = 6
private const val PREFETCH_ITEMS = 12

@Composable
internal fun SectionGrid(
    slug: String,
    contentFocus: FocusRequester,
    onUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = LocalAppActions.current
    val list = rememberPagedList(slug) { page -> KinoApp.repository.category(slug, page) }
    val gridState = rememberLazyGridState()
    val restoreIndex = remember { gridState.firstVisibleItemIndex }
    var restoring by remember { mutableStateOf(restoreIndex > 0) }
    val error = list.error

    // Не размещаем короткую первую страницу до восстановления: она обнулит прокрутку сетки.
    LaunchedEffect(list.items.size, list.isLoading, error, list.endReached) {
        if (restoring) {
            if (list.items.size > restoreIndex + COLUMNS * 3 || list.endReached) {
                restoring = false
            } else if (!list.isLoading && error == null) {
                list.loadMore()
            }
        }
    }

    val retry = {
        onUp()
        list.retry()
    }

    Box(modifier.fillMaxSize().focusRequester(contentFocus).focusGroup()) {
        when {
            restoring || list.items.isEmpty() -> {
                if (error != null) {
                    Box(Modifier.returnToFilters(onUp)) {
                        ErrorBox(error, retry)
                    }
                } else if (list.isLoading || restoring) {
                    LoadingBox()
                } else {
                    Text(
                        "В этом разделе пока нет тайтлов",
                        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 24.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = KinoColors.TextSecondary,
                    )
                }
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(COLUMNS),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Dimens.ScreenPadding,
                    end = Dimens.ScreenPadding,
                    top = Dimens.RowSpacing,
                    bottom = Dimens.ScreenPadding,
                ),
                horizontalArrangement = Arrangement.spacedBy(Dimens.RowSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.RowSpacing),
            ) {
                itemsIndexed(list.items, key = { _, card -> card.url }) { index, card ->
                    PosterCard(
                        card = card,
                        onClick = { actions.openDetails(card) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (index < COLUMNS) Modifier.returnToFilters(onUp) else Modifier),
                        onFocused = {
                            if (index >= list.items.size - PREFETCH_ITEMS && list.error == null) {
                                list.loadMore()
                            }
                        },
                    )
                }
                if (list.isLoading) {
                    item(key = "category-loading", span = { GridItemSpan(maxLineSpan) }) {
                        LoadingBox()
                    }
                }
                if (error != null) {
                    item(key = "category-error", span = { GridItemSpan(maxLineSpan) }) {
                        ErrorBox(error, retry)
                    }
                }
            }
        }
    }
}

private fun Modifier.returnToFilters(onUp: () -> Unit): Modifier = onPreviewKeyEvent {
    if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionUp) {
        onUp()
        true
    } else {
        false
    }
}
