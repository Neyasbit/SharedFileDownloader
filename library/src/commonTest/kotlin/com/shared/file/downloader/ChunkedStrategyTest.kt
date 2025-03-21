package com.shared.file.downloader

import com.shared.file.downloader.file_downloader.SliceProgress
import com.shared.file.downloader.file_downloader.remote.ChunkedStrategy
import com.shared.file.downloader.file_downloader.utils.FileInfo
import com.shared.file.downloader.file_downloader.utils.FileInfo.Companion.getFileInfo
import com.shared.file.downloader.file_downloader.utils.FilesOperation
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChunkedStrategyTest {
    private lateinit var client: HttpClient
    private lateinit var filesOperation: FilesOperation
    private lateinit var chunkedStrategy: ChunkedStrategy
    private lateinit var progressFlow: MutableSharedFlow<SliceProgress>
    private lateinit var fileInfo: FileInfo
    private val tempPath: Path = "/tmp".toPath()
    private val outputPath = "/output/path".toPath()
    private val mockEngine = MockEngine {
        respond(
            content = ByteArray(6000000) { it.toByte() },
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentLength, "6000000")
        )
    }
    private val fakeFileSystem = FakeFileSystem()
    private val testImageUrl = "https://i.imgur.com/xQ9UO95.jpeg"

    @BeforeTest
    fun setup() = runTest {
        client = HttpClient(mockEngine)
        fileInfo = getFileInfo(testImageUrl, Logger.SIMPLE, client)
        filesOperation = FilesOperation(Logger.SIMPLE, fakeFileSystem)
        fakeFileSystem.createDirectories(tempPath)
        chunkedStrategy =
            ChunkedStrategy(
                client,
                filesOperation,
                Logger.SIMPLE,
                fakeFileSystem,
                fileInfo,
                outputPath
            )

        progressFlow = MutableSharedFlow(
            extraBufferCapacity = 100,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    }


    @Test
    fun `test calculateThreads`() {
        val strategy =
            ChunkedStrategy(
                client,
                filesOperation,
                Logger.SIMPLE,
                fakeFileSystem,
                fileInfo,
                outputPath
            )

        assertEquals(
            1,
            strategy.calculateThreads(4 * 1024 * 1024),
            "Thread count should be 1 for files < 5MB"
        )
        assertEquals(
            2,
            strategy.calculateThreads(10 * 1024 * 1024),
            "Thread count should be 2 for files < 50MB"
        )
        assertEquals(
            3,
            strategy.calculateThreads(60 * 1024 * 1024),
            "Thread count should be 3 for files < 100MB"
        )
        assertEquals(
            5,
            strategy.calculateThreads(200 * 1024 * 1024),
            "Thread count should be 5 for files >= 100MB"
        )
    }

    @Test
    fun `test downloadFilePart downloads file part and emits progress`() = runTest {
        val tempPath = "/temp/part0.part".toPath()
        fakeFileSystem.createDirectories(tempPath.parent!!)

        val collectedProgress = mutableListOf<SliceProgress>()

        val job = launch {
            progressFlow.collect { sliceProgress ->
                collectedProgress.add(sliceProgress)
                println("Progress: $sliceProgress")
                if (sliceProgress.progress.downloaded == sliceProgress.progress.total)
                    cancel("Download completed")
            }
        }

        val mockEngine = MockEngine {
            respond(
                content = ByteArray(100) { it.toByte() },
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentLength, "100")
            )
        }
        val client = HttpClient(mockEngine)

        val strategy =
            ChunkedStrategy(
                client,
                filesOperation,
                Logger.SIMPLE,
                fakeFileSystem,
                fileInfo,
                outputPath
            )


        strategy.downloadFilePart(
            url = "https://example.com/file",
            tempPath = tempPath,
            progressFlow = progressFlow,
            start = 0,
            end = 99,
            index = 0
        )

        job.join()

        assertTrue(collectedProgress.isNotEmpty(), "Progress updates should be emitted")
        assertEquals(
            100,
            collectedProgress.last().progress.downloaded,
            "Final progress should match file part size"
        )
        assertTrue(fakeFileSystem.exists(tempPath), "File part should exist")
    }

    @Test
    fun `test downloadFile downloads file in chunks and emits progress`() = runTest {
        val collectedProgress = mutableListOf<SliceProgress>()
        val job = launch {
            progressFlow.collect { progress ->
                collectedProgress.add(progress)
                println("Progress: $progress")
                if (progress.progress.downloaded == progress.progress.total)
                    cancel("Download completed")
            }
        }

        assertTrue(fakeFileSystem.exists(tempPath), "Parent directory should be created")
        chunkedStrategy.downloadFile(testImageUrl, tempPath, progressFlow)

        job.join()


        assertTrue(collectedProgress.isNotEmpty(), "Progress updates should be emitted")
        assertEquals(
            6000000,
            collectedProgress.last().progress.downloaded,
            "Final progress should match file size"
        )

        for (i in 0 until chunkedStrategy.calculateThreads(fileInfo.fileSize)) {
            println("File part $i should exist")
            assertTrue(
                fakeFileSystem.exists(tempPath.resolve("$i.part")),
                "File part $i should exist"
            )
        }
    }

    @Test
    fun `test saveFile merges parts and moves file`() = runTest {
        // Создаём части файла
        val fileSize = 10 * 1024 * 1024
        for (i in 0 until 2) {
            fakeFileSystem.write(tempPath.resolve("$i.part")) {
                write(ByteArray(fileSize / 2) { it.toByte() })
            }
        }

        // Объединяем части и перемещаем файл
        fakeFileSystem.createDirectories(outputPath.parent!!)
        chunkedStrategy.saveFile(testImageUrl, tempPath)

        // Проверяем, что файл был создан
        assertTrue(fakeFileSystem.exists(outputPath), "Output file should exist")

        // Проверяем размер файла
        assertEquals(
            fileSize.toLong(),
            fakeFileSystem.metadata(outputPath).size,
            "Output file size should be $fileSize"
        )

        // Проверяем, что части были удалены
        for (i in 0 until 2) {
            assertFalse(
                fakeFileSystem.exists(tempPath.resolve("$i.part")),
                "File part $i should be deleted"
            )
        }
    }
}