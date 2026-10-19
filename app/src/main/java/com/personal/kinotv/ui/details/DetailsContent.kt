package com.personal.kinotv.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.personal.kinotv.KinoApp
import com.personal.kinotv.data.Details
import com.personal.kinotv.ui.common.Dimens
import com.personal.kinotv.ui.common.LocalAppActions
import com.personal.kinotv.ui.common.PosterCard
import com.personal.kinotv.ui.common.RatingText
import com.personal.kinotv.ui.theme.KinoColors

@Composable
internal fun DetailsContent(details: Details) {
    val actions = LocalAppActions.current
    val favorites by KinoApp.library.favorites.collectAsState()
    val isFavorite = favorites.any { it.url == details.url }
    val card = remember(details) { details.toCard() }
    val mainFocus = remember { FocusRequester() }

    LaunchedEffect(details.url) {
        withFrameNanos { }
        mainFocus.requestFocus()
    }

    Box(Modifier.fillMaxSize()) {
        DetailsBackdrop(details.poster)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 32.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DetailsInformation(
                details = details,
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .padding(start = Dimens.ScreenPadding, end = 16.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenPadding, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(Dimens.RowSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (details.players.isEmpty()) {
                    Button(onClick = {}, enabled = false) { Text("Плеер недоступен") }
                } else {
                    details.players.forEachIndexed { index, player ->
                        Button(
                            onClick = { actions.play(card, player) },
                            modifier = if (index == 0) Modifier.focusRequester(mainFocus) else Modifier,
                        ) {
                            val label = player.label.takeIf { it.isNotBlank() } ?: "Плеер ${index + 1}"
                            Text(if (index == 0) "▶ Смотреть" else "▶ $label")
                        }
                    }
                }
                Button(
                    onClick = { KinoApp.library.toggleFavorite(card) },
                    modifier = if (details.players.isEmpty()) Modifier.focusRequester(mainFocus) else Modifier,
                ) {
                    Text(if (isFavorite) "★ В избранном" else "★ В избранное")
                }
            }
            if (details.related.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Похожие",
                        style = MaterialTheme.typography.titleMedium,
                        color = KinoColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.RowSpacing),
                        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = 12.dp),
                    ) {
                        items(details.related, key = { it.url }) { related ->
                            PosterCard(
                                card = related,
                                onClick = { actions.openDetails(related) },
                                width = 120.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsBackdrop(poster: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        Box(Modifier.fillMaxWidth(0.5f).fillMaxHeight()) {
            AsyncImage(
                model = poster,
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
    }
}

@Composable
private fun DetailsInformation(details: Details, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            details.title,
            style = MaterialTheme.typography.headlineMedium,
            color = KinoColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        details.originalTitle?.takeIf { it.isNotBlank() }?.let { title ->
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = KinoColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DetailsMetadata(details)
        val genres = details.genres.filter { it.isNotBlank() }
        if (genres.isNotEmpty()) {
            Text(
                genres.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = KinoColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        details.description?.takeIf { it.isNotBlank() }?.let { description ->
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = KinoColors.TextSecondary,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val credits = buildList {
            val actors = details.actors.filter { it.isNotBlank() }.take(5)
            val directors = details.directors.filter { it.isNotBlank() }
            if (actors.isNotEmpty()) add("В ролях: ${actors.joinToString(", ")}")
            if (directors.isNotEmpty()) add("Режиссёр: ${directors.joinToString(", ")}")
        }
        if (credits.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                credits.forEach { credit ->
                    Text(
                        credit,
                        style = MaterialTheme.typography.bodySmall,
                        color = KinoColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsMetadata(details: Details) {
    val beforeRating = listOfNotNull(
        details.year?.takeIf { it.isNotBlank() },
        details.country?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
    val afterRating = buildString {
        if (details.rating != null && details.ratingVotes != null) append(" (${details.ratingVotes})")
        if (details.isSeries) {
            if (beforeRating.isNotEmpty() || details.rating != null) append(" · ")
            append("Сериал")
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (beforeRating.isNotEmpty()) {
            Text(
                beforeRating,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.bodyMedium,
                color = KinoColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (details.rating != null) {
            if (beforeRating.isNotEmpty()) Text(" · ", color = KinoColors.TextSecondary)
            RatingText(details.rating)
        }
        if (afterRating.isNotEmpty()) {
            Text(
                afterRating,
                style = MaterialTheme.typography.bodyMedium,
                color = KinoColors.TextSecondary,
                maxLines = 1,
            )
        }
    }
}
