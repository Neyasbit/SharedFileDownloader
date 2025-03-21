package com.shared.file.downloader.file_downloader.remote

import com.shared.file.downloader.file_downloader.SliceProgress
import com.shared.file.downloader.file_downloader.utils.DependenciesHolder
import com.shared.file.downloader.file_downloader.utils.FileInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import okio.Path

/**
 * Interface for strategies to download files to a temporary location.
 *
 * Implementations of this interface should handle downloading a file from a URL to a specified
 * temporary path, while optionally reporting progress updates through a shared flow.
 */
internal interface DownloadToTempStrategy {

    //Todo maybe return progress flow
    suspend fun downloadFile(
        url: String,
        tempPath: Path,
        progressFlow: MutableSharedFlow<SliceProgress>
    )
}

/**
 * Interface for strategies to save the file from a temporary location to a destination.
 *
 * Implementations of this interface should handle saving a file from a temporary location to a
 * destination location, potentially with some additional logic.
 */
internal interface SaverStrategy {

    fun saveFile(
        url: String,
        tempPath: Path
    )
}


internal interface Strategy : DownloadToTempStrategy, SaverStrategy

internal interface Factory {

    fun create(
        dependenciesHolder: DependenciesHolder,
        fileInfo: FileInfo,
        outputPath: Path
    ): Strategy
}