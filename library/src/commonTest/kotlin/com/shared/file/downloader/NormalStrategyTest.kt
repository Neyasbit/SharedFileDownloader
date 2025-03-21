package com.shared.file.downloader

import com.shared.file.downloader.file_downloader.SliceProgress
import com.shared.file.downloader.file_downloader.remote.NormalStrategy
import com.shared.file.downloader.file_downloader.utils.FileInfo
import com.shared.file.downloader.file_downloader.utils.FileInfo.Companion.getFileInfo
import com.shared.file.downloader.file_downloader.utils.FilesOperation
import com.shared.file.downloader.file_downloader.utils.HandleFileFailed
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okio.FileNotFoundException
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration

class NormalStrategyTest {

    private lateinit var client: HttpClient
    private lateinit var filesOperation: FilesOperation
    private lateinit var normalStrategy: NormalStrategy
    private lateinit var progressFlow: MutableSharedFlow<SliceProgress>
    private lateinit var fileInfo: FileInfo
    private val tempPath: Path = "/tmp/testfile".toPath()
    private val outputPath = "/output/path".toPath()
    private val mockEngine = MockEngine {
        respond(
            content = ByteArray(5000) { it.toByte() },
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentLength, "5000")
        )
    }
    private val fakeFileSystem = FakeFileSystem()
    private val testImageUrl = "https://i.imgur.com/xQ9UO95.jpeg"

    @BeforeTest
    fun setup() = runTest {
        client = HttpClient(mockEngine)
        fileInfo = getFileInfo(testImageUrl, Logger.SIMPLE, client)
        filesOperation = FilesOperation(Logger.SIMPLE, fakeFileSystem)
        fakeFileSystem.createDirectories(tempPath.parent!!)
        normalStrategy =
            NormalStrategy(
                client,
                filesOperation,
                Logger.SIMPLE,
                fakeFileSystem,
                fileInfo.fileSize,
                outputPath
            )

        progressFlow = MutableSharedFlow(
            extraBufferCapacity = 100,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    }

    @Test
    fun `test downloadFile emits first update`() = runTest(
        timeout = Duration.parse("5s")
    ) {
        val collectedProgress = mutableListOf<SliceProgress>()
        val job = launch {
            progressFlow.take(1).collect {
                println("Collected progress: $it")
                collectedProgress.add(it)
            }
        }
        normalStrategy.downloadFile(testImageUrl, tempPath, progressFlow)
        job.join()
        job.cancel()

        assertTrue(collectedProgress.isNotEmpty(), "Progress updates should be emitted")
    }

    @Test
    fun `test downloadFile completes successfully`() = runTest {
        val collectedProgress = mutableListOf<SliceProgress>()
        val job = launch {
            progressFlow
                .collect { sliceProgress ->
                    collectedProgress.add(sliceProgress)
                    if (sliceProgress.progress.downloaded == sliceProgress.progress.total)
                        cancel("Download completed")
                }
        }
        normalStrategy.downloadFile(testImageUrl, tempPath, progressFlow)
        job.join()
        assertEquals(
            fileInfo.fileSize,
            collectedProgress.last().progress.downloaded,
            "Final progress should match file size"
        )
    }

    @Test
    fun `test downloadFile skips download if file exists`() = runTest {

        fakeFileSystem.write(tempPath) {
            write(ByteArray(fileInfo.fileSize.toInt()) { it.toByte() })
        }

        val collectedProgress = mutableListOf<SliceProgress>()
        val job = launch {
            progressFlow.collect { progress ->
                println("Collected progress: $progress")
                collectedProgress.add(progress)
            }
        }

        normalStrategy.downloadFile(testImageUrl, tempPath, progressFlow)

        job.cancel()

        assertTrue(collectedProgress.isEmpty(), "Progress updates should not be emitted")
    }

    @Test
    fun `test saveFile moves file correctly`() = runTest {
        fakeFileSystem.write(tempPath) {
            write(ByteArray(fileInfo.fileSize.toInt()) { it.toByte() })
        }
        fakeFileSystem.createDirectories(outputPath.parent!!, false)
        normalStrategy.saveFile(testImageUrl, tempPath)

        assertTrue(fakeFileSystem.exists(outputPath), "File should be moved to output path")
        assertFalse(fakeFileSystem.exists(tempPath), "Temp file should not exist after move")
    }

    @Test
    fun `test saveFile handles missing temp file`() = runTest {
        fakeFileSystem.delete(tempPath, mustExist = false)

        assertFailsWith<HandleFileFailed> {
            normalStrategy.saveFile(testImageUrl, tempPath)
        }
    }

    @Test
    fun `test downloadFile handles temp file creation error`() = runTest {
        fakeFileSystem.delete(tempPath.parent!!, mustExist = false)

        assertFailsWith<FileNotFoundException> {
            normalStrategy.downloadFile(testImageUrl, tempPath, progressFlow)
        }
    }
}
