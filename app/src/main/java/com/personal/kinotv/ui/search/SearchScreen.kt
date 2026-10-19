package com.personal.kinotv.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.personal.kinotv.ui.common.Dimens
import com.personal.kinotv.ui.common.LoadingBox
import com.personal.kinotv.ui.theme.KinoColors
import kotlinx.coroutines.delay

@Composable
fun SearchScreen() {
    val queryState: SearchQueryState = viewModel()
    var text by rememberSaveable { mutableStateOf(queryState.query) }
    var query by remember { mutableStateOf("") }
    var focusResultsFor by remember { mutableStateOf<String?>(null) }
    val inputFocus = remember { FocusRequester() }
    val normalized = text.trim()

    LaunchedEffect(Unit) {
        withFrameNanos { }
        inputFocus.requestFocus()
    }

    LaunchedEffect(text) {
        if (normalized.length < 2) {
            query = ""
        } else {
            delay(600)
            query = normalized
        }
    }

    Column(Modifier.fillMaxSize()) {
        SearchInput(
            text = text,
            onTextChange = {
                text = it
                queryState.query = it
                query = ""
                focusResultsFor = null
            },
            inputFocus = inputFocus,
            onSearch = {
                inputFocus.requestFocus()
                focusResultsFor = text.trim().takeIf { it.length >= 2 }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding, vertical = 16.dp),
        )

        when {
            normalized.isEmpty() -> SearchHistory(Modifier.weight(1f))
            normalized.length < 2 -> SearchHint("Введите не менее 2 символов")
            query != normalized -> LoadingBox()
            else -> key(query) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Найдено по «$query»:",
                        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
                        color = KinoColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                    )
                    SearchResults(
                        query = query,
                        inputFocus = inputFocus,
                        focusRequested = focusResultsFor == query,
                        onFocusHandled = { focusResultsFor = null },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun SearchHint(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 24.dp),
        color = KinoColors.TextSecondary,
        style = MaterialTheme.typography.titleMedium,
    )
}
