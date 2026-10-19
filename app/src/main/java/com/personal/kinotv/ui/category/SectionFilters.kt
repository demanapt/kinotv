package com.personal.kinotv.ui.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Text
import com.personal.kinotv.data.Section
import com.personal.kinotv.ui.common.Dimens
import com.personal.kinotv.ui.theme.KinoColors

@Composable
internal fun SectionFilters(
    filters: List<Section>,
    selectedSlug: String,
    state: LazyListState,
    selectedFocus: FocusRequester,
    contentFocus: FocusRequester,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        state = state,
        modifier = Modifier.fillMaxWidth().focusGroup(),
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(Dimens.RowSpacing),
    ) {
        items(filters, key = { it.slug }) { filter ->
            val selected = filter.slug == selectedSlug
            FilterChip(
                selected = selected,
                onClick = { onSelect(filter.slug) },
                modifier = Modifier
                    .then(if (selected) Modifier.focusRequester(selectedFocus) else Modifier)
                    .focusProperties { down = contentFocus },
                colors = FilterChipDefaults.colors(
                    containerColor = KinoColors.Surface,
                    contentColor = KinoColors.TextPrimary,
                    focusedContainerColor = KinoColors.SurfaceVariant,
                    focusedContentColor = KinoColors.TextPrimary,
                    selectedContainerColor = KinoColors.Accent,
                    selectedContentColor = KinoColors.TextPrimary,
                    focusedSelectedContainerColor = KinoColors.Accent,
                    focusedSelectedContentColor = KinoColors.TextPrimary,
                ),
                border = FilterChipDefaults.border(
                    focusedBorder = Border(BorderStroke(2.dp, KinoColors.FocusBorder)),
                    focusedSelectedBorder = Border(BorderStroke(2.dp, KinoColors.FocusBorder)),
                ),
            ) {
                Text(filter.title)
            }
        }
    }
}
