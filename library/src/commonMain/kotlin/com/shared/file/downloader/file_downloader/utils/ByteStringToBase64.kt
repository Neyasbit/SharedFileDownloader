package com.shared.file.downloader.file_downloader.utils

import io.ktor.util.encodeBase64
import okio.ByteString.Companion.encodeUtf8

internal fun String.byteStringToBase64(): String {
    val byteArray = this.chunked(2).map { it.toInt(radix = 16).toByte() }.toByteArray()
    return byteArray.encodeBase64()
}

internal fun String.md5(): String {
    return this.encodeUtf8().md5().hex()
}