package com.shared.file.downloader.file_downloader

import com.shared.file.downloader.file_downloader.utils.DependenciesHolder
import com.shared.file.downloader.file_downloader.utils.FileInfo.Companion.getFileInfo
import com.shared.file.downloader.file_downloader.utils.FileValidator
import com.shared.file.downloader.file_downloader.utils.ProgressStateHolder
import com.shared.file.downloader.file_downloader.utils.StrategyFabric
import com.shared.file.downloader.file_downloader.utils.ThrottledFlow
import com.shared.file.downloader.file_downloader.utils.ValidateFileFailed
import io.ktor.util.collections.ConcurrentMap
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.DIRECTORY_SEPARATOR
import okio.Path.Companion.toPath

interface FileDownloader {
    /**
     * Download a file from a specified [url] to a file with the specified [fileName].
     *
     * @param url the url of the file to download
     * @param fileName the name of the file to download, the file will be saved in the temporary directory
     * @return a flow that emits the progress of the download
     */
    fun downloadFile(url: String, fileName: String): Flow<DownloadResult>
}

fun createFileDownloader(): FileDownloader = RealFileDownloader()

// Progress of a slice
internal data class SliceProgress(
    val url: String, val progress: Progress, val sliceIndex: Int = SINGLE_INDEX
) {
    private companion object {
        const val SINGLE_INDEX = 0
    }
}


internal class RealFileDownloader(
    private val dependenciesHolder: DependenciesHolder = DependenciesHolder(),
    private val strategyFabric: StrategyFabric = StrategyFabric(dependenciesHolder),
    private val dispatcherIO: CoroutineDispatcher = Dispatchers.IO,
    private val validator: FileValidator? = null
) : FileDownloader {
    private val logger = dependenciesHolder.localLogger
    private val filesOperation = dependenciesHolder.filesOperation

    // Download a file and report progress
    @OptIn(DelicateCoroutinesApi::class)
    override fun downloadFile(
        url: String, fileName: String
    ): Flow<DownloadResult> = callbackFlow {
        val outputPath =
            (FileSystem.SYSTEM_TEMPORARY_DIRECTORY.toString() + DIRECTORY_SEPARATOR + fileName).toPath()
        // Exception handler to catch and handle exceptions
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            logger.log("FileDownloader coroutineExceptionHandler caught exception $exception ")
        }

        // Launch the download in the background
        launch(dispatcherIO + exceptionHandler) {
            try {
                logger.log("FileDownloader start download url = $url")
                // Get the target file info. If fileInfo is null, fetch it from the URL
                val targetFileInfo =
                    getFileInfo(url = url, logger = logger, client = dependenciesHolder.client)
                val tempPath = filesOperation.createUrlFolder(outputPath, url)

                // Progress map to keep track of progress for each slice
                val progressMap = ConcurrentMap<Int, Progress>()
                val progressStateHolder = ProgressStateHolder(progressMap, targetFileInfo)

                logger.log("FileDownloader tempPath = $tempPath ")
                val simpleFlow = MutableSharedFlow<SliceProgress>(
                    extraBufferCapacity = 100, onBufferOverflow = BufferOverflow.DROP_OLDEST
                )

                val throttledFlow = ThrottledFlow(simpleFlow, 100)

                // Update progress
                fun updateProgress(
                    url: String, progress: Progress
                ) {
                    logger.log(
                        "FileDownloader download progress update " + "downloaded = ${progress.downloaded}," + " total = ${progress.total}, url= $url"
                    )

                    trySend(DownloadResult.ProgressUpdate(progress, url))
                }

                // Collect from the flow to update progress
                launch {
                    throttledFlow.collect { sliceProgress ->
                        with(progressStateHolder) {
                            put(sliceProgress.sliceIndex, sliceProgress.progress)
                            updateProgress(sliceProgress.url, get())
                        }
                    }
                }

                logger.log("FileDownloader start download url = $url")

                with(strategyFabric.create(targetFileInfo, outputPath)) {
                    downloadFile(
                        url = url, tempPath = tempPath, progressFlow = throttledFlow
                    )

                    logger.log("FileDownloader downloaded to temp directory start moving url = $url")

                    saveFile(url = url, tempPath = tempPath)
                }

                // Verify downloaded file if validator is provided
                validator?.apply {
                    if (!validateFileWithDigest(outputPath)) send(
                        DownloadResult.DownloadFailed(
                            ValidateFileFailed("FileDownloader validate file failed")
                        )
                    )
                }

                logger.log("FileDownloader download success url= $url")

                // If no exception occurred, send DownloadCompleted event
                if (!isClosedForSend)
                    send(DownloadResult.DownloadCompleted(filePath = outputPath.toString(), url))

            } catch (exception: Exception) {

                logger.log("FileDownloader caught exception = $exception url= $url")

                // If any exception occurred, send DownloadFailed event
                if (exception !is CancellationException) trySend(
                    DownloadResult.DownloadFailed(
                        exception, url
                    )
                )

            } finally {
                // Close the channel when done
                channel.close()
            }
        }

        // Wait for the download to complete
        awaitClose {
            logger.log("FileDownloader awaitClose")
        }
    }
}
