package com.shared.file.downloader.file_downloader

sealed interface DownloadResult {

    val exception: Exception?

    val url: String

    data class DownloadCompleted(
        val filePath: String,
        override val url: String
    ) : DownloadResult {
        override val exception: Exception? = null
    }

    data class DownloadFailed(
        override val exception: Exception,
        override val url: String = exception.message ?: exception.cause?.message.toString()
    ) : DownloadResult

    data class ProgressUpdate(
        val progress: Progress,
        override val url: String
    ) : DownloadResult {
        override val exception: Exception? = null
    }

}

data class Progress(val downloaded: Long, val total: Long) {
    val value: Float get() = downloaded.toFloat() / total.toFloat()
}