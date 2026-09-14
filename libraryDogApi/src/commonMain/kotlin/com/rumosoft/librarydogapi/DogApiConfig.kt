package com.rumosoft.librarydogapi

/**
 * HTTP settings for [DogApi.create]. Ktor is an implementation detail, so this is how callers
 * tune the client instead of supplying one.
 *
 * @param maxRetries Retries for transient failures (I/O errors and 5xx). GETs are idempotent so
 *   this is safe; 4xx is never retried. Use `0` to disable.
 */
public class DogApiConfig(
    public val baseUrl: String = DogApi.DEFAULT_BASE_URL,
    public val logger: DogApiLogger = NoOpDogApiLogger,
    public val connectTimeoutMillis: Long = DogApi.DEFAULT_CONNECT_TIMEOUT_MS,
    public val requestTimeoutMillis: Long = DogApi.DEFAULT_REQUEST_TIMEOUT_MS,
    public val socketTimeoutMillis: Long = DogApi.DEFAULT_SOCKET_TIMEOUT_MS,
    public val maxRetries: Int = DogApi.DEFAULT_MAX_RETRIES,
)
