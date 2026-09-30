package com.lamz.compressly.feature.pdfcompressor

import android.app.Application
import android.net.Uri
import android.graphics.pdf.PdfRenderer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lamz.compressly.core.common.BatchProcessingSummary
import com.lamz.compressly.core.common.CompressionUiState
import com.lamz.compressly.core.storage.PreferencesManager
import com.lamz.compressly.core.storage.StorageManager
import com.lamz.compressly.core.util.FileUtils
import com.lamz.compressly.data.pdf.PdfCompressorEngine
import com.lamz.compressly.domain.model.PdfCompressionConfig
import com.lamz.compressly.domain.model.PdfDpiPreset
import com.lamz.compressly.domain.model.PdfStrategy
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SelectedPdfInfo(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val pageCount: Int
)

class PdfCompressorViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = StorageManager(application)
    private val pdfEngine = PdfCompressorEngine(application, storageManager)
    private val preferencesManager = PreferencesManager(application)

    private val _selectedPdf = MutableStateFlow<SelectedPdfInfo?>(null)
    val selectedPdf: StateFlow<SelectedPdfInfo?> = _selectedPdf.asStateFlow()

    private val _config = MutableStateFlow(PdfCompressionConfig())
    val config: StateFlow<PdfCompressionConfig> = _config.asStateFlow()

    private val _uiState = MutableStateFlow<CompressionUiState>(CompressionUiState.Idle)
    val uiState: StateFlow<CompressionUiState> = _uiState.asStateFlow()

    private var compressionJob: Job? = null

    init {
        viewModelScope.launch {
            val defaultStrategyStr = preferencesManager.defaultPdfStrategy.first()
            val strategy = try {
                PdfStrategy.valueOf(defaultStrategyStr)
            } catch (e: Exception) {
                PdfStrategy.SMART
            }
            _config.value = _config.value.copy(strategy = strategy)
        }
    }

    fun setSelectedPdf(uri: Uri) {
        val (name, size) = FileUtils.getFileNameAndSize(getApplication(), uri)
        var pages = 1
        try {
            getApplication<Application>().contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                pages = renderer.pageCount
                renderer.close()
            }
        } catch (e: Exception) {
            // In case PdfRenderer cannot read it (e.g. password protected), default to 1
        }

        _selectedPdf.value = SelectedPdfInfo(uri, name, size, pages)
        _uiState.value = CompressionUiState.Idle
    }

    fun clearPdf() {
        _selectedPdf.value = null
        _uiState.value = CompressionUiState.Idle
    }

    fun updateStrategy(strategy: PdfStrategy) {
        _config.value = _config.value.copy(strategy = strategy)
    }

    fun updateDpiPreset(preset: PdfDpiPreset) {
        _config.value = _config.value.copy(dpiPreset = preset)
    }

    fun startCompression() {
        val pdf = _selectedPdf.value ?: return

        compressionJob?.cancel()
        compressionJob = viewModelScope.launch {
            val totalPages = pdf.pageCount
            val startTime = System.currentTimeMillis()

            _uiState.value = CompressionUiState.Processing(
                currentIndex = 1,
                totalCount = 1,
                currentFileName = pdf.name,
                progressPercentage = 5
            )

            val result = pdfEngine.compressPdf(
                uri = pdf.uri,
                config = _config.value,
                onProgress = { page, total ->
                    val pct = ((page.toFloat() / total.toFloat()) * 90).toInt()
                    _uiState.value = CompressionUiState.Processing(
                        currentIndex = page,
                        totalCount = total,
                        currentFileName = "${pdf.name} (Page $page/$total)",
                        progressPercentage = pct
                    )
                }
            )

            val duration = System.currentTimeMillis() - startTime
            val summary = BatchProcessingSummary(listOf(result), duration)
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
