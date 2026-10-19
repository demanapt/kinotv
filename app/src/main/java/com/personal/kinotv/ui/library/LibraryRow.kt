package com.personal.kinotv.ui.library

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.personal.kinotv.data.Card
import com.personal.kinotv.ui.common.Dimens
import com.personal.kinotv.ui.common.PosterCard
import com.personal.kinotv.ui.theme.KinoColors

@Composable
internal fun LibraryRow(
    title: String,
    cards: List<Card>,
    emptyText: String,
    requestInitialFocus: Boolean,
    onInitialFocusHandled: () -> Unit,
    onClick: (Card) -> Unit,
) {
    val firstPosterFocus = remember { FocusRequester() }

    Column(Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = KinoColors.TextPrimary,
            modifier = Modifier.padding(start = Dimens.ScreenPadding, top = 4.dp),
        )
        if (cards.isEmpty()) {
            Text(
                emptyText,
                color = KinoColors.TextSecondary,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 24.dp),
            )
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth().focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.RowSpacing),
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = 12.dp),
            ) {
                itemsIndexed(cards, key = { _, card -> card.url }) { index, card ->
                    PosterCard(
                        card = card,
                        onClick = { onClick(card) },
                        modifier = if (index == 0) Modifier.focusRequester(firstPosterFocus) else Modifier,
                        width = 120.dp,
                    )
                    if (index == 0 && requestInitialFocus) {
                        LaunchedEffect(Unit) {
                            // Постер уже создан ленивым списком; ждём его размещения.
                            withFrameNanos { }
                            firstPosterFocus.requestFocus()
                            onInitialFocusHandled()
                        }
                    }
                }
            }
        }
    }
}
