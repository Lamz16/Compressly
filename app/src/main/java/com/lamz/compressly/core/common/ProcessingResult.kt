package com.lamz.compressly.core.common

import android.net.Uri
import java.io.File

data class ProcessingItemResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val originalUri: Uri,
    val originalName: String,
    val originalSizeBytes: Long,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val originalFormat: String = "",
    val outputUri: Uri? = null,
    val outputFile: File? = null,
    val outputSizeBytes: Long = 0,
    val outputWidth: Int = 0,
    val outputHeight: Int = 0,
    val outputFormat: String = "",
    val isSuccess: Boolean = true,
    val errorMessage: String? = null,
    val processingDurationMs: Long = 0L
) {
    val savedSizeBytes: Long
        get() = (originalSizeBytes - outputSizeBytes).coerceAtLeast(0L)

    val savedPercentage: Int
        get() {
            if (originalSizeBytes <= 0L) return 0
            val pct = ((originalSizeBytes - outputSizeBytes).toDouble() / originalSizeBytes.toDouble()) * 100.0
            return pct.toInt().coerceIn(0, 100)
        }
}

data class BatchProcessingSummary(
    val results: List<ProcessingItemResult>,
    val totalTimeMs: Long = 0L
) {
    val successCount: Int get() = results.count { it.isSuccess }
    val failureCount: Int get() = results.count { !it.isSuccess }
    val totalOriginalBytes: Long get() = results.filter { it.isSuccess }.sumOf { it.originalSizeBytes }
    val totalOutputBytes: Long get() = results.filter { it.isSuccess }.sumOf { it.outputSizeBytes }
    val totalSavedBytes: Long get() = (totalOriginalBytes - totalOutputBytes).coerceAtLeast(0L)
    val averageSavedPercentage: Int
        get() {
            if (totalOriginalBytes <= 0L) return 0
            val pct = ((totalOriginalBytes - totalOutputBytes).toDouble() / totalOriginalBytes.toDouble()) * 100.0
            return pct.toInt().coerceIn(0, 100)
        }
}
