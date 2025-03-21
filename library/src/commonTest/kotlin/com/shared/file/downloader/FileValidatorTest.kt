package com.shared.file.downloader

import com.shared.file.downloader.file_downloader.FILESYSTEM
import com.shared.file.downloader.file_downloader.utils.Md5FileValidator
import com.shared.file.downloader.file_downloader.utils.SHA256FileValidator
import com.shared.file.downloader.file_downloader.utils.byteStringToBase64
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val fileContent = "Hello, World!"
private val tempPath = "/tmp/testfile".toPath()

class Md5FileValidatorTest {
    //Не заню почему здесб не работает в тестах FakeFileSystem
    private val fakeFileSystem = FILESYSTEM

    // Ожидаемый MD5 сгенерированный хэш для строки "Hello, World!"
    private val expectedMd5Hash = "65a8e27d8879283831b664bd8b7f0ad4"

    @BeforeTest
    fun setup() = runTest {
        fakeFileSystem.createDirectories(tempPath.parent!!)
        // Создаём временный файл с известным содержимым
        fakeFileSystem.write(tempPath) {
            write(fileContent.toByteArray(Charsets.UTF_8))
        }

    }

    @Test
    fun `test Md5FileValidator validates file correctly`() {

        val validator = Md5FileValidator(fakeFileSystem, expectedMd5Hash)

        // Проверяем, что файл валиден
        assertTrue(validator.validateFileWithDigest(tempPath, false), "File should be valid")

        // Проверяем, что файл не валиден с неправильным хэшем
        val invalidValidator = Md5FileValidator(fakeFileSystem, "wronghash")
        assertFalse(invalidValidator.validateFileWithDigest(tempPath), "File should be invalid")
    }

    @Test
    fun `test Md5FileValidator validates file correctly with base64`() {

        val validator = Md5FileValidator(fakeFileSystem, expectedMd5Hash.byteStringToBase64())

        assertTrue(validator.validateFileWithDigest(tempPath), "File should be valid")
        assertFalse(validator.validateFileWithDigest(tempPath, false), "File should be valid")
    }
}

class SHA256FileValidatorTest {

    @Test
    fun `test SHA256FileValidator validates file correctly`() {
        val fakeFileSystem = FILESYSTEM

        val fileContent = "Hello, World!"
        val filePath = "/tmp/testfile".toPath()

        fakeFileSystem.createDirectories(filePath.parent!!)
        fakeFileSystem.write(filePath) {
            write(fileContent.toByteArray())
        }

        // Ожидаемый SHA-256 хэш для строки "Hello, World!"
        val expectedSha256Hash = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f"

        val validator =
            SHA256FileValidator(fileSystem = fakeFileSystem, expect = expectedSha256Hash)

        assertTrue(validator.validateFileWithDigest(filePath, false), "File should be valid")

        val invalidValidator = SHA256FileValidator(fakeFileSystem, "wronghash")
        assertFalse(invalidValidator.validateFileWithDigest(filePath), "File should be invalid")

    }
}


class FileValidatorTest {

    @Test
    fun `test FileValidator returns false for non-existent file`() {
        // Создаём валидатор
        val validator = Md5FileValidator(FILESYSTEM, "somehash")

        // Проверяем, что файл не существует
        val nonExistentFilePath = "/tmp/nonexistentfile".toPath()
        assertFalse(validator.validateFileWithDigest(nonExistentFilePath), "File should not exist")
    }
}