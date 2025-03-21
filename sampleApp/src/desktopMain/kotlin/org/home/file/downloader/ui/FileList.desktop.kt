package org.home.file.downloader.ui

import androidx.compose.runtime.Composable
import java.awt.Desktop
import java.io.File

@Composable
internal actual fun OpenFile(path: String) {
    val file = File(path)
    if (file.exists()) {
        try {
            // Используем Desktop API для открытия файла
            Desktop.getDesktop().open(file)
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to open file: ${e.message}")
        }
    } else {
        println("File does not exist: $path")
    }
}