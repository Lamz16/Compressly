package com.lamz.compressly.feature.imageresizer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lamz.compressly.core.common.BatchProcessingSummary
import com.lamz.compressly.core.common.CompressionUiState
import com.lamz.compressly.core.common.ProcessingItemResult
import com.lamz.compressly.core.storage.StorageManager
import com.lamz.compressly.core.util.FileUtils
import com.lamz.compressly.data.resizer.ImageResizerEngine
import com.lamz.compressly.domain.model.ImageResizeConfig
import com.lamz.compressly.domain.model.OutputImageFormat
import com.lamz.compressly.domain.model.ResizeMode
import com.lamz.compressly.feature.imagecompressor.SelectedImageItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImageResizerViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = StorageManager(application)
    private val resizerEngine = ImageResizerEngine(application, storageManager)

    private val _selectedImages = MutableStateFlow<List<SelectedImageItem>>(emptyList())
    val selectedImages: StateFlow<List<SelectedImageItem>> = _selectedImages.asStateFlow()

    private val _config = MutableStateFlow(ImageResizeConfig())
    val config: StateFlow<ImageResizeConfig> = _config.asStateFlow()

    private val _uiState = MutableStateFlow<CompressionUiState>(CompressionUiState.Idle)
    val uiState: StateFlow<CompressionUiState> = _uiState.asStateFlow()

    private var resizeJob: Job? = null

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

    fun updateMode(mode: ResizeMode) {
        _config.value = _config.value.copy(mode = mode)
    }

    fun updatePresetMaxDimension(dimension: Int) {
        _config.value = _config.value.copy(targetMaxDimension = dimension)
    }

    fun updatePercentage(percentage: Int) {
        _config.value = _config.value.copy(percentage = percentage)
    }

    fun updateDimensions(width: Int, height: Int) {
        _config.value = _config.value.copy(targetWidth = width, targetHeight = height)
    }

    fun updateMaintainAspectRatio(maintain: Boolean) {
        _config.value = _config.value.copy(maintainAspectRatio = maintain)
    }

    fun updateQuality(quality: Int) {
        _config.value = _config.value.copy(quality = quality)
    }

    fun updateFormat(format: OutputImageFormat) {
        _config.value = _config.value.copy(format = format)
    }

    fun startResize() {
        val images = _selectedImages.value
        if (images.isEmpty()) return

        resizeJob?.cancel()
        resizeJob = viewModelScope.launch {
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

                val result = resizerEngine.resizeImage(item.uri, _config.value)
                results.add(result)
            }

            val totalDuration = System.currentTimeMillis() - startTime
            val summary = BatchProcessingSummary(results, totalDuration)
            _uiState.value = CompressionUiState.Completed(summary)
        }
    }

    fun cancelResize() {
        resizeJob?.cancel()
        _uiState.value = CompressionUiState.Idle
    }

    fun resetState() {
        _uiState.value = CompressionUiState.Idle
    }
}
