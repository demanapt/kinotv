package com.personal.kinotv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.personal.kinotv.ui.common.Dimens
import com.personal.kinotv.ui.common.LoadingBox
import com.personal.kinotv.ui.common.RatingText
import com.personal.kinotv.ui.common.onFocusGained
import com.personal.kinotv.ui.theme.KinoColors

@Composable
internal fun HomeBanner(
    title: HomeBannerTitle?,
    loading: Boolean,
    playFocus: FocusRequester,
    detailsFocus: FocusRequester,
    onButtonFocused: (Int) -> Unit,
    onDown: () -> Unit,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(240.dp)) {
        if (title == null) {
            if (loading) {
                LoadingBox(Modifier.align(Alignment.Center))
            } else {
                Text(
                    "Выберите тайтл в рядах ниже",
                    color = KinoColors.TextSecondary,
                    modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = Dimens.ScreenPadding),
                )
            }
            return@Box
        }
        Box(Modifier.align(Alignment.CenterEnd).fillMaxWidth(0.45f).height(240.dp)) {
            AsyncImage(
                model = title.poster,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(listOf(KinoColors.Background, Color.Transparent)),
                ),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, KinoColors.Background)),
                ),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .padding(start = Dimens.ScreenPadding, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                title.card.title,
                style = MaterialTheme.typography.headlineMedium,
                color = KinoColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            BannerMetadata(title)
            title.card.excerpt?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = KinoColors.TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.padding(top = 4.dp).onPreviewKeyEvent {
                    if (it.key == Key.DirectionDown && it.type == KeyEventType.KeyDown) {
                        onDown()
                        true
                    } else {
                        false
                    }
                },
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.focusRequester(playFocus).onFocusGained { onButtonFocused(0) },
                ) { Text("▶ Смотреть") }
                Button(
                    onClick = onDetails,
                    modifier = Modifier.focusRequester(detailsFocus).onFocusGained { onButtonFocused(1) },
                ) { Text("ℹ Подробнее") }
            }
        }
    }
}

@Composable
private fun BannerMetadata(title: HomeBannerTitle) {
    val year = title.card.year?.takeIf { it.isNotBlank() }
    val afterRating = listOfNotNull(
        title.episode?.season?.takeIf { it.isNotBlank() },
        title.episode?.episode?.takeIf { it.isNotBlank() },
        title.episode?.translation?.takeIf { it.isNotBlank() },
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (year != null) {
            Text(year, color = KinoColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        if (title.card.rating != null) {
            if (year != null) Text(" · ", color = KinoColors.TextSecondary)
            RatingText(title.card.rating)
        }
        if (afterRating.isNotEmpty()) {
            val separator = if (year != null || title.card.rating != null) " · " else ""
            Text(
                separator + afterRating.joinToString(" · "),
                color = KinoColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
