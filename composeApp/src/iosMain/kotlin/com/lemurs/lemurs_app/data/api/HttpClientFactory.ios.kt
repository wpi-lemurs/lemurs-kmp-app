package com.lemurs.lemurs_app.data.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

/**
 * iOS implementation uses Darwin engine which supports TLS on native platforms.
 * CIO engine does NOT support TLS on Kotlin/Native (iOS).
 */
actual fun createPlatformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient {
    return HttpClient(Darwin) {
        config()
    }
}
