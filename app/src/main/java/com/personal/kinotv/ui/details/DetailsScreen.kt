package com.personal.kinotv.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.personal.kinotv.KinoApp
import com.personal.kinotv.data.Details
import com.personal.kinotv.ui.common.ErrorBox
import com.personal.kinotv.ui.common.LoadingBox
import com.personal.kinotv.ui.theme.KinoColors
import kotlinx.coroutines.CancellationException

@Composable
fun DetailsScreen(url: String) {
    key(url) {
        DetailsPage(url)
    }
}

@Composable
private fun DetailsPage(url: String) {
    // Сохранённая на приставке копия показывается сразу, свежая с сайта подменяет её, когда загрузится
    var details by remember { mutableStateOf<Details?>(KinoApp.repository.cachedDetails(url)) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    val retryFocus = remember { FocusRequester() }

    LaunchedEffect(url, attempt) {
        try {
            details = KinoApp.repository.details(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Если уже показана сохранённая копия — ошибку сети не показываем
            if (details == null) error = e.message?.takeIf { it.isNotBlank() } ?: "Ошибка загрузки"
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(KinoColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        val loaded = details
        val message = error
        when {
            loaded != null -> DetailsContent(loaded)
            message != null -> {
                Box(Modifier.focusRequester(retryFocus).focusGroup()) {
                    ErrorBox(
                        message = message,
                        onRetry = {
                            error = null
                            attempt++
                        },
                    )
                }
                LaunchedEffect(message) {
                    withFrameNanos { }
                    retryFocus.requestFocus()
                }
            }
            else -> LoadingBox()
        }
    }
}
