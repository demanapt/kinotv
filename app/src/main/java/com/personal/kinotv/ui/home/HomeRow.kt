package com.personal.kinotv.ui.home

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.personal.kinotv.ui.common.LocalTabsFocused
import com.personal.kinotv.data.Card
import com.personal.kinotv.ui.common.Dimens
import com.personal.kinotv.ui.common.ErrorBox
import com.personal.kinotv.ui.common.LoadingBox
import com.personal.kinotv.ui.common.PosterCard
import com.personal.kinotv.ui.theme.KinoColors
import kotlinx.coroutines.flow.first

@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal fun HomeRow(
    title: String,
    cards: List<Card>,
    state: LazyListState,
    target: HomeFocusTarget?,
    onFocusHandled: () -> Unit,
    onEmpty: () -> Unit,
    onFocused: (Int, Card) -> Unit,
    onClick: (Card) -> Unit,
    loading: Boolean = false,
    error: String? = null,
    waitingForTarget: Boolean = false,
    onRetry: () -> Unit = {},
    badge: (Int) -> String? = { null },
) {
    val posterFocus = remember { FocusRequester() }
    val tabsFocused = LocalTabsFocused.current
    val errorFocus = remember { FocusRequester() }
    val targetIndex = target?.let {
        cards.indexOfFirst { card -> card.url == it.url }.takeIf { index -> index >= 0 }
            ?: it.index.coerceIn(0, (cards.size - 1).coerceAtLeast(0))
    }

    LaunchedEffect(target, cards.size, loading, error, waitingForTarget) {
        if (target == null || waitingForTarget) return@LaunchedEffect
        if (cards.isNotEmpty()) {
            val index = targetIndex ?: return@LaunchedEffect
            val url = cards[index].url
            if (state.layoutInfo.visibleItemsInfo.none { it.key == url }) state.scrollToItem(index)
            snapshotFlow { state.layoutInfo.visibleItemsInfo.any { it.key == url } }.first { it }
            withFrameNanos { }
            if (!tabsFocused.value) posterFocus.requestFocus()
            onFocusHandled()
        } else if (error != null) {
            withFrameNanos { }
            errorFocus.requestFocus()
            onFocusHandled()
        } else if (!loading) {
            onEmpty()
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = KinoColors.TextPrimary,
            modifier = Modifier.padding(start = Dimens.ScreenPadding, top = 4.dp),
        )
        when {
            cards.isEmpty() && loading -> LoadingBox()
            cards.isEmpty() && error != null -> Box(
                Modifier.focusRequester(errorFocus).focusGroup(),
            ) {
                ErrorBox(error, onRetry)
            }
            cards.isEmpty() -> Text(
                "Пока ничего нет",
                color = KinoColors.TextSecondary,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 24.dp),
            )
            else -> {
                LazyRow(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (target != null) {
                                // При входе восстанавливаем сохранённый постер, а не историю группы.
                                Modifier.focusProperties { enter = { FocusRequester.Default } }
                            } else {
                                Modifier
                            },
                        )
                        .focusRestorer()
                        .focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.RowSpacing),
                    contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = 12.dp),
                ) {
                    itemsIndexed(cards, key = { _, card -> card.url }) { index, card ->
                        PosterCard(
                            card = card,
                            onClick = { onClick(card) },
                            modifier = if (index == targetIndex) Modifier.focusRequester(posterFocus) else Modifier,
                            width = 120.dp,
                            onFocused = { onFocused(index, card) },
                            badge = badge(index),
                        )
                    }
                }
                if (error != null) {
                    ErrorBox(error, onRetry)
                }
            }
        }
    }
}
