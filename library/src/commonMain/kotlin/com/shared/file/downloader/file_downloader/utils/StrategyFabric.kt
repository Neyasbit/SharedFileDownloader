package com.shared.file.downloader.file_downloader.utils

import com.shared.file.downloader.file_downloader.remote.ChunkedStrategy
import com.shared.file.downloader.file_downloader.remote.NormalStrategy
import com.shared.file.downloader.file_downloader.remote.Strategy
import okio.Path

internal class StrategyFabric(
    private val dependenciesHolder: DependenciesHolder
) {
    fun create(fileInfo: FileInfo, outputPath: Path): Strategy =
        if (fileInfo.acceptRange)
            ChunkedStrategy.create(dependenciesHolder, fileInfo, outputPath)
        else NormalStrategy.create(dependenciesHolder, fileInfo, outputPath)
}