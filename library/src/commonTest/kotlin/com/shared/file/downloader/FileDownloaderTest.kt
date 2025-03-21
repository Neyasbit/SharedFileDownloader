package com.shared.file.downloader

import com.shared.file.downloader.file_downloader.DownloadResult
import com.shared.file.downloader.file_downloader.RealFileDownloader
import com.shared.file.downloader.file_downloader.utils.DependenciesHolder
import com.shared.file.downloader.file_downloader.utils.StrategyFabric
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileDownloaderTest {

    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())
    private val fakeFileSystem = FakeFileSystem()

    @Test
    fun `test downloadFile completes successfully`() = runTest(
        context = testDispatcher
    ) {
        val fileSize = 100000
        val mockEngine = MockEngine {
            respond(
                content = ByteArray(fileSize) { it.toByte() },
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentLength, fileSize.toString())
            )
        }
        val client = HttpClient(mockEngine)
        val dependenciesHolder = DependenciesHolder(client, fakeFileSystem, Logger.SIMPLE)
        val fabric = StrategyFabric(dependenciesHolder)
        val fileDownloader =
            RealFileDownloader(dependenciesHolder, fabric, dispatcherIO = testDispatcher)

        val fileName = "testfile"
        val url = "https://example.com/file"

        val result = fileDownloader.downloadFile(url, fileName).toList()

        val successResult =
            result.filterIsInstance<DownloadResult.DownloadCompleted>().first()
        assertEquals(fileName, successResult.filePath.toPath().name, "File name should be the same")

        val file = fakeFileSystem.exists(successResult.filePath.toPath())
        assertTrue(file, "File should exist")
        // Проверяем размер файла
        assertEquals(
            fileSize.toLong(),
            fakeFileSystem.metadata(successResult.filePath.toPath()).size
        )
        // Проверяем, что последнее событие — DownloadCompleted
        assertTrue(
            result.last() is DownloadResult.DownloadCompleted,
            "Download should complete successfully"
        )
    }

    @Test
    fun `test downloadFile the big file completes successfully`() = runTest(
        context = testDispatcher
    ) {
        val fileSize = 10 * 1024 * 1024
        val mockEngine = MockEngine { request ->
            val rangeHeader = request.headers[HttpHeaders.Range]

            val fullContent = ByteArray(fileSize) { it.toByte() }

            if (rangeHeader != null) {
                val match = Regex("bytes=(\\d+)-(\\d+)?").find(rangeHeader)
                val (start, end) = match?.destructured ?: error("Invalid range header")

                val startByte = start.toInt()
                val endByte = end.toIntOrNull() ?: (fileSize - 1)

                val chunk = fullContent.copyOfRange(startByte, endByte + 1)

                respond(
                    content = chunk,
                    status = HttpStatusCode.PartialContent,
                    headers = headers {
                        append(HttpHeaders.ContentLength, chunk.size.toString())
                        append(HttpHeaders.ContentRange, "bytes $startByte-$endByte/$fileSize")
                        append(HttpHeaders.AcceptRanges, "bytes")
                    }
                )
            } else {
                respond(
                    content = fullContent,
                    status = HttpStatusCode.OK,
                    headers = headers {
                        append(HttpHeaders.ContentLength, fileSize.toString())
                        append(HttpHeaders.AcceptRanges, "bytes")
                    }
                )
            }
        }

        val client = HttpClient(mockEngine)
        val dependenciesHolder = DependenciesHolder(client, fakeFileSystem, Logger.SIMPLE)
        val fabric = StrategyFabric(dependenciesHolder)
        val fileDownloader =
            RealFileDownloader(dependenciesHolder, fabric, dispatcherIO = testDispatcher)

        val fileName = "testfile"
        val url = "https://example.com/file"

        val result = fileDownloader.downloadFile(url, fileName).toList()
        println("Result: $result")

        val successResult =
            result.filterIsInstance<DownloadResult.DownloadCompleted>().first()
        val file = fakeFileSystem.exists(successResult.filePath.toPath())
        assertTrue(file, "File should exist")
        assertEquals(
            fileSize.toLong(),
            fakeFileSystem.metadata(successResult.filePath.toPath()).size,
            "File size should be $fileSize"
        )

        // Проверяем, что последнее событие — DownloadCompleted
        assertTrue(
            result.last() is DownloadResult.DownloadCompleted,
            "Download should complete successfully"
        )
    }

    @Test
    fun `test downloadFile handles cancellation`() = runTest(
        context = testDispatcher
    ) {
        val mockEngine = MockEngine {
            respond(
                content = ByteArray(100000000) { it.toByte() },
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentLength, "100000000")
            )
        }
        val client = HttpClient(mockEngine)
        val dependenciesHolder = DependenciesHolder(client, fakeFileSystem, Logger.SIMPLE)
        // Внимательно проверяем, что Dispatcher.Default используется, иначе походу deadlock
        val fileDownloader = RealFileDownloader(
            dependenciesHolder,
            StrategyFabric(dependenciesHolder),
            dispatcherIO = Dispatchers.Default
        )

        val fileName = "testfile"
        val url = "https://example.com/file"

        val job = launch {
            fileDownloader.downloadFile(url, fileName).collect {
                println("Download result: $it")
                if (it is DownloadResult.ProgressUpdate) {

                    cancel("Download cancelled")
                }
            }
        }

        job.join()

        assertTrue(job.isCancelled, "Download should be cancelled")
    }
}