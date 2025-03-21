package com.shared.file.downloader.file_downloader.utils

import io.ktor.client.plugins.logging.Logger
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

internal class FilesOperation(
    private val logger: Logger,
    private val fileSystem: FileSystem
) {

    // Check if the temp file exists and return its size
    fun checkTempFileSize(tempFilePath: Path): Long {
        return fileSystem.takeIf { it.exists(tempFilePath) }?.run {
            metadataOrNull(tempFilePath)?.size ?: 0
        } ?: 0
    }

    // Create a folder for the temp file
    fun createUrlFolder(outputPath: Path, url: String): Path {
        logger.log("FileDownloader createUrlFolder: outputPath = $outputPath, url = $url")
        val parentPath = outputPath.parent ?: outputPath
        val urlPath = url.toPath().name.md5()
        val resultPath = parentPath.resolve(urlPath)
        // If the folder does not exist, create it
        logger.log("FileDownloader resultPath = $resultPath")
        if (!fileSystem.exists(resultPath)) {
            logger.log("FileDownloader create directory")
            fileSystem.createDirectories(resultPath, false)
        }
        return resultPath
    }

    // Merge all parts into a single file
    fun mergeFileParts(tempPath: Path, outputPath: Path, threadCount: Int) {
        try {
            // Delete the output file if it already exists
            fileSystem.delete(path = outputPath, mustExist = false)

            // Open the output file
            fileSystem.sink(outputPath).buffer().use { output ->
                try {
                    for (index in 0 until threadCount) {
                        logger.log("FileDownloader mergeFileParts index = $index tempPath = $tempPath")
                        val tPath = tempPath.resolve("$index.part")
                        // Check if the part file exists; it might not exist if the file size is smaller than expected
                        if (fileSystem.exists(tPath)) {
                            // Read from the part file and write to the output file
                            fileSystem.source(tPath).buffer().use { input ->
                                val size = input.readAll(output)
                                logger.log("FileDownloader readAll from path = $tPath, size = $size")
                            }
                        }
                    }

                    // Delete all parts
                    fileSystem.deleteRecursively(tempPath, mustExist = false)
                } catch (e: Exception) {
                    throw HandleFileFailed(
                        "merge tempFile to output path failed exception = ${e.message}",
                        e
                    )
                }
            }
        } catch (e: Exception) {
            throw HandleFileFailed(
                "merge tempFile to output path failed exception = ${e.message}",
                e
            )
        }
    }

    // Move temp file to final location
    fun safeMoveFile(url: String, tempPath: Path, outputPath: Path) {
        try {
            logger.log("FileDownloader safeMoveFile url = $url, tempPath = $tempPath, outputPath = $outputPath")
            fileSystem.atomicMove(tempPath, outputPath)
        } catch (e: Exception) {
            throw HandleFileFailed(
                "move tempFile to output path failed exception = ${e.message}",
                e
            )
        }
    }
}