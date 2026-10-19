package com.personal.kinotv.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

// Вкладки удаляются из композиции; сохраняем только выбранные жанры, без списков и картинок.
internal class SectionFiltersState(private val savedState: SavedStateHandle) : ViewModel() {
    fun selectedSlug(sectionSlug: String): String = savedState[sectionSlug] ?: sectionSlug

    fun select(sectionSlug: String, slug: String) {
        savedState[sectionSlug] = slug
    }
}
