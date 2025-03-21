package com.shared.file.downloader.file_downloader.utils

import com.shared.file.downloader.file_downloader.remote.ChunkedStrategy
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.head
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess

// File information
internal data class FileInfo(
    val fileSize: Long,
    val acceptRange: Boolean = false
) {
    // Get file info, including size and whether it supports range requests or not
    // Выбор стратегии в зависимости от поддержки range-запросов
    companion object {
        suspend fun getFileInfo(url: String, logger: Logger, client: HttpClient): FileInfo {
            logger.log("FileDownloader getFileInfo url = $url")

            val response = client.head(url)

            logger.log("FileDownloader getFileInfo response = $response")

            if (!response.status.isSuccess()) {
                logger.log("FileDownloader getFileInfo request failed url= $url")
                throw GetFileInfoFailed("get file info request failed!")
            }

            logger.log("FileDownloader getFileInfo request success url= $url")

            val fileSize =
                response.contentLength() ?: throw GetFileInfoFailed("contentLength not found")
            val isAcceptRange =
                response.headers[HttpHeaders.AcceptRanges]?.contains("bytes") == true &&
                        fileSize > ChunkedStrategy.FIVE_MB
            return FileInfo(
                fileSize = fileSize,
                acceptRange = isAcceptRange
            )
        }
    }
}