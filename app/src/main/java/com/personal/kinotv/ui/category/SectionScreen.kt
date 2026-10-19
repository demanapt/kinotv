package com.personal.kinotv.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.kinotv.data.Section
import com.personal.kinotv.data.Sections
import com.personal.kinotv.ui.theme.KinoColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SectionScreen(section: Section) {
    val filtersState: SectionFiltersState = viewModel()
    key(section.slug) {
        SectionContent(section, filtersState)
    }
}

@Composable
private fun SectionContent(section: Section, filtersState: SectionFiltersState) {
    var slug by rememberSaveable(section.slug) {
        mutableStateOf(filtersState.selectedSlug(section.slug))
    }
    val filters = remember(section) { listOf(section.copy(title = "Все")) + Sections.genres }
    val selectedIndex = filters.indexOfFirst { it.slug == slug }.coerceAtLeast(0)
    val rowState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val selectedFocus = remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    suspend fun focusSelectedFilter() {
        if (rowState.layoutInfo.visibleItemsInfo.none { it.key == slug }) {
            rowState.scrollToItem(selectedIndex)
        }
        snapshotFlow { rowState.layoutInfo.visibleItemsInfo.any { it.key == slug } }.first { it }
        withFrameNanos { }
        selectedFocus.requestFocus()
    }

    LaunchedEffect(section.slug) {
        focusSelectedFilter()
    }

    Column(Modifier.fillMaxSize().background(KinoColors.Background)) {
        SectionFilters(
            filters = filters,
            selectedSlug = slug,
            state = rowState,
            selectedFocus = selectedFocus,
            contentFocus = contentFocus,
            onSelect = { selected ->
                filtersState.select(section.slug, selected)
                slug = selected
            },
        )
        key(slug) {
            SectionGrid(
                slug = slug,
                contentFocus = contentFocus,
                onUp = { scope.launch { focusSelectedFilter() } },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
