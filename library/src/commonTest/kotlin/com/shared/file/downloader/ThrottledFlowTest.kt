package com.shared.file.downloader

import com.shared.file.downloader.file_downloader.utils.ThrottledFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThrottledFlowTest {

    @Test
    fun testEmitWithinInterval() = runBlocking {
        val sharedFlow = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1)
        val throttledFlow = ThrottledFlow(sharedFlow, 1000L)

        assertTrue(throttledFlow.tryEmit(1))
        assertFalse(throttledFlow.tryEmit(2))
        delay(1000L)
        assertTrue(throttledFlow.tryEmit(3))
    }

    @Test
    fun testEmitAfterInterval() = runBlocking {
        val sharedFlow = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1)
        val throttledFlow = ThrottledFlow(sharedFlow, 500L)

        assertTrue(throttledFlow.tryEmit(4))
        delay(600L)
        assertTrue(throttledFlow.tryEmit(5))
    }

    @Test
    fun testEmitRespectsInterval() = runBlocking {
        val sharedFlow = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1)
        val throttledFlow = ThrottledFlow(sharedFlow, 200L)

        throttledFlow.emit(6)
        assertFalse(throttledFlow.tryEmit(7))
        delay(200L)
        assertTrue(throttledFlow.tryEmit(8))
    }
}