package com.lemurs.lemurs_app.data.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO

/**
 * Android implementation uses CIO engine which supports TLS on JVM/Android.
 */
actual fun createPlatformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient {
    return HttpClient(CIO) {
        config()
    }
}
