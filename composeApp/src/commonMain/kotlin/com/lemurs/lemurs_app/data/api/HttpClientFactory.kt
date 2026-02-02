package com.lemurs.lemurs_app.data.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * Expect declaration for platform-specific HTTP client engine.
 * Android uses OkHttp or CIO (both support TLS)
 * iOS uses Darwin engine (which supports TLS on native platforms)
 */
expect fun createPlatformHttpClient(config: HttpClientConfig<*>.() -> Unit = {}): HttpClient
