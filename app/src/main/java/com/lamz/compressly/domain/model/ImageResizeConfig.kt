package com.lamz.compressly.domain.model

enum class ResizeMode(val title: String) {
    PRESET("Preset Max Dimension"),
    PERCENTAGE("Percentage Scale"),
    CUSTOM_DIMENSIONS("Width x Height")
}

data class ImageResizeConfig(
    val mode: ResizeMode = ResizeMode.PRESET,
    val targetMaxDimension: Int = 1920,
    val percentage: Int = 50,
    val targetWidth: Int = 1920,
    val targetHeight: Int = 1080,
    val maintainAspectRatio: Boolean = true,
    val quality: Int = 90,
    val format: OutputImageFormat = OutputImageFormat.AUTO
)
