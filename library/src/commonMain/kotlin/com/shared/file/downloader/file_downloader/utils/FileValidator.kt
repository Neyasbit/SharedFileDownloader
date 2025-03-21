package com.shared.file.downloader.file_downloader.utils

import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import kotlin.random.Random

/**
 * Abstract class for file validation with hashing capability.
 *
 * @property fileSystem the file system used for file operations.
 * @property expect the expected hash value for validation.
 */
internal abstract class FileValidator(
    private val fileSystem: FileSystem,
    private val expect: String
) {

    /**
     * Hashes a file at the specified output path using a hashing sink.
     *
     * @param outputPath the path of the file to hash.
     * @return the hash value as a string.
     */
    abstract fun hashFileWithHashSink(outputPath: Path): String

    /**
     * Validates a file by comparing its hash (optionally Base64 encoded) with the expected hash.
     *
     * @param filePath the path of the file to validate.
     * @param needBase64 whether the hash should be Base64 encoded.
     * @return true if the file's hash matches the expected hash, false otherwise.
     */
    fun validateFileWithDigest(filePath: Path, needBase64: Boolean = true): Boolean {
        if (!fileSystem.exists(filePath)) return false
        val hash = hashFileWithHashSink(filePath).lowercase()
        if (!needBase64) {
            return hash == expect.lowercase()
        }
        val base64 = hash.byteStringToBase64().lowercase()
        return base64 == expect.lowercase()
    }

    /**
     * Creates a temporary file under the same parent directory as the specified output path.
     *
     * @param outputPath the base path for creating the temporary file.
     * @return the path of the created temporary file.
     */
    protected fun createTempFile(outputPath: Path): Path {
        val parentPath = outputPath.parent ?: kotlin.run { outputPath }
        val temp = randomString(10)
        val resultPath = parentPath.resolve(temp)
        if (!fileSystem.exists(resultPath)) {
            val path = resultPath.toString() + "1"
            return path.toPath()
        }
        return resultPath
    }

    /**
     * Generates a random string of specified length using allowed characters.
     *
     * @param length the length of the generated string.
     * @param allowedChars the characters allowed in the generated string.
     * @return the generated random string.
     */
    private fun randomString(
        length: Int,
        allowedChars: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    ): String {
        return (1..length)
            .map { allowedChars.random(Random.Default) }
            .joinToString("")
    }
}

/**
 * Implementation of FileValidator that uses MD5 hashing for validation.
 *
 * @property expect the expected MD5 hash value.
 */
internal class Md5FileValidator(
    private val fileSystem: FileSystem,
    private val expect: String
) : FileValidator(fileSystem, expect) {

    /**
     * Hashes a file using MD5 and returns the hash as a string.
     *
     * @param outputPath the path of the file to hash.
     * @return the MD5 hash as a string.
     */
    override fun hashFileWithHashSink(outputPath: Path): String {
        lateinit var md5HashingSink: HashingSink
        val tempFilePath = createTempFile(outputPath)
        fileSystem.source(outputPath).use { source ->
            md5HashingSink = HashingSink.md5(fileSystem.sink(tempFilePath))
            source.buffer().use { bufferedSource ->
                bufferedSource.readAll(md5HashingSink)
            }
        }
        val digest = md5HashingSink.hash.hex()
        fileSystem.delete(tempFilePath, false)
        return digest
    }
}

/**
 * Implementation of FileValidator that uses SHA-256 hashing for validation.
 *
 * @property expect the expected SHA-256 hash value.
 */
internal class SHA256FileValidator(
    private val fileSystem: FileSystem,
    private val expect: String
) : FileValidator(fileSystem, expect) {

    /**
     * Hashes a file using SHA-256 and returns the hash as a string.
     *
     * @param outputPath the path of the file to hash.
     * @return the SHA-256 hash as a string.
     */
    override fun hashFileWithHashSink(outputPath: Path): String {
        lateinit var sha256HashingSink: HashingSink
        val tempFilePath = createTempFile(outputPath)
        fileSystem.source(outputPath).use { source ->
            sha256HashingSink = HashingSink.sha256(fileSystem.sink(tempFilePath))
            source.buffer().use { bufferedSource ->
                bufferedSource.readAll(sha256HashingSink)
            }
        }
        val digest = sha256HashingSink.hash.hex()
        fileSystem.delete(tempFilePath, false)
        return digest
    }
}
