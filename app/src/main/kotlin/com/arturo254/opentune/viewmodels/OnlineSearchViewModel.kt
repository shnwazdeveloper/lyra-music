/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.arturo254.opentune.innertube.models.filterExplicit
import com.arturo254.opentune.innertube.models.filterVideo
import com.arturo254.opentune.innertube.pages.SearchSummaryPage
import com.arturo254.opentune.constants.HideExplicitKey
import com.arturo254.opentune.constants.HideVideoKey
import com.arturo254.opentune.models.ItemsPage
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.get
import com.arturo254.opentune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.get<String>("query")!!
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    init {
        viewModelScope.launch {
            filter.collect { filter ->
                if (filter == null) {
                    if (summaryPage == null) {
                        YouTube
                            .searchSummary(query)
                            .onSuccess {
                                summaryPage = it.filterExplicit(context.dataStore.get(HideExplicitKey, false)).filterVideo(context.dataStore.get(HideVideoKey, false))
                            }.onFailure {
                                reportException(it)
                            }
                    }
                    if (viewStateMap[FILTER_SONG.value] == null) {
                        loadSearchPage(FILTER_SONG)
                    }
                } else {
                    if (viewStateMap[filter.value] == null) {
                        loadSearchPage(filter)
                    }
                }
            }
        }
    }

    fun loadMore() {
        val filter = filter.value ?: FILTER_SONG
        viewModelScope.launch {
            val viewState = viewStateMap[filter.value] ?: return@launch
            val continuation = viewState.continuation
            if (continuation != null) {
                val searchResult =
                    YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                viewStateMap[filter.value] = ItemsPage(
                    (viewState.items + searchResult.items)
                        .distinctBy { it.id }
                        .filterExplicit(context.dataStore.get(HideExplicitKey, false))
                        .filterVideo(context.dataStore.get(HideVideoKey, false)),
                    searchResult.continuation
                )
            }
        }
    }

    private suspend fun loadSearchPage(filter: YouTube.SearchFilter) {
        YouTube
            .search(query, filter)
            .onSuccess { result ->
                viewStateMap[filter.value] =
                    ItemsPage(
                        result.items
                            .distinctBy { it.id }
                            .filterExplicit(context.dataStore.get(HideExplicitKey, false))
                            .filterVideo(context.dataStore.get(HideVideoKey, false)),
                        result.continuation,
                    )
            }.onFailure {
                reportException(it)
            }
    }
}
