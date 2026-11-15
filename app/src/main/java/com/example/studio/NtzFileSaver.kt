package com.example.studio

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class SavedFileResult(
  val fileName: String,
  val mimeType: String,
  val uri: Uri,
)

object NtzFileSaver {

  fun sanitizeFileName(rawName: String, fallbackExt: String = "bin"): String {
    val trimmed = rawName.trim().ifEmpty { "ntz-export-${System.currentTimeMillis()}.$fallbackExt" }
    val safe = trimmed.replace(Regex("[\\\\/:*?\"<>|\\r\\n]+"), "_")
    return if (safe.contains('.')) safe else "$safe.$fallbackExt"
  }

  fun resolveMimeType(fileName: String, providedMime: String?): String {
    val cleanProvided = providedMime?.substringBefore(';')?.trim().orEmpty()
    if (cleanProvided.isNotEmpty() && cleanProvided != "application/octet-stream") {
      return cleanProvided
    }
    return when (fileName.substringAfterLast('.', "").lowercase()) {
      "mp4" -> "video/mp4"
      "webm" -> "video/webm"
      "mov" -> "video/quicktime"
      "csv" -> "text/csv"
      "html", "htm" -> "text/html"
      "json" -> "application/json"
      else -> "application/octet-stream"
    }
  }

  fun publishTempFileToDownloads(
    context: Context,
    tempFile: File,
    requestedName: String,
    providedMime: String?,
  ): SavedFileResult {
    val ext =
      when {
        providedMime?.contains("mp4", true) == true -> "mp4"
        providedMime?.contains("quicktime", true) == true -> "mov"
        providedMime?.contains("webm", true) == true -> "webm"
        providedMime?.contains("csv", true) == true -> "csv"
        else -> "bin"
      }
    val fileName = sanitizeFileName(requestedName, ext)
    val mimeType = resolveMimeType(fileName, providedMime)

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val resolver = context.contentResolver
      val values =
        ContentValues().apply {
          put(MediaStore.Downloads.DISPLAY_NAME, fileName)
          put(MediaStore.Downloads.MIME_TYPE, mimeType)
          put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/NTZ Motion Studio")
          put(MediaStore.Downloads.IS_PENDING, 1)
        }
      val uri =
        resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
          ?: throw IllegalStateException("Tidak dapat membuat file di folder Downloads")
      try {
        resolver.openOutputStream(uri)?.use { out ->
          FileInputStream(tempFile).use { input -> input.copyTo(out) }
        } ?: throw IllegalStateException("Gagal membuka output stream Downloads")
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        SavedFileResult(fileName = fileName, mimeType = mimeType, uri = uri)
      } catch (t: Throwable) {
        runCatching { resolver.delete(uri, null, null) }
        throw t
      }
    } else {
      val downloadsDir =
        File(
          context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir,
          "NTZ Motion Studio",
        )
      if (!downloadsDir.exists()) downloadsDir.mkdirs()
      var target = File(downloadsDir, fileName)
      if (target.exists()) {
        val base = fileName.substringBeforeLast('.', fileName)
        val dotExt = fileName.substringAfterLast('.', "")
        val suffix = System.currentTimeMillis() % 10000
        target =
          if (dotExt.isNotEmpty()) File(downloadsDir, "${base}_$suffix.$dotExt")
          else File(downloadsDir, "${base}_$suffix")
      }
      FileInputStream(tempFile).use { input ->
        FileOutputStream(target).use { out -> input.copyTo(out) }
      }
      val uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
      SavedFileResult(fileName = target.name, mimeType = mimeType, uri = uri)
    }
  }

  fun shareOrOpenFile(context: Context, result: SavedFileResult) {
    val shareIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = result.mimeType
        putExtra(Intent.EXTRA_STREAM, result.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    val chooser =
      Intent.createChooser(shareIntent, result.fileName).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(chooser)
  }
}
