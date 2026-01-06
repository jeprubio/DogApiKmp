package com.rumosoft.librarydogapi

/**
 * Sealed class representing different types of errors that can occur when using the Dog API.
 * This provides better error handling compared to generic exceptions.
 */
public sealed class DogApiError : Exception() {

    /**
     * Network-related errors (connection issues, timeouts, etc.)
     */
    public data class NetworkError(
        override val message: String,
        override val cause: Throwable? = null
    ) : DogApiError()

    /**
     * HTTP errors (4xx, 5xx status codes)
     */
    public data class HttpError(
        val statusCode: Int,
        override val message: String
    ) : DogApiError()

    /**
     * Serialization/deserialization errors
     */
    public data class SerializationError(
        override val message: String,
        override val cause: Throwable? = null
    ) : DogApiError()

    /**
     * Invalid breed or sub-breed name
     */
    public data class InvalidBreedError(
        val breedName: String,
        override val message: String = "Invalid breed name: $breedName"
    ) : DogApiError()

    /**
     * Unknown or unexpected errors
     */
    public data class UnknownError(
        override val message: String,
        override val cause: Throwable? = null
    ) : DogApiError()
}

