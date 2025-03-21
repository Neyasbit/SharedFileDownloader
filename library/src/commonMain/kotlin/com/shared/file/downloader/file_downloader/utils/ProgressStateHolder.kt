package com.shared.file.downloader.file_downloader.utils

import com.shared.file.downloader.file_downloader.Progress
import io.ktor.util.collections.ConcurrentMap

/**
 * This class is used to hold the progress of each client
 * and provide the overall progress of all clients.
 *
 * @property progressMap the map of progress for each client.
 * @property fileInfo the file info.
 */
internal class ProgressStateHolder(
    private val progressMap: ConcurrentMap<Int, Progress>,
    private val fileInfo: FileInfo
) {

    fun put(clientIndex: Int, progress: Progress) {
        progressMap[clientIndex] = progress
    }

    fun get(): Progress {
        val totalDownloaded = progressMap.values.sumOf(Progress::downloaded)
        val totalContentLength = fileInfo.fileSize
        return Progress(
            totalDownloaded,
            totalContentLength
        )
    }
}