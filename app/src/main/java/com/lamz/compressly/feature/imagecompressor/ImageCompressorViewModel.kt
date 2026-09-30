package com.lamz.compressly.feature.imagecompressor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lamz.compressly.core.common.BatchProcessingSummary
import com.lamz.compressly.core.common.CompressionUiState
import com.lamz.compressly.core.common.ProcessingItemResult
import com.lamz.compressly.core.storage.PreferencesManager
import com.lamz.compressly.core.storage.StorageManager
import com.lamz.compressly.core.util.FileUtils
import com.lamz.compressly.data.compressor.ImageCompressorEngine
import com.lamz.compressly.domain.model.ImageCompressionConfig
import com.lamz.compressly.domain.model.ImagePreset
import com.lamz.compressly.domain.model.OutputImageFormat
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SelectedImageItem(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long
)

class ImageCompressorViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = StorageManager(application)
    private val compressorEngine = ImageCompressorEngine(application, storageManager)
    private val preferencesManager = PreferencesManager(application)

    private val _selectedImages = MutableStateFlow<List<SelectedImageItem>>(emptyList())
    val selectedImages: StateFlow<List<SelectedImageItem>> = _selectedImages.asStateFlow()

    private val _config = MutableStateFlow(ImageCompressionConfig())
    val config: StateFlow<ImageCompressionConfig> = _config.asStateFlow()

    private val _uiState = MutableStateFlow<CompressionUiState>(CompressionUiState.Idle)
    val uiState: StateFlow<CompressionUiState> = _uiState.asStateFlow()

    private var compressionJob: Job? = null

    init {
        viewModelScope.launch {
            val defaultPresetName = preferencesManager.defaultImagePreset.first()
            val defaultFormatName = preferencesManager.defaultImageFormat.first()
            val stripExif = preferencesManager.stripExif.first()

            val preset = try {
                ImagePreset.valueOf(defaultPresetName)
            } catch (e: Exception) {
                ImagePreset.BALANCED
            }

            val format = try {
                OutputImageFormat.valueOf(defaultFormatName)
            } catch (e: Exception) {
                OutputImageFormat.AUTO
            }

            _config.value = _config.value.copy(
                preset = preset,
                format = format,
                keepExif = !stripExif
            )
        }
    }

    fun setSelectedImages(uris: List<Uri>) {
        val items = uris.map { uri ->
            val (name, size) = FileUtils.getFileNameAndSize(getApplication(), uri)
            SelectedImageItem(uri, name, size)
        }
        _selectedImages.value = items
        _uiState.value = CompressionUiState.Idle
    }

    fun removeImage(item: SelectedImageItem) {
        _selectedImages.value = _selectedImages.value.filter { it.uri != item.uri }
    }

    fun clearImages() {
        _selectedImages.value = emptyList()
        _uiState.value = CompressionUiState.Idle
    }

    fun updatePreset(preset: ImagePreset) {
        _config.value = _config.value.copy(preset = preset)
    }

    fun updateTargetSize(targetKb: Long?) {
        val bytes = if (targetKb != null && targetKb > 0) targetKb * 1024L else null
        _config.value = _config.value.copy(
            targetSizeBytes = bytes,
            preset = if (bytes != null) ImagePreset.TARGET_SIZE else _config.value.preset
        )
    }

    fun updateManualQuality(quality: Int) {
        _config.value = _config.value.copy(manualQuality = quality)
    }

    fun updateManualMaxDimension(dimension: Int) {
        _config.value = _config.value.copy(manualMaxDimension = dimension)
    }

    fun updateFormat(format: OutputImageFormat) {
        _config.value = _config.value.copy(format = format)
    }

    fun updateKeepExif(keep: Boolean) {
        _config.value = _config.value.copy(keepExif = keep)
    }

    fun startCompression() {
        val images = _selectedImages.value
        if (images.isEmpty()) return

        compressionJob?.cancel()
        compressionJob = viewModelScope.launch {
            val total = images.size
            val results = mutableListOf<ProcessingItemResult>()
            val startTime = System.currentTimeMillis()

            for (index in images.indices) {
                val item = images[index]
                val pct = ((index.toFloat() / total.toFloat()) * 100).toInt()
                _uiState.value = CompressionUiState.Processing(
                    currentIndex = index + 1,
                    totalCount = total,
                    currentFileName = item.name,
                    progressPercentage = pct
                )

                val result = compressorEngine.compressImage(item.uri, _config.value)
                results.add(result)
            }

            val totalDuration = System.currentTimeMillis() - startTime
            val summary = BatchProcessingSummary(results, totalDuration)
            _uiState.value = CompressionUiState.Completed(summary)
        }
    }

    fun cancelCompression() {
        compressionJob?.cancel()
        _uiState.value = CompressionUiState.Idle
    }

    fun resetState() {
        _uiState.value = CompressionUiState.Idle
    }
}
