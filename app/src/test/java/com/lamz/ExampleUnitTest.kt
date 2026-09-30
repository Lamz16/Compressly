package com.lamz

import android.net.Uri
import com.lamz.compressly.core.common.BatchProcessingSummary
import com.lamz.compressly.core.common.ProcessingItemResult
import com.lamz.compressly.core.util.FileUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {
    @Test
    fun testFormatBytes() {
        assertEquals("0 B", FileUtils.formatBytes(0))
        assertEquals("500 B", FileUtils.formatBytes(500))
        assertEquals("1 KB", FileUtils.formatBytes(1024))
        assertEquals("1.5 KB", FileUtils.formatBytes(1536))
        assertEquals("1 MB", FileUtils.formatBytes(1024 * 1024))
    }

    @Test
    fun testSavedPercentage() {
        val originalSize = 10_000_000L
        val compressedSize = 2_000_000L
        val item = ProcessingItemResult(
            originalUri = Uri.parse("content://media/external/images/media/1"),
            originalName = "test.jpg",
            originalSizeBytes = originalSize,
            outputSizeBytes = compressedSize,
            isSuccess = true
        )
        assertEquals(8_000_000L, item.savedSizeBytes)
        assertEquals(80, item.savedPercentage)
    }

    @Test
    fun testBatchProcessingSummary() {
        val item1 = ProcessingItemResult(
            originalUri = Uri.parse("content://1"),
            originalName = "1.jpg",
            originalSizeBytes = 1000L,
            outputSizeBytes = 500L,
            isSuccess = true
        )
        val item2 = ProcessingItemResult(
            originalUri = Uri.parse("content://2"),
            originalName = "2.jpg",
            originalSizeBytes = 2000L,
            outputSizeBytes = 1000L,
            isSuccess = true
        )
        val summary = BatchProcessingSummary(listOf(item1, item2))
        assertEquals(2, summary.successCount)
        assertEquals(3000L, summary.totalOriginalBytes)
        assertEquals(1500L, summary.totalOutputBytes)
        assertEquals(1500L, summary.totalSavedBytes)
        assertEquals(50, summary.averageSavedPercentage)
    }
}
