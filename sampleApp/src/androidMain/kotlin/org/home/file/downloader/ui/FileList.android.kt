package org.home.file.downloader.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
internal actual fun OpenFile(path: String) {
    val context = LocalContext.current
    val file = File(path)

    if (file.exists()) {

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val mimeType = context.contentResolver.getType(uri) ?: getMimeType(file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } else {
        println("File does not exist: $path")
    }
}

private fun getMimeType(file: File): String {
    return when (file.extension.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        "mp4" -> "video/mp4"
        "3gp" -> "video/3gpp"
        "webm" -> "video/webm"
        else -> "*/*"
    }
}