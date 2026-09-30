package com.lamz.compressly.data.resizer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.lamz.compressly.core.common.ProcessingItemResult
import com.lamz.compressly.core.storage.StorageManager
import com.lamz.compressly.core.util.FileUtils
import com.lamz.compressly.domain.model.ImageResizeConfig
import com.lamz.compressly.domain.model.OutputImageFormat
import com.lamz.compressly.domain.model.ResizeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

class ImageResizerEngine(
    private val context: Context,
    private val storageManager: StorageManager
) {

    suspend fun resizeImage(
        uri: Uri,
        config: ImageResizeConfig
    ): ProcessingItemResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val (fileName, originalSize) = FileUtils.getFileNameAndSize(context, uri)

        try {
            // 1. Decode bounds and EXIF
            val (origWidth, origHeight, mimeType) = decodeBounds(uri)
            if (origWidth <= 0 || origHeight <= 0) {
                return@withContext ProcessingItemResult(
                    originalUri = uri,
                    originalName = fileName,
                    originalSizeBytes = originalSize,
                    isSuccess = false,
                    errorMessage = "Cannot read image dimensions for resizing."
                )
            }

            val orientation = getExifOrientation(uri)
            val isRotated90or270 = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                    orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
                    orientation == ExifInterface.ORIENTATION_TRANSVERSE ||
                    orientation == ExifInterface.ORIENTATION_TRANSPOSE

            val effectiveOrigWidth = if (isRotated90or270) origHeight else origWidth
            val effectiveOrigHeight = if (isRotated90or270) origWidth else origHeight

            // 2. Calculate target Width and Height
            val (targetWidth, targetHeight) = calculateTargetDimensions(
                effectiveOrigWidth,
                effectiveOrigHeight,
                config
            )

            // 3. Downsample decode
            val sampleSize = calculateInSampleSize(origWidth, origHeight, targetWidth, targetHeight)
            val decodedBitmap = decodeSampledBitmap(uri, sampleSize)
                ?: return@withContext ProcessingItemResult(
                    originalUri = uri,
                    originalName = fileName,
                    originalSizeBytes = originalSize,
                    originalWidth = effectiveOrigWidth,
                    originalHeight = effectiveOrigHeight,
                    isSuccess = false,
                    errorMessage = "Failed to decode image data into memory."
                )

            // 4. Orient and scale
            val resizedBitmap = applyOrientationAndScale(decodedBitmap, orientation, targetWidth, targetHeight)
            if (resizedBitmap != decodedBitmap) {
                decodedBitmap.recycle()
            }

            // 5. Output format and compress
            val (format, ext) = resolveFormat(config.format, mimeType, resizedBitmap.hasAlpha())
            val outputDir = storageManager.getOutputDir()
            val outputFile = FileUtils.generateUniqueFile(outputDir, fileName, ext, "resized")

            FileOutputStream(outputFile).use { fos ->
                resizedBitmap.compress(format, config.quality.coerceIn(1, 100), fos)
                fos.flush()
            }

            val finalW = resizedBitmap.width
            val finalH = resizedBitmap.height
            resizedBitmap.recycle()

            val duration = System.currentTimeMillis() - startTime
            val outputSize = outputFile.length()

            ProcessingItemResult(
                originalUri = uri,
                originalName = fileName,
                originalSizeBytes = originalSize,
                originalWidth = effectiveOrigWidth,
                originalHeight = effectiveOrigHeight,
                originalFormat = mimeType ?: ext.uppercase(),
                outputFile = outputFile,
                outputSizeBytes = outputSize,
                outputWidth = finalW,
                outputHeight = finalH,
                outputFormat = ext.uppercase(),
                isSuccess = true,
                processingDurationMs = duration
            )
        } catch (oom: OutOfMemoryError) {
            System.gc()
            ProcessingItemResult(
                originalUri = uri,
                originalName = fileName,
                originalSizeBytes = originalSize,
                isSuccess = false,
                errorMessage = "Resizing ran out of memory. Try a smaller resolution."
            )
        } catch (e: Exception) {
            ProcessingItemResult(
                originalUri = uri,
                originalName = fileName,
                originalSizeBytes = originalSize,
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Unknown resize error"
            )
        }
    }

    private fun calculateTargetDimensions(
        origW: Int,
        origH: Int,
        config: ImageResizeConfig
    ): Pair<Int, Int> {
        return when (config.mode) {
            ResizeMode.PERCENTAGE -> {
                val factor = config.percentage.coerceIn(5, 500) / 100f
                val w = (origW * factor).roundToInt().coerceAtLeast(1)
                val h = (origH * factor).roundToInt().coerceAtLeast(1)
                Pair(w, h)
            }
            ResizeMode.PRESET -> {
                val maxDim = config.targetMaxDimension
                val currentMax = max(origW, origH)
                if (currentMax > maxDim) {
                    val ratio = maxDim.toFloat() / currentMax.toFloat()
                    val w = (origW * ratio).roundToInt().coerceAtLeast(1)
                    val h = (origH * ratio).roundToInt().coerceAtLeast(1)
                    Pair(w, h)
                } else {
                    Pair(origW, origH)
                }
            }
            ResizeMode.CUSTOM_DIMENSIONS -> {
                var reqW = config.targetWidth.coerceAtLeast(1)
                var reqH = config.targetHeight.coerceAtLeast(1)
                if (config.maintainAspectRatio) {
                    val aspect = origW.toFloat() / origH.toFloat()
                    if (reqW > 0 && reqH > 0) {
                        // Fit within bounding box while maintaining aspect ratio
                        if (reqW.toFloat() / reqH.toFloat() > aspect) {
                            reqW = (reqH * aspect).roundToInt().coerceAtLeast(1)
                        } else {
                            reqH = (reqW / aspect).roundToInt().coerceAtLeast(1)
                        }
                    }
                }
                Pair(reqW, reqH)
            }
        }
    }

    private fun decodeBounds(uri: Uri): Triple<Int, Int, String?> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        return Triple(options.outWidth, options.outHeight, options.outMimeType)
    }

    private fun getExifOrientation(uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun decodeSampledBitmap(uri: Uri, sampleSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun applyOrientationAndScale(
        src: Bitmap,
        orientation: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }

        val rotated = if (!matrix.isIdentity) {
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        } else {
            src
        }

        return if (rotated.width != targetWidth || rotated.height != targetHeight) {
            val scaled = Bitmap.createScaledBitmap(rotated, targetWidth, targetHeight, true)
            if (scaled != rotated && rotated != src) {
                rotated.recycle()
            }
            scaled
        } else {
            rotated
        }
    }

    private fun resolveFormat(
        outputFormat: OutputImageFormat,
        sourceMimeType: String?,
        hasAlpha: Boolean
    ): Pair<Bitmap.CompressFormat, String> {
        return when (outputFormat) {
            OutputImageFormat.PNG -> Pair(Bitmap.CompressFormat.PNG, "png")
            OutputImageFormat.WEBP -> {
                val fmt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                Pair(fmt, "webp")
            }
            OutputImageFormat.JPEG -> Pair(Bitmap.CompressFormat.JPEG, "jpg")
            OutputImageFormat.AUTO -> {
                if (hasAlpha) {
                    Pair(Bitmap.CompressFormat.PNG, "png")
                } else {
                    Pair(Bitmap.CompressFormat.JPEG, "jpg")
                }
            }
        }
    }
}
