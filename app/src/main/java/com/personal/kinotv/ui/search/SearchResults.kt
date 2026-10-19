package com.personal.kinotv.ui.search

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.personal.kinotv.KinoApp
import com.personal.kinotv.ui.common.Dimens
import com.personal.kinotv.ui.common.ErrorBox
import com.personal.kinotv.ui.common.LoadingBox
import com.personal.kinotv.ui.common.LocalAppActions
import com.personal.kinotv.ui.common.PosterCard
import com.personal.kinotv.ui.common.rememberPagedList

private const val COLUMNS = 6
private const val PREFETCH_ITEMS = 12

@Composable
internal fun SearchResults(
    query: String,
    inputFocus: FocusRequester,
    focusRequested: Boolean,
    onFocusHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = LocalAppActions.current
    val list = rememberPagedList(query) { page -> KinoApp.repository.search(query, page) }
    val contentFocus = remember { FocusRequester() }
    val error = list.error

    LaunchedEffect(focusRequested, list.isLoading, list.items.isNotEmpty(), error) {
        if (focusRequested && !list.isLoading) {
            if (list.items.isNotEmpty() || error != null) {
                withFrameNanos { }
                contentFocus.requestFocus()
            }
            onFocusHandled()
        }
    }

    val retry = {
        inputFocus.requestFocus()
        list.retry()
    }

    Box(modifier.fillMaxSize().focusRequester(contentFocus).focusGroup()) {
        when {
            list.items.isEmpty() -> when {
                error != null -> ErrorBox(error, retry)
                list.isLoading -> LoadingBox()
                else -> SearchHint("По запросу «$query» ничего не нашлось")
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(COLUMNS),
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
                        modifier = Modifier.fillMaxWidth(),
                        onFocused = {
                            if (index >= list.items.size - PREFETCH_ITEMS && list.error == null) {
                                list.loadMore()
                            }
                        },
                    )
                }
                if (list.isLoading) {
                    item(key = "search-loading", span = { GridItemSpan(maxLineSpan) }) {
                        LoadingBox()
                    }
                }
                if (error != null) {
                    item(key = "search-error", span = { GridItemSpan(maxLineSpan) }) {
                        ErrorBox(error, retry)
                    }
                }
            }
        }
    }
}
