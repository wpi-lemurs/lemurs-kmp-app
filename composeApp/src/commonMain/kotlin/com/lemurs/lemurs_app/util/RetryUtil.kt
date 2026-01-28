package com.lemurs.lemurs_app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

suspend fun <T> retryIO(
    apiCall: suspend () -> T,
    maxRetries: Int = 3,
    initialDelay: Long = 100,
    factor: Double = 2.0,
    shouldRetry: (Exception) -> Boolean = { true }
): T {
    var lastException: Exception? = null
    var currentDelay = initialDelay
    repeat(maxRetries) { attempt ->
        try {
            return apiCall()
        } catch (e: Exception) {
            // Check if the exception should be retried
            if (!shouldRetry(e)) {
                throw e
            }

            lastException = e
            currentDelay = (currentDelay * factor).toLong()
            withContext(Dispatchers.IO) { kotlinx.coroutines.delay(currentDelay) }
        }
    }

    throw lastException ?: Exception("Unknown error occurred")
}
