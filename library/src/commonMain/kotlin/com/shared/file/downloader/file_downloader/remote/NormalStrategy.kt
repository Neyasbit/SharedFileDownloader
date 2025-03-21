package com.shared.file.downloader.file_downloader.remote

import com.shared.file.downloader.file_downloader.Progress
import com.shared.file.downloader.file_downloader.SliceProgress
import com.shared.file.downloader.file_downloader.remote.ChunkedStrategy.Companion.DEFAULT_CHUNK_SIZE
import com.shared.file.downloader.file_downloader.utils.DependenciesHolder
import com.shared.file.downloader.file_downloader.utils.FileInfo
import com.shared.file.downloader.file_downloader.utils.FilesOperation
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.io.readByteArray
import okio.FileSystem
import okio.Path
import okio.use

internal class NormalStrategy(
    private val client: HttpClient,
    private val filesOperation: FilesOperation,
    private val logger: Logger,
    private val fileSystem: FileSystem,
    private val fileSize: Long,
    private val outputPath: Path
) : Strategy {

    // Download a file in a single thread
    override suspend fun downloadFile(
        url: String,
        tempPath: Path,
        progressFlow: MutableSharedFlow<SliceProgress>
    ) {
        logger.log("FileDownloader start download with NormalStrategy")
        // Check if the temp file already exists and has correct size
        val existSize = filesOperation.checkTempFileSize(tempPath)
        val isNeedDownload = existSize != fileSize
        logger.log("FileDownloader isNeedDownload=$isNeedDownload, existSize=$existSize")
        if (isNeedDownload) {
            // If not, delete the temp file
            fileSystem.delete(tempPath, mustExist = false)
            logger.log("FileDownloader delete temp $tempPath file url= $url")

            fileSystem.openReadWrite(tempPath).use { handle ->
                var bytesReadTotal: Long = 0
                client.prepareGet(url)
                    .execute { httpResponse ->

                        val channel: ByteReadChannel = httpResponse.bodyAsChannel()

                        while (!channel.isClosedForRead) {
                            val packet = channel.readRemaining(DEFAULT_CHUNK_SIZE.toLong())
                            val bytes = packet.readByteArray()
                            handle.write(bytesReadTotal, bytes, 0, bytes.size)
                            bytesReadTotal += bytes.size

                            val progress = SliceProgress(
                                url,
                                Progress(bytesReadTotal, fileSize)
                            )
                            progressFlow.tryEmit(progress)
                        }
                    }

            }
        }
    }

    override fun saveFile(url: String, tempPath: Path) {
        // Move the temp file to the output path
        filesOperation.safeMoveFile(
            url,
            tempPath,
            outputPath
        )
    }

    companion object : Factory {

        override fun create(
            dependenciesHolder: DependenciesHolder,
            fileInfo: FileInfo,
            outputPath: Path
        ): Strategy {
            return NormalStrategy(
                dependenciesHolder.client,
                dependenciesHolder.filesOperation,
                dependenciesHolder.localLogger,
                dependenciesHolder.fileSystem,
                fileInfo.fileSize,
                outputPath
            )
        }
    }
}