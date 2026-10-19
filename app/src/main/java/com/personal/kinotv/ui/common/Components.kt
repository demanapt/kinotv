package com.personal.kinotv.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.personal.kinotv.data.Card as TitleCard
import com.personal.kinotv.ui.theme.KinoColors

/** Размеры, общие для всех экранов. */
object Dimens {
    val PosterWidth = 150.dp
    val ScreenPadding = 48.dp
    val RowSpacing = 16.dp
}

/**
 * Постер тайтла с фокусом в стиле Netflix: при фокусе увеличивается и получает белую рамку.
 * Внизу — затемнение с названием и годом.
 */
@Composable
fun PosterCard(
    card: TitleCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = Dimens.PosterWidth,
    onFocused: (() -> Unit)? = null,
    badge: String? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    Card(
        onClick = onClick,
        modifier = modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .then(if (onFocused != null) Modifier.onFocusGained(onFocused) else Modifier),
        shape = CardDefaults.shape(shape),
        scale = CardDefaults.scale(focusedScale = 1.08f),
        border = CardDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, KinoColors.FocusBorder), shape = shape),
        ),
        colors = CardDefaults.colors(containerColor = KinoColors.Surface),
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = card.poster,
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000))))
                    .padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
            ) {
                Text(
                    card.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = KinoColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                card.year?.let { Text(it, fontSize = 11.sp, color = KinoColors.TextSecondary) }
            }
            if (badge != null) {
                Text(
                    badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(KinoColors.Accent, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/** Индикатор загрузки на месте контента. */
@Composable
fun LoadingBox(modifier: Modifier = Modifier, text: String = "Загрузка…") {
    Box(modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Text(text, color = KinoColors.TextSecondary, style = MaterialTheme.typography.titleMedium)
    }
}

/** Ошибка с кнопкой «Повторить» (кнопка фокусируемая — работает с пульта). */
@Composable
fun ErrorBox(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Не удалось загрузить: $message", color = KinoColors.TextSecondary)
        Button(onClick = onRetry) { Text("Повторить") }
    }
}

/** Рейтинг «★ 4.2». */
@Composable
fun RatingText(rating: Float?, modifier: Modifier = Modifier) {
    if (rating == null) return
    Row(modifier) {
        Text("★ ", color = KinoColors.Rating, style = MaterialTheme.typography.bodyMedium)
        Text(String.format("%.1f", rating), color = KinoColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}
