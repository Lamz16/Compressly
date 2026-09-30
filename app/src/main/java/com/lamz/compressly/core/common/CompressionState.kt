package com.lamz.compressly.core.common

sealed interface CompressionUiState {
    data object Idle : CompressionUiState

    data class Processing(
        val currentIndex: Int,
        val totalCount: Int,
        val currentFileName: String,
        val progressPercentage: Int
    ) : CompressionUiState

    data class Completed(
        val summary: BatchProcessingSummary
    ) : CompressionUiState

    data class Error(
        val message: String
    ) : CompressionUiState
}
