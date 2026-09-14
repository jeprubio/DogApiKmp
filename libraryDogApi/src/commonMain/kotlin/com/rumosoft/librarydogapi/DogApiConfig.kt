package com.rumosoft.librarydogapi

/**
 * HTTP settings for [DogApi.create], since Ktor is not exposed.
 *
 * @param maxRetries Retries I/O errors and 5xx, never 4xx. Use `0` to disable.
 */
public class DogApiConfig(
    public val baseUrl: String = DogApi.DEFAULT_BASE_URL,
    public val logger: DogApiLogger = NoOpDogApiLogger,
    public val connectTimeoutMillis: Long = DogApi.DEFAULT_CONNECT_TIMEOUT_MS,
    public val requestTimeoutMillis: Long = DogApi.DEFAULT_REQUEST_TIMEOUT_MS,
    public val socketTimeoutMillis: Long = DogApi.DEFAULT_SOCKET_TIMEOUT_MS,
    public val maxRetries: Int = DogApi.DEFAULT_MAX_RETRIES,
)
