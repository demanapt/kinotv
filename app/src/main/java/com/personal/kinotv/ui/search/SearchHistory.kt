package com.personal.kinotv.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.personal.kinotv.KinoApp
import com.personal.kinotv.ui.common.Dimens
import com.personal.kinotv.ui.common.LocalAppActions
import com.personal.kinotv.ui.common.PosterCard
import com.personal.kinotv.ui.theme.KinoColors

@Composable
internal fun SearchHistory(modifier: Modifier = Modifier) {
    val history by KinoApp.library.history.collectAsState()
    val actions = LocalAppActions.current

    Column(modifier) {
        SearchHint("Начни вводить название")
        if (history.isNotEmpty()) {
            Text(
                text = "Продолжить просмотр",
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
                color = KinoColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = Dimens.ScreenPadding,
                    vertical = 24.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(Dimens.RowSpacing),
            ) {
                items(history, key = { it.url }) { card ->
                    PosterCard(card = card, onClick = { actions.openDetails(card) })
                }
            }
        }
    }
}
