package com.lamz.compressly.core.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.text.DecimalFormat
import java.util.Locale

object FileUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        val df = DecimalFormat("#,##0.#")
        return "${df.format(value)} ${units[index]}"
    }

    fun getFileNameAndSize(context: Context, uri: Uri): Pair<String, Long> {
        var name = "unknown_file"
        var size = 0L

        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val displayName = cursor.getString(nameIndex)
                            if (!displayName.isNullOrBlank()) {
                                name = displayName
                            }
                        }
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            size = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (size <= 0L) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    size = stream.available().toLong()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        if (name == "unknown_file") {
            uri.lastPathSegment?.let { seg ->
                name = seg.substringAfterLast('/')
            }
        }

        return Pair(name, size)
    }

    fun generateUniqueFile(directory: File, baseName: String, extension: String, suffix: String): File {
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val cleanBaseName = baseName.substringBeforeLast('.')
            .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val cleanExtension = extension.trimStart('.').lowercase(Locale.ROOT)

        var candidate = File(directory, "${cleanBaseName}_${suffix}.${cleanExtension}")
        var counter = 1
        while (candidate.exists()) {
            candidate = File(directory, "${cleanBaseName}_${suffix}_${counter}.${cleanExtension}")
            counter++
        }
        return candidate
    }

    fun getOutputExtension(format: String): String {
        return when (format.uppercase(Locale.ROOT)) {
            "PNG" -> "png"
            "WEBP" -> "webp"
            "PDF" -> "pdf"
            else -> "jpg"
        }
    }
}
