package com.lamz.compressly.domain.model

enum class ImagePreset(val title: String, val description: String, val defaultQuality: Int, val maxDimension: Int) {
    MAXIMUM("Maximum Quality", "Quality 92-95%, max 4096px", 94, 4096),
    HIGH("High Quality", "Quality 85-90%, max 2560px", 88, 2560),
    BALANCED("Balanced", "Quality 80-85%, max 1920px (Recommended)", 82, 1920),
    SMALL("Small Size", "Quality 70-80%, max 1280px", 75, 1280),
    TARGET_SIZE("Target Size", "Adaptive search to fit specified KB/MB", 80, 2560),
    CUSTOM("Custom", "Manual quality and dimension control", 80, 1920)
}

enum class OutputImageFormat(val title: String, val extension: String) {
    AUTO("Smart Auto", "auto"),
    JPEG("JPEG (.jpg)", "jpg"),
    WEBP("WebP (.webp)", "webp"),
    PNG("PNG (.png)", "png")
}

data class ImageCompressionConfig(
    val preset: ImagePreset = ImagePreset.BALANCED,
    val targetSizeBytes: Long? = null,
    val manualQuality: Int = 82,
    val manualMaxDimension: Int = 1920,
    val format: OutputImageFormat = OutputImageFormat.AUTO,
    val keepExif: Boolean = false
) {
    val effectiveQuality: Int
        get() = when (preset) {
            ImagePreset.CUSTOM -> manualQuality
            ImagePreset.TARGET_SIZE -> manualQuality
            else -> preset.defaultQuality
        }

    val effectiveMaxDimension: Int
        get() = when (preset) {
            ImagePreset.CUSTOM -> manualMaxDimension
            ImagePreset.TARGET_SIZE -> manualMaxDimension
            else -> preset.maxDimension
        }
}
