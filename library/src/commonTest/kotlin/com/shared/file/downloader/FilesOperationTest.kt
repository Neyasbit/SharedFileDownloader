package com.shared.file.downloader

import com.shared.file.downloader.file_downloader.utils.FilesOperation
import com.shared.file.downloader.file_downloader.utils.HandleFileFailed
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.SIMPLE
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FilesOperationTest {

    private val fakeFileSystem = FakeFileSystem()
    private val filesOperation = FilesOperation(Logger.SIMPLE, fakeFileSystem)
    private val tempFilePath = "/temp/testfile".toPath()

    @BeforeTest
    fun setup() {
        fakeFileSystem.createDirectories(tempFilePath.parent!!)
    }

    @Test
    fun `test checkTempFileSize returns correct size`() = runTest {

        // Создаём временный файл
        fakeFileSystem.write(tempFilePath) {
            write(ByteArray(100) { it.toByte() })
        }

        // Проверяем размер файла
        assertEquals(100, filesOperation.checkTempFileSize(tempFilePath), "File size should be 100")

        // Удаляем файл и проверяем, что размер равен 0
        fakeFileSystem.delete(tempFilePath)
        assertEquals(
            0,
            filesOperation.checkTempFileSize(tempFilePath),
            "File size should be 0 after deletion"
        )
    }

    @Test
    fun `test createUrlFolder creates folder if not exists`() = runTest {
        val outputPath = "/output".toPath()
        fakeFileSystem.createDirectories(outputPath.parent!!)
        val url = "https://example.com/file"

        // Создаём папку
        val resultPath = filesOperation.createUrlFolder(outputPath, url)

        // Проверяем, что папка создана
        assertTrue(fakeFileSystem.exists(resultPath), "Folder should be created")

        // Проверяем, что метод возвращает тот же путь, если папка уже существует
        val sameResultPath = filesOperation.createUrlFolder(outputPath, url)
        assertEquals(
            resultPath,
            sameResultPath,
            "Same path should be returned if folder already exists"
        )
    }

    @Test
    fun `test mergeFileParts merges files and deletes parts`() = runTest {
        val tempPath = "/temp".toPath()
        val outputPath = "/output/mergedfile".toPath()
        fakeFileSystem.createDirectories(outputPath.parent!!)
        fakeFileSystem.createDirectories(tempPath.parent!!)


        // Создаём части файлов
        fakeFileSystem.write(tempPath.resolve("0.part")) {
            write(ByteArray(50) { it.toByte() })
        }
        fakeFileSystem.write(tempPath.resolve("1.part")) {
            write(ByteArray(50) { it.toByte() })
        }

        // Объединяем части
        filesOperation.mergeFileParts(tempPath, outputPath, threadCount = 2)

        // Проверяем, что файл был создан
        assertTrue(fakeFileSystem.exists(outputPath), "Merged file should exist")

        // Проверяем размер объединённого файла
        assertEquals(
            100,
            fakeFileSystem.metadata(outputPath).size,
            "Merged file size should be 100"
        )

        // Проверяем, что части были удалены
        assertFalse(
            fakeFileSystem.exists(tempPath.resolve("0.part")),
            "Part file 0 should be deleted"
        )
        assertFalse(
            fakeFileSystem.exists(tempPath.resolve("1.part")),
            "Part file 1 should be deleted"
        )
    }

    @Test
    fun `test safeMoveFile moves file correctly`() = runTest {
        val tempPath = "/temp/testfile".toPath()
        val outputPath = "/output/testfile".toPath()
        fakeFileSystem.createDirectories(outputPath.parent!!)
        fakeFileSystem.createDirectories(tempPath.parent!!)
        // Создаём временный файл
        fakeFileSystem.write(tempPath) {
            write(ByteArray(100) { it.toByte() })
        }

        // Перемещаем файл
        filesOperation.safeMoveFile("https://example.com/file", tempPath, outputPath)

        // Проверяем, что файл был перемещён
        assertTrue(fakeFileSystem.exists(outputPath), "File should be moved to output path")
        assertFalse(fakeFileSystem.exists(tempPath), "Temp file should not exist after move")
    }

    @Test
    fun `test safeMoveFile throws exception if temp file does not exist`() = runTest {
        val tempPath = "/temp/nonexistent".toPath()
        val outputPath = "/output/testfile".toPath()
        fakeFileSystem.createDirectories(outputPath.parent!!)
        fakeFileSystem.createDirectories(tempPath.parent!!)

        // Ожидаем, что метод выбросит исключение
        assertFailsWith<HandleFileFailed> {
            filesOperation.safeMoveFile("https://example.com/file", tempPath, outputPath)
        }
    }

    @Test
    fun `test mergeFileParts handles missing part files`() = runTest {
        val tempPath = "/temp".toPath()
        val outputPath = "/output/mergedfile".toPath()
        fakeFileSystem.createDirectories(outputPath.parent!!)
        fakeFileSystem.createDirectories(tempPath.parent!!)

        // Создаём только одну часть файла
        fakeFileSystem.write(tempPath.resolve("0.part")) {
            write(ByteArray(50) { it.toByte() })
        }

        // Объединяем части (одна часть отсутствует)
        filesOperation.mergeFileParts(tempPath, outputPath, threadCount = 2)

        // Проверяем, что файл был создан
        assertTrue(fakeFileSystem.exists(outputPath), "Merged file should exist")

        // Проверяем размер объединённого файла
        assertEquals(50, fakeFileSystem.metadata(outputPath).size, "Merged file size should be 50")
    }

    @Test
    fun `test createUrlFolder handles missing parent directory`() = runTest {
        val outputPath = "/nonexistent/output".toPath()
        fakeFileSystem.createDirectories(outputPath.parent!!)
        val url = "https://example.com/file"

        // Убедимся, что родительский каталог не существует
        fakeFileSystem.delete(outputPath.parent!!, mustExist = false)

        // Создаём папку
        val resultPath = filesOperation.createUrlFolder(outputPath, url)

        // Проверяем, что папка создана
        assertTrue(fakeFileSystem.exists(resultPath), "Folder should be created")
    }
}