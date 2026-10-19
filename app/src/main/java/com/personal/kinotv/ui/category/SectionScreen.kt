package com.personal.kinotv.ui.category

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.personal.kinotv.data.Section

/** Заглушка. Реализация — по docs/specs/category.md */
@Composable
fun SectionScreen(section: Section) {
    Text("Раздел", modifier = Modifier.fillMaxSize().padding(48.dp))
}
