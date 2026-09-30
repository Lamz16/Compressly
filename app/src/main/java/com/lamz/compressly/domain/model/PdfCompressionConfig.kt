package com.lamz.compressly.domain.model

enum class PdfStrategy(val title: String, val description: String) {
    SMART("Smart (Auto-Detect)", "Chooses native optimization or scanned mode automatically"),
    NATIVE("Native PDF Optimization", "Keeps text searchable & vectors intact, compresses embedded photos"),
    SCANNED("Scanned Document Mode", "Rasterizes pages at optimal DPI, best for scanned book/invoices")
}

enum class PdfDpiPreset(val title: String, val dpi: Int, val quality: Int, val description: String) {
    HIGH("High Quality (220 DPI)", 220, 85, "Great for detailed reading and printing"),
    BALANCED("Balanced (170 DPI)", 170, 78, "Recommended for screen reading & emailing"),
    SMALL("Small Size (130 DPI)", 130, 68, "Maximum file reduction for uploads")
}

data class PdfCompressionConfig(
    val strategy: PdfStrategy = PdfStrategy.SMART,
    val dpiPreset: PdfDpiPreset = PdfDpiPreset.BALANCED
)
