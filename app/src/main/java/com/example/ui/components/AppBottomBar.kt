package com.example.ui.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState

@Composable
fun AppBottomBar(
    hazeState: HazeState,
    openedFileUri: Uri?,
    currentPage: Int,
    totalPages: Int,
    pageSize: Int,
    filteredCount: Int,
    onFirstPage: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onLastPage: () -> Unit,
    onJumpToPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (openedFileUri != null) {
        PaginationBar(
            hazeState = hazeState,
            currentPage = currentPage,
            totalPages = totalPages,
            pageSize = pageSize,
            filteredCount = filteredCount,
            onFirstPage = onFirstPage,
            onPrevPage = onPrevPage,
            onNextPage = onNextPage,
            onLastPage = onLastPage,
            onJumpToPage = onJumpToPage
        )
    }
}
