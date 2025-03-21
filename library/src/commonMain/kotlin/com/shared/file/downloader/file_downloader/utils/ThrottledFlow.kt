package com.shared.file.downloader.file_downloader.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

/**
 * Flow with the ability to throttle the emission of the values.
 *
 * This is useful when you want to limit the number of times a value is emitted in a certain amount of time.
 *
 * @param origin the flow that you want to throttle the emission of.
 * @param intervalMillis the interval of time that the flow will be throttled.
 *
 * */
internal class ThrottledFlow<T>(
    private val origin: MutableSharedFlow<T>,
    private val intervalMillis: Long
) : MutableSharedFlow<T> by origin {

    private var lastEmitTime: Long = 0L

    /**
     * Emits a value to the flow if the specified interval has passed since the last emission.
     *
     * @param value The value to be emitted.
     */
    override suspend fun emit(value: T) {
        if (canEmit()) {
            lastEmitTime = Clock.System.now().toEpochMilliseconds()
            origin.emit(value)
        }
    }

    /**
     * Attempts to emit a value to the flow if the specified interval has passed since the last emission.
     *
     * @param value The value to be emitted.
     * @return true if the value was emitted, false otherwise.
     */
    override fun tryEmit(value: T): Boolean {
        if (canEmit()) {
            lastEmitTime = Clock.System.now().toEpochMilliseconds()
            return origin.tryEmit(value)
        }
        return false
    }

    /**
     * Determines if a value can be emitted based on the interval since the last emission.
     *
     * @return true if a value can be emitted, false otherwise.
     */
    private fun canEmit(): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        return now - lastEmitTime >= intervalMillis
    }
}

/**
 * Extension function to throttle the first emission from a Flow within a specified interval.
 *
 * @param intervalMillis The interval of time that the flow will be throttled.
 * @return A new Flow that emits values at most once within the specified interval.
 */
internal fun <T> Flow<T>.throttledFirst(intervalMillis: Long): Flow<T> = flow {
    var lastEmissionTime: Long = 0L

    collect { value ->
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastEmissionTime >= intervalMillis) {
            lastEmissionTime = now
            emit(value)
        }
    }
}
