package com.shared.file.downloader.file_downloader.utils

import com.shared.file.downloader.file_downloader.FILESYSTEM
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.SIMPLE
import okio.FileSystem


internal class DependenciesHolder(
    val client: HttpClient = HttpClient(),
    val fileSystem: FileSystem = FILESYSTEM,
    val localLogger: Logger = Logger.SIMPLE,
    val filesOperation: FilesOperation = FilesOperation(localLogger, fileSystem)
)