package org.home.file.downloader.ui

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentInteractionController

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun OpenFile(path: String) {
    val fileUrl = NSURL.fileURLWithPath(path)
    // Создаем UIDocumentInteractionController
    val documentInteractionController =
        UIDocumentInteractionController.interactionControllerWithURL(fileUrl)
    // Показываем опции открытия
    val rootViewController =
        platform.UIKit.UIApplication.sharedApplication.keyWindow?.rootViewController
    if (rootViewController != null && !documentInteractionController.presentOptionsMenuFromRect(
            rect = CGRectMake(0.0, 0.0, 0.0, 0.0), // Прямоугольник для всплывающего меню
            inView = rootViewController.view, // Родительский UIView
            animated = true
        )
    ) {
        println("No applications available to open this file type.")
    }
}