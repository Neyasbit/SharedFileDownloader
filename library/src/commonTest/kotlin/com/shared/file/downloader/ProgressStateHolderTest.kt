package com.shared.file.downloader

import com.shared.file.downloader.file_downloader.Progress
import com.shared.file.downloader.file_downloader.utils.FileInfo
import com.shared.file.downloader.file_downloader.utils.ProgressStateHolder
import io.ktor.util.collections.ConcurrentMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/*
Test Cases for ProgressStateHolder

1. Test put() Method:
   - Scenario: Add progress for a new client.
   - Input: clientIndex = 1, progress = Progress(downloaded = 50, total = 100)
   - Expected: progressMap should contain the entry (1 -> Progress(downloaded = 50, total = 100))

2. Test get() Method:
   - Scenario: Calculate total progress when progressMap has multiple entries.
   - Setup: progressMap contains {(1, Progress(downloaded = 50)), (2, Progress(downloaded = 30))}
   - Expected: get() should return Progress(downloaded = 80, total = fileInfo.fileSize)

3. Test get() Method with Empty Map:
   - Scenario: Calculate progress when progressMap is empty.
   - Expected: get() should return Progress(downloaded = 0, total = fileInfo.fileSize)
*/

class ProgressStateHolderTest {

    @Test
    fun `put should add progress for a new client`() {
        val progressStateHolder = ProgressStateHolder(ConcurrentMap(), FileInfo(fileSize = 100))
        progressStateHolder.put(1, Progress(downloaded = 50, total = 100))
        assertEquals(50, progressStateHolder.get().downloaded)
    }

    @Test
    fun `get should calculate total progress`() {
        val progressStateHolder = ProgressStateHolder(ConcurrentMap(), FileInfo(fileSize = 100))
        progressStateHolder.put(1, Progress(downloaded = 50, total = 100))
        assertEquals(50, progressStateHolder.get().downloaded)
        assertEquals(0.5f, progressStateHolder.get().value)
        progressStateHolder.put(2, Progress(downloaded = 30, total = 100))
        assertEquals(80, progressStateHolder.get().downloaded)
        assertEquals(0.8f, progressStateHolder.get().value)
    }

    @Test
    fun `get should return 0 when progressMap is empty`() {
        val progressStateHolder = ProgressStateHolder(ConcurrentMap(), FileInfo(fileSize = 12))
        assertTrue(progressStateHolder.get().downloaded.toInt() == 0)
        assertEquals(0, progressStateHolder.get().downloaded)
    }
}