package com.personal.kinotv.ui.home

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.personal.kinotv.data.Card
import com.personal.kinotv.ui.common.Dimens
import com.personal.kinotv.ui.common.ErrorBox
import com.personal.kinotv.ui.common.LoadingBox
import com.personal.kinotv.ui.common.PosterCard
import com.personal.kinotv.ui.theme.KinoColors

internal class HomeRowFocus {
    val group = FocusRequester()
    val poster = FocusRequester()
    var index by mutableIntStateOf(0)
    var url by mutableStateOf<String?>(null)
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal fun HomeRow(
    title: String,
    cards: List<Card>,
    state: LazyListState,
    focus: HomeRowFocus,
    onRowFocused: () -> Unit,
    onFocused: (Int, Card) -> Unit,
    onClick: (Card) -> Unit,
    loading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    badge: (Int) -> String? = { null },
) {
    val posterIndex = cards.indexOfFirst { it.url == focus.url }.takeIf { it >= 0 }
        ?: focus.index.coerceIn(0, cards.lastIndex.coerceAtLeast(0))

    Column(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.hasFocus) onRowFocused() }
            .focusGroup(),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = KinoColors.TextPrimary,
            modifier = Modifier.padding(start = Dimens.ScreenPadding, top = 4.dp),
        )
        when {
            cards.isEmpty() && loading -> LoadingBox()
            cards.isEmpty() && error != null -> Box(
                Modifier.focusRequester(focus.group).focusGroup(),
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
                        .focusRequester(focus.group)
                        // Вход в ряд — сразу на запомненный постер. Стандартный focusRestorer после ухода
                        // ряда за экран пытался вернуть фокус на выгруженный постер, и фокус «повисал»
                        .focusProperties { enter = { focus.poster } }
                        .focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.RowSpacing),
                    contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = 12.dp),
                ) {
                    itemsIndexed(cards, key = { _, card -> card.url }) { index, card ->
                        PosterCard(
                            card = card,
                            onClick = { onClick(card) },
                            modifier = if (index == posterIndex) Modifier.focusRequester(focus.poster) else Modifier,
                            width = 120.dp,
                            onFocused = { onFocused(index, card) },
                            badge = badge(index),
                        )
                    }
                    if (error != null) {
                        item(key = "retry") {
                            ErrorBox(error, onRetry, modifier = Modifier.width(320.dp))
                        }
                    }
                }
            }
        }
    }
}
