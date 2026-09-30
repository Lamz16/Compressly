package com.lamz.compressly.data.compressor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.lamz.compressly.core.common.ProcessingItemResult
import com.lamz.compressly.core.storage.StorageManager
import com.lamz.compressly.core.util.FileUtils
import com.lamz.compressly.domain.model.ImageCompressionConfig
import com.lamz.compressly.domain.model.ImagePreset
import com.lamz.compressly.domain.model.OutputImageFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

class ImageCompressorEngine(
    private val context: Context,
    private val storageManager: StorageManager
) {

    suspend fun compressImage(
        uri: Uri,
        config: ImageCompressionConfig
    ): ProcessingItemResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val (fileName, originalSize) = FileUtils.getFileNameAndSize(context, uri)

        try {
            // 1. Read bounds and EXIF orientation without full memory allocation
            val (origWidth, origHeight, mimeType) = decodeBounds(uri)
            if (origWidth <= 0 || origHeight <= 0) {
                return@withContext ProcessingItemResult(
                    originalUri = uri,
                    originalName = fileName,
                    originalSizeBytes = originalSize,
                    isSuccess = false,
                    errorMessage = "Cannot read image dimensions. File may be corrupted or unsupported."
                )
            }

            val orientation = getExifOrientation(uri)
            val isRotated90or270 = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                    orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
                    orientation == ExifInterface.ORIENTATION_TRANSVERSE ||
                    orientation == ExifInterface.ORIENTATION_TRANSPOSE

            val effectiveOrigWidth = if (isRotated90or270) origHeight else origWidth
            val effectiveOrigHeight = if (isRotated90or270) origWidth else origHeight

            // 2. Determine target dimensions based on config
            val maxAllowedDimension = config.effectiveMaxDimension
            val scaleFactor = calculateScaleFactor(effectiveOrigWidth, effectiveOrigHeight, maxAllowedDimension)

            val targetWidth = (effectiveOrigWidth * scaleFactor).roundToInt().coerceAtLeast(1)
            val targetHeight = (effectiveOrigHeight * scaleFactor).roundToInt().coerceAtLeast(1)

            // 3. Decode sampled bitmap into memory safely
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

            // 4. Apply EXIF orientation and final dimension adjustment
            val orientedBitmap = applyOrientationAndScale(decodedBitmap, orientation, targetWidth, targetHeight)
            if (orientedBitmap != decodedBitmap) {
                decodedBitmap.recycle()
            }

            // 5. Determine output format & transparency handling
            val hasAlpha = orientedBitmap.hasAlpha()
            val formatResult = resolveCompressFormat(config.format, mimeType, hasAlpha)
            val compressFormat = formatResult.format
            val fileExtension = formatResult.extension

            // Prepare bitmap for JPEG if it had alpha
            val finalBitmapToCompress: Bitmap
            if (compressFormat == Bitmap.CompressFormat.JPEG && hasAlpha) {
                finalBitmapToCompress = Bitmap.createBitmap(
                    orientedBitmap.width,
                    orientedBitmap.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(finalBitmapToCompress)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(orientedBitmap, 0f, 0f, null)
                orientedBitmap.recycle()
            } else {
                finalBitmapToCompress = orientedBitmap
            }

            // 6. Adaptive or preset compression
            val compressedBytes: ByteArray = if (config.preset == ImagePreset.TARGET_SIZE && config.targetSizeBytes != null && config.targetSizeBytes > 0) {
                performAdaptiveCompression(finalBitmapToCompress, compressFormat, config.targetSizeBytes)
            } else {
                encodeToByteArray(finalBitmapToCompress, compressFormat, config.effectiveQuality)
            }

            // 7. Write result to output file
            val outputDir = storageManager.getOutputDir()
            val outputFile = FileUtils.generateUniqueFile(outputDir, fileName, fileExtension, "compressed")
            FileOutputStream(outputFile).use { fos ->
                fos.write(compressedBytes)
                fos.flush()
            }

            // 8. Optionally preserve EXIF
            if (config.keepExif && (fileExtension == "jpg" || fileExtension == "jpeg")) {
                try {
                    copyExif(uri, outputFile)
                } catch (e: Exception) {
                    // Ignore EXIF copy errors gracefully
                }
            }

            val finalWidth = finalBitmapToCompress.width
            val finalHeight = finalBitmapToCompress.height
            finalBitmapToCompress.recycle()

            val duration = System.currentTimeMillis() - startTime
            val outputSize = outputFile.length()

            ProcessingItemResult(
                originalUri = uri,
                originalName = fileName,
                originalSizeBytes = originalSize,
                originalWidth = effectiveOrigWidth,
                originalHeight = effectiveOrigHeight,
                originalFormat = mimeType ?: fileExtension.uppercase(),
                outputFile = outputFile,
                outputSizeBytes = outputSize,
                outputWidth = finalWidth,
                outputHeight = finalHeight,
                outputFormat = fileExtension.uppercase(),
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
                errorMessage = "Image is too large for available device memory. Try a smaller preset."
            )
        } catch (e: Exception) {
            ProcessingItemResult(
                originalUri = uri,
                originalName = fileName,
                originalSizeBytes = originalSize,
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Unknown compression failure"
            )
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

    private fun calculateScaleFactor(width: Int, height: Int, maxDimension: Int): Float {
        val currentMax = max(width, height)
        return if (currentMax > maxDimension) {
            maxDimension.toFloat() / currentMax.toFloat()
        } else {
            1.0f
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

        // Now scale to exact target dimensions if different
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

    private data class FormatInfo(val format: Bitmap.CompressFormat, val extension: String)

    private fun resolveCompressFormat(
        outputFormat: OutputImageFormat,
        sourceMimeType: String?,
        hasAlpha: Boolean
    ): FormatInfo {
        return when (outputFormat) {
            OutputImageFormat.PNG -> FormatInfo(Bitmap.CompressFormat.PNG, "png")
            OutputImageFormat.WEBP -> {
                val fmt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (hasAlpha) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                FormatInfo(fmt, "webp")
            }
            OutputImageFormat.JPEG -> FormatInfo(Bitmap.CompressFormat.JPEG, "jpg")
            OutputImageFormat.AUTO -> {
                if (hasAlpha) {
                    // Retain transparency with WebP or PNG
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        FormatInfo(Bitmap.CompressFormat.WEBP_LOSSY, "webp")
                    } else {
                        FormatInfo(Bitmap.CompressFormat.PNG, "png")
                    }
                } else {
                    FormatInfo(Bitmap.CompressFormat.JPEG, "jpg")
                }
            }
        }
    }

    private fun encodeToByteArray(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        val clampedQuality = quality.coerceIn(1, 100)
        bitmap.compress(format, clampedQuality, stream)
        return stream.toByteArray()
    }

    private fun performAdaptiveCompression(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        targetSizeBytes: Long
    ): ByteArray {
        var lowQuality = 30
        var highQuality = 95
        var bestBytes: ByteArray = encodeToByteArray(bitmap, format, highQuality)

        if (bestBytes.size <= targetSizeBytes) {
            return bestBytes
        }

        // Binary search for highest quality that fits targetSizeBytes
        var iterations = 0
        val maxIterations = 7

        while (lowQuality <= highQuality && iterations < maxIterations) {
            iterations++
            val midQuality = (lowQuality + highQuality) / 2
            val candidateBytes = encodeToByteArray(bitmap, format, midQuality)

            if (candidateBytes.size <= targetSizeBytes) {
                bestBytes = candidateBytes
                lowQuality = midQuality + 1 // Try higher quality
            } else {
                bestBytes = candidateBytes
                highQuality = midQuality - 1 // Reduce quality
            }
        }

        // If still oversized after binary search, downscale bitmap slightly and re-compress at quality 65
        if (bestBytes.size > targetSizeBytes && bitmap.width > 400 && bitmap.height > 400) {
            val downscaleRatio = 0.75f
            val newW = (bitmap.width * downscaleRatio).roundToInt()
            val newH = (bitmap.height * downscaleRatio).roundToInt()
            val smallerBitmap = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
            val fallbackBytes = encodeToByteArray(smallerBitmap, format, 70)
            if (smallerBitmap != bitmap) smallerBitmap.recycle()
            if (fallbackBytes.size < bestBytes.size) {
                return fallbackBytes
            }
        }

        return bestBytes
    }

    private fun copyExif(sourceUri: Uri, destFile: File) {
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            val sourceExif = ExifInterface(inputStream)
            val destExif = ExifInterface(destFile.absolutePath)

            val tags = arrayOf(
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_FLASH,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_WHITE_BALANCE
            )

            for (tag in tags) {
                val value = sourceExif.getAttribute(tag)
                if (value != null) {
                    destExif.setAttribute(tag, value)
                }
            }
            destExif.saveAttributes()
        }
    }
}
