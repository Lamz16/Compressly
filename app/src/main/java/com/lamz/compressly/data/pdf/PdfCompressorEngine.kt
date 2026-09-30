package com.lamz.compressly.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.lamz.compressly.core.common.ProcessingItemResult
import com.lamz.compressly.core.storage.StorageManager
import com.lamz.compressly.core.util.FileUtils
import com.lamz.compressly.domain.model.PdfCompressionConfig
import com.lamz.compressly.domain.model.PdfStrategy
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PdfCompressorEngine(
    private val context: Context,
    private val storageManager: StorageManager
) {

    suspend fun compressPdf(
        uri: Uri,
        config: PdfCompressionConfig,
        onProgress: (page: Int, totalPages: Int) -> Unit = { _, _ -> }
    ): ProcessingItemResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val (fileName, originalSize) = FileUtils.getFileNameAndSize(context, uri)

        // Copy source URI to temporary local file to work with ParcelFileDescriptor and PDFBox reliably
        val tempSourceFile = File(storageManager.getProcessingDir(), "source_${System.currentTimeMillis()}.pdf")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempSourceFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext ProcessingItemResult(
                originalUri = uri,
                originalName = fileName,
                originalSizeBytes = originalSize,
                isSuccess = false,
                errorMessage = "Cannot open PDF file stream."
            )

            // Determine strategy
            val resolvedStrategy = when (config.strategy) {
                PdfStrategy.NATIVE -> PdfStrategy.NATIVE
                PdfStrategy.SCANNED -> PdfStrategy.SCANNED
                PdfStrategy.SMART -> detectSmartStrategy(tempSourceFile)
            }

            val outputDir = storageManager.getOutputDir()
            val outputFile = FileUtils.generateUniqueFile(outputDir, fileName, "pdf", "compressed")

            val result = if (resolvedStrategy == PdfStrategy.NATIVE) {
                tryNativePdfOptimization(tempSourceFile, outputFile, config, onProgress)
            } else {
                compressScannedPdf(tempSourceFile, outputFile, config, onProgress)
            }

            // If native optimization yielded minimal or negative reduction, try scanned mode as fallback if smart
            val finalOutputFile: File
            if (config.strategy == PdfStrategy.SMART && result.isSuccess && outputFile.exists()) {
                val outputLen = outputFile.length()
                if (outputLen >= originalSize && originalSize > 50_000) {
                    // Try scanned compression fallback
                    val scannedFallbackFile = FileUtils.generateUniqueFile(outputDir, fileName, "pdf", "scanned_compressed")
                    val scannedResult = compressScannedPdf(tempSourceFile, scannedFallbackFile, config, onProgress)
                    if (scannedResult.isSuccess && scannedFallbackFile.length() < outputLen) {
                        outputFile.delete()
                        finalOutputFile = scannedFallbackFile
                    } else {
                        scannedFallbackFile.delete()
                        finalOutputFile = outputFile
                    }
                } else {
                    finalOutputFile = outputFile
                }
            } else {
                finalOutputFile = outputFile
            }

            if (!result.isSuccess) {
                return@withContext ProcessingItemResult(
                    originalUri = uri,
                    originalName = fileName,
                    originalSizeBytes = originalSize,
                    isSuccess = false,
                    errorMessage = result.errorMessage
                )
            }

            val duration = System.currentTimeMillis() - startTime
            val finalOutputSize = finalOutputFile.length()

            ProcessingItemResult(
                originalUri = uri,
                originalName = fileName,
                originalSizeBytes = originalSize,
                outputFile = finalOutputFile,
                outputSizeBytes = finalOutputSize,
                outputFormat = "PDF",
                isSuccess = true,
                processingDurationMs = duration
            )
        } catch (e: Exception) {
            ProcessingItemResult(
                originalUri = uri,
                originalName = fileName,
                originalSizeBytes = originalSize,
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "PDF compression failed"
            )
        } finally {
            if (tempSourceFile.exists()) {
                tempSourceFile.delete()
            }
        }
    }

    private fun detectSmartStrategy(file: File): PdfStrategy {
        return try {
            PDDocument.load(file).use { doc ->
                if (doc.isEncrypted) return PdfStrategy.SCANNED
                val stripper = PDFTextStripper()
                stripper.startPage = 1
                stripper.endPage = min(3, doc.numberOfPages)
                val text = stripper.getText(doc)
                // If the first pages contain significant text, preserve text with native optimization
                if (text.trim().length > 120) {
                    PdfStrategy.NATIVE
                } else {
                    PdfStrategy.SCANNED
                }
            }
        } catch (e: Exception) {
            PdfStrategy.SCANNED
        }
    }

    private data class EngineRunResult(val isSuccess: Boolean, val errorMessage: String? = null)

    private fun tryNativePdfOptimization(
        sourceFile: File,
        outputFile: File,
        config: PdfCompressionConfig,
        onProgress: (page: Int, totalPages: Int) -> Unit
    ): EngineRunResult {
        return try {
            PDDocument.load(sourceFile).use { doc ->
                if (doc.isEncrypted) {
                    return EngineRunResult(false, "PDF is password protected or encrypted.")
                }

                val totalPages = doc.numberOfPages
                val qualityFloat = (config.dpiPreset.quality / 100f).coerceIn(0.4f, 0.95f)

                for (pageIndex in 0 until totalPages) {
                    onProgress(pageIndex + 1, totalPages)
                    val page = doc.getPage(pageIndex)
                    val resources = page.resources ?: continue

                    val xObjectNames = resources.xObjectNames?.toList() ?: emptyList()
                    for (xName in xObjectNames) {
                        try {
                            val xObject = resources.getXObject(xName)
                            if (xObject is PDImageXObject) {
                                val origBitmap = xObject.image
                                if (origBitmap != null) {
                                    val maxDim = when (config.dpiPreset) {
                                        com.lamz.compressly.domain.model.PdfDpiPreset.HIGH -> 2000
                                        com.lamz.compressly.domain.model.PdfDpiPreset.BALANCED -> 1600
                                        com.lamz.compressly.domain.model.PdfDpiPreset.SMALL -> 1200
                                    }

                                    val currMax = max(origBitmap.width, origBitmap.height)
                                    val finalBmp = if (currMax > maxDim) {
                                        val ratio = maxDim.toFloat() / currMax.toFloat()
                                        val newW = (origBitmap.width * ratio).roundToInt().coerceAtLeast(1)
                                        val newH = (origBitmap.height * ratio).roundToInt().coerceAtLeast(1)
                                        Bitmap.createScaledBitmap(origBitmap, newW, newH, true)
                                    } else {
                                        origBitmap
                                    }

                                    val newImage = JPEGFactory.createFromImage(doc, finalBmp, qualityFloat)
                                    resources.put(xName, newImage)

                                    if (finalBmp != origBitmap) {
                                        finalBmp.recycle()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Skip single image if not convertible
                        }
                    }
                }

                doc.save(outputFile)
            }
            EngineRunResult(true)
        } catch (e: Exception) {
            EngineRunResult(false, e.localizedMessage ?: "Native PDF optimization error")
        }
    }

    private fun compressScannedPdf(
        sourceFile: File,
        outputFile: File,
        config: PdfCompressionConfig,
        onProgress: (page: Int, totalPages: Int) -> Unit
    ): EngineRunResult {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val pdfDocument = PdfDocument()

        return try {
            pfd = ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)

            val pageCount = renderer.pageCount
            if (pageCount == 0) {
                return EngineRunResult(false, "PDF document contains no pages.")
            }

            val targetDpi = config.dpiPreset.dpi
            val scale = targetDpi / 72.0f
            val jpegQuality = config.dpiPreset.quality

            for (i in 0 until pageCount) {
                onProgress(i + 1, pageCount)
                val page = renderer.openPage(i)
                val originalPtWidth = page.width
                val originalPtHeight = page.height

                val renderW = (originalPtWidth * scale).roundToInt().coerceAtLeast(1)
                val renderH = (originalPtHeight * scale).roundToInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                // Compress page bitmap to JPEG bytes
                val byteStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, byteStream)
                bitmap.recycle()

                val compressedBytes = byteStream.toByteArray()
                val compressedBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

                // Write page into android.graphics.pdf.PdfDocument
                val pageInfo = PdfDocument.PageInfo.Builder(originalPtWidth, originalPtHeight, i + 1).create()
                val docPage = pdfDocument.startPage(pageInfo)

                val paint = Paint().apply { isFilterBitmap = true }
                val matrix = Matrix()
                matrix.postScale(
                    originalPtWidth.toFloat() / compressedBitmap.width.toFloat(),
                    originalPtHeight.toFloat() / compressedBitmap.height.toFloat()
                )
                docPage.canvas.drawBitmap(compressedBitmap, matrix, paint)
                pdfDocument.finishPage(docPage)
                compressedBitmap.recycle()
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
                fos.flush()
            }

            EngineRunResult(true)
        } catch (e: Exception) {
            EngineRunResult(false, e.localizedMessage ?: "Scanned PDF compression error")
        } finally {
            try {
                pdfDocument.close()
            } catch (e: Exception) {
                // Ignore
            }
            try {
                renderer?.close()
            } catch (e: Exception) {
                // Ignore
            }
            try {
                pfd?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
