package com.rumosoft.librarydogapi

/**
 * Sealed class representing different types of errors that can occur when using the Dog API.
 * This provides better error handling compared to generic exceptions.
 */
public sealed class DogApiError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * Network-related errors (connection issues, timeouts, etc.)
     */
    public class NetworkError(
        message: String,
        cause: Throwable? = null,
    ) : DogApiError(message, cause)

    /**
     * HTTP errors (4xx, 5xx status codes)
     */
    public class HttpError(
        public val statusCode: Int,
        message: String,
    ) : DogApiError(message)

    /**
     * Serialization/deserialization errors
     */
    public class SerializationError(
        message: String,
        cause: Throwable? = null,
    ) : DogApiError(message, cause)

    /**
     * Invalid breed or sub-breed name
     */
    public class InvalidBreedError(
        public val breedName: String,
        message: String = "Invalid breed name: $breedName",
    ) : DogApiError(message)

    /**
     * Unknown or unexpected errors
     */
    public class UnknownError(
        message: String,
        cause: Throwable? = null,
    ) : DogApiError(message, cause)
}
