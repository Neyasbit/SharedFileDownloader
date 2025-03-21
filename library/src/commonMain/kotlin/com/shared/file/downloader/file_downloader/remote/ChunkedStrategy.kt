package com.shared.file.downloader.file_downloader.remote

import com.shared.file.downloader.file_downloader.Progress
import com.shared.file.downloader.file_downloader.SliceProgress
import com.shared.file.downloader.file_downloader.utils.DependenciesHolder
import com.shared.file.downloader.file_downloader.utils.FileInfo
import com.shared.file.downloader.file_downloader.utils.FilesOperation
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import okio.FileSystem
import okio.Path
import okio.use
import kotlin.coroutines.coroutineContext
import kotlin.math.ceil
import kotlin.math.min

internal class ChunkedStrategy(
    private val client: HttpClient,
    private val filesOperation: FilesOperation,
    private val logger: Logger,
    private val fileSystem: FileSystem,
    private val targetFileInfo: FileInfo,
    private val outputPath: Path
) : Strategy {

    private val threadCount = calculateThreads(fileSize = targetFileInfo.fileSize)

    override suspend fun downloadFile(
        url: String,
        tempPath: Path,
        progressFlow: MutableSharedFlow<SliceProgress>
    ): Unit = coroutineScope {
        logger.log("FileDownloader start download with ChunkedStrategy")
        // If the target file supports range requests, download it in multiple threads
        val chunkSize = ceil(targetFileInfo.fileSize.toDouble() / threadCount).toLong()
        logger.log("FileDownloader chunkSize = $chunkSize")
        logger.log("FileDownloader threadCount = $threadCount")
        val downloads = (0 until threadCount).map { threadIndex ->

            async {
                val start = chunkSize * threadIndex
                val end = min(start + chunkSize - 1, targetFileInfo.fileSize - 1)

                val tempFilePath = tempPath.resolve("$threadIndex.part")

                downloadFilePart(
                    url,
                    start = start,
                    end = end,
                    tempPath = tempFilePath,
                    progressFlow = progressFlow,
                    index = threadIndex
                )
            }
        }
        // Wait for all parts to be downloaded
        downloads.awaitAll()

    }

    override fun saveFile(url: String, tempPath: Path) {
        // Merge the parts to a single
        filesOperation.mergeFileParts(
            tempPath,
            outputPath,
            threadCount
        )
    }


    // Download a part of a file in a single thread
    internal suspend fun downloadFilePart(
        url: String,
        tempPath: Path,
        progressFlow: MutableSharedFlow<SliceProgress>,
        start: Long,
        end: Long,
        index: Int
    ) {
        var startLocation = start
        val existSize = filesOperation.checkTempFileSize(tempPath)

        logger.log("FileDownloader existSize=$existSize")

        // Adjust the start location if part of the file has already been downloaded
        if (existSize in 1 until (end - start + 1)) {
            startLocation += existSize
        } else {
            fileSystem.delete(tempPath, mustExist = false)
        }
        logger.log(
            "FileDownloader downloadFilePart startLocation = $startLocation, existSize = $existSize, url= $url \n"
        )
        val request = client.prepareGet(url) {
            onDownload { bytesSentTotal, contentLength ->
                val progress = SliceProgress(
                    url,
                    Progress(
                        (bytesSentTotal + existSize),
                        ((contentLength ?: 0) + existSize)
                    ),
                    sliceIndex = index
                )
                progressFlow.tryEmit(progress)
            }

            headers.append(
                name = HttpHeaders.Range,
                value = "bytes=${startLocation}-${end}"
            )
        }

        val fileHandle = fileSystem.openReadWrite(
            tempPath,
            mustCreate = false,
            mustExist = false
        )

        fileHandle.use { handle ->

            val writeStartPosition = startLocation - start
            logger.log("FileDownloader writing at position = $writeStartPosition for range [$start-$end]")

            request.execute { response ->
                coroutineContext.ensureActive()
                val bytes = ByteArray(DEFAULT_CHUNK_SIZE)
                var bytesRead: Int
                val channel: ByteReadChannel = response.bodyAsChannel()

                var offset = writeStartPosition
                while (channel.readAvailable(bytes)
                        .also { bytesRead = it } != -1
                ) {
                    coroutineContext.ensureActive()
                    handle.write(offset, bytes, 0, bytesRead)
                    offset += bytesRead
                }
            }
        }
    }

    // Calculate the number of threads to use for downloading
    internal fun calculateThreads(fileSize: Long): Int {
        return when {
            fileSize < FIVE_MB -> 1
            fileSize < FIFTY_MB -> min(2, MAX_THREAD_COUNT)
            fileSize < ONE_HUNDRED_MB -> min(3, MAX_THREAD_COUNT)
            else -> MAX_THREAD_COUNT
        }
    }

    internal companion object : Factory {
        private const val MAX_THREAD_COUNT = 5
        const val DEFAULT_CHUNK_SIZE = 8192
        const val FIVE_MB = 5 * 1024 * 1024
        private const val FIFTY_MB = 50 * 1024 * 1024
        private const val ONE_HUNDRED_MB = 100 * 1024 * 1024

        override fun create(
            dependenciesHolder: DependenciesHolder,
            fileInfo: FileInfo,
            outputPath: Path
        ): Strategy {
            return ChunkedStrategy(
                client = dependenciesHolder.client,
                filesOperation = dependenciesHolder.filesOperation,
                logger = dependenciesHolder.localLogger,
                fileSystem = dependenciesHolder.fileSystem,
                targetFileInfo = fileInfo,
                outputPath = outputPath
            )
        }
    }
}