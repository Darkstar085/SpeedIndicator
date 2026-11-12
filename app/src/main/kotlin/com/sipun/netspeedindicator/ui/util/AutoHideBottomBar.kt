package com.sipun.netspeedindicator.ui.util

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Tracks scroll direction and hides/shows the bottom navigation bar: the bar hides
 * once the user scrolls content down past [threshold] pixels, comes back on upward
 * scrolls, and is always visible at the top of a scroll container.
 *
 * Call this once per screen, right where the scroll state is created.
 * Screens must provide [LocalBottomBarVisible] further up the tree.
 */
@Composable
fun AutoHideBottomBarEffect(scrollState: ScrollState, threshold: Int = 18) {
    val bottomBarVisible = LocalBottomBarVisible.current
    val lastOffset = remember { mutableIntStateOf(scrollState.value) }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }
            .distinctUntilChanged()
            .collect { offset ->
                val delta = offset - lastOffset.intValue
                lastOffset.intValue = offset
                when {
                    offset <= 0 -> bottomBarVisible.value = true
                    delta > threshold -> bottomBarVisible.value = false
                    delta < -threshold -> bottomBarVisible.value = true
                }
            }
    }
}

@Composable
fun AutoHideBottomBarEffect(listState: LazyListState, threshold: Int = 18) {
    val bottomBarVisible = LocalBottomBarVisible.current
    val lastIndex = remember { mutableIntStateOf(0) }
    val lastOffset = remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow {
            Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                // One whole item forward counts as far more than [threshold].
                val delta = (index - lastIndex.intValue) * 100_000 + (offset - lastOffset.intValue)
                lastIndex.intValue = index
                lastOffset.intValue = offset
                when {
                    index == 0 && offset == 0 -> bottomBarVisible.value = true
                    delta > threshold -> bottomBarVisible.value = false
                    delta < -threshold -> bottomBarVisible.value = true
                }
            }
    }
}
