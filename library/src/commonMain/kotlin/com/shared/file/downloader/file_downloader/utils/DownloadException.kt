package com.shared.file.downloader.file_downloader.utils

internal class GetFileInfoFailed(message: String) : Exception(message)

internal class DownloadRequestFailed(message: String, internalException: Exception) :
    Exception(message)

internal class HandleFileFailed(message: String, internalException: Exception) : Exception(message)

internal class ValidateFileFailed(message: String) : Exception(message)