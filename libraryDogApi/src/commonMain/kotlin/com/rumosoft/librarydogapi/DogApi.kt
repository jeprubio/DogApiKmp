package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.Breed
import com.rumosoft.librarydogapi.models.BreedImagesResult
import com.rumosoft.librarydogapi.models.BreedsResult
import com.rumosoft.librarydogapi.models.RandomImageResult
import com.rumosoft.librarydogapi.models.SubBreedsResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerializationException

/**
 * Default implementation of the Dog API client.
 *
 * This class provides access to the Dog CEO API (https://dog.ceo/dog-api/).
 * It implements DogApiClient for better testability and dependency injection.
 *
 * The default implementation uses a shared HttpClient for efficiency.
 * You can also provide your own HttpClient instance for custom configuration.
 *
 * Example usage:
 * ```
 * val api = DogApi.createDefault()
 * val breeds = api.breeds().getOrNull()
 * ```
 *
 * For custom HttpClient configuration:
 * ```
 * val customClient = HttpClient { /* your config */ }
 * val api = DogApi(customClient)
 * ```
 *
 * For iOS developers: Use the protocol DogApiClient for dependency injection
 * to make your code more testable.
 */
public class DogApi(
    private val client: HttpClient,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val logger: DogApiLogger = NoOpDogApiLogger,
) : DogApiClient {

    public companion object {
        public const val DEFAULT_BASE_URL: String = "https://dog.ceo/api"
        internal const val HTTP_NOT_FOUND: Int = 404

        public const val DEFAULT_CONNECT_TIMEOUT_MS: Long = 15_000L
        public const val DEFAULT_REQUEST_TIMEOUT_MS: Long = 30_000L
        public const val DEFAULT_SOCKET_TIMEOUT_MS: Long  = 15_000L

        /**
         * Shared HttpClient instance used by createDefault().
         * This client is reused across all default DogApi instances for efficiency.
         */
        private val sharedClient: HttpClient by lazy {
            HttpClient {
                expectSuccess = true  // Throw exceptions for non-2xx responses
                install(ContentNegotiation) {
                    json(kotlinx.serialization.json.Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    })
                }
                install(HttpTimeout) {
                    connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MS
                    requestTimeoutMillis = DEFAULT_REQUEST_TIMEOUT_MS
                    socketTimeoutMillis  = DEFAULT_SOCKET_TIMEOUT_MS
                }
            }
        }

        /**
         * Creates a DogApi instance with default configuration using a shared HttpClient.
         *
         * @param baseUrl Override the base URL (useful for testing against a local server).
         * @param logger Optional logger. Defaults to [NoOpDogApiLogger] (silent).
         *   Pass your own implementation to route logs to Logcat, Napier, etc.
         */
        public fun createDefault(
            baseUrl: String = DEFAULT_BASE_URL,
            logger: DogApiLogger = NoOpDogApiLogger,
        ): DogApi {
            return DogApi(sharedClient, baseUrl, logger)
        }
    }

    override suspend fun breeds(): Result<List<Breed>> = safeApiCall(logger = logger) {
        logger.d("Fetching all breeds")
        val url = "$baseUrl/breeds/list/all"
        getAndLog<BreedsResult>(url).message.map { (breed, subBreeds) ->
            Breed(name = breed, subBreeds = subBreeds)
        }
    }

    override suspend fun randomImage(): Result<String> = safeApiCall(logger = logger) {
        logger.d("Fetching random image")
        val url = "$baseUrl/breeds/image/random"
        getAndLog<RandomImageResult>(url).message
    }

    override suspend fun randomImage(breed: String): Result<String> {
        BreedNameValidator.validate(breed)?.let { return Result.failure(it) }
        return safeApiCall(breedName = breed, logger = logger) {
            logger.d("Fetching random image for breed '$breed'")
            val url = "$baseUrl/breed/${breed.lowercase()}/images/random"
            getAndLog<RandomImageResult>(url).message
        }
    }

    override suspend fun breedImages(breed: String): Result<List<String>> {
        BreedNameValidator.validate(breed)?.let { return Result.failure(it) }
        return safeApiCall(breedName = breed, logger = logger) {
            logger.d("Fetching all images for breed '$breed'")
            val url = "$baseUrl/breed/${breed.lowercase()}/images"
            getAndLog<BreedImagesResult>(url).message
        }
    }

    override suspend fun subBreedImages(breed: String, subBreed: String): Result<List<String>> {
        BreedNameValidator.validate(breed)?.let { return Result.failure(it) }
        BreedNameValidator.validate(subBreed, "sub-breed")?.let { return Result.failure(it) }
        return safeApiCall(breedName = breed, logger = logger) {
            logger.d("Fetching images for sub-breed '$breed/$subBreed'")
            val url = "$baseUrl/breed/${breed.lowercase()}/${subBreed.lowercase()}/images"
            getAndLog<BreedImagesResult>(url).message
        }
    }

    override suspend fun listSubBreeds(breed: String): Result<List<String>> {
        BreedNameValidator.validate(breed)?.let { return Result.failure(it) }
        return safeApiCall(breedName = breed, logger = logger) {
            logger.d("Fetching sub-breeds for '$breed'")
            val url = "$baseUrl/breed/${breed.lowercase()}/list"
            getAndLog<SubBreedsResult>(url).message
        }
    }

    /**
     * Helper to make HTTP GET request, log the request/response, and parse the body.
     */
    private suspend inline fun <reified T> getAndLog(url: String): T {
        val response: HttpResponse = client.get(url)
        logger.d("GET $url → ${response.status.value}")
        return response.body<T>()
    }
}

/**
 * Wraps API calls with error handling, converting exceptions to typed DogApiError instances.
 * Returns only Result.success or Result.failure, never throws.
 */
private suspend inline fun <T> safeApiCall(
    logger: DogApiLogger = NoOpDogApiLogger,
    block: suspend () -> T,
): Result<T> {
    return runCatching { block() }
        .recoverCatching { exception ->
            throw when (exception) {
                is ClientRequestException ->
                    DogApiError.HttpError(exception.response.status.value, "Client error: ${exception.message}")
                is ServerResponseException ->
                    DogApiError.HttpError(exception.response.status.value, "Server error: ${exception.message}")
                is ConnectTimeoutException ->
                    DogApiError.NetworkError("Connection timeout", exception)
                is SocketTimeoutException ->
                    DogApiError.NetworkError("Request timeout", exception)
                is SerializationException ->
                    DogApiError.SerializationError("Failed to parse response", exception)
                else ->
                    DogApiError.UnknownError("Request failed: ${exception.message}", exception)
            }.also { error -> logger.e(error.message ?: "Unknown error", exception) }
        }
}

/**
 * Wraps breed-specific API calls with error handling.
 * Converts 404 errors to InvalidBreedError for better semantic error handling.
 */
private suspend inline fun <T> safeApiCall(
    breedName: String,
    logger: DogApiLogger = NoOpDogApiLogger,
    block: suspend () -> T,
): Result<T> {
    return runCatching { block() }
        .recoverCatching { exception ->
            throw when (exception) {
                is ClientRequestException ->
                    if (exception.response.status.value == DogApi.HTTP_NOT_FOUND) {
                        DogApiError.InvalidBreedError(breedName, "Breed '$breedName' not found")
                    } else {
                        DogApiError.HttpError(exception.response.status.value, "Client error: ${exception.message}")
                    }
                is ServerResponseException ->
                    DogApiError.HttpError(exception.response.status.value, "Server error: ${exception.message}")
                is ConnectTimeoutException ->
                    DogApiError.NetworkError("Connection timeout", exception)
                is SocketTimeoutException ->
                    DogApiError.NetworkError("Request timeout", exception)
                is SerializationException ->
                    DogApiError.SerializationError("Failed to parse response", exception)
                else ->
                    DogApiError.UnknownError("Request failed: ${exception.message}", exception)
            }.also { error ->
                if (error is DogApiError.InvalidBreedError) {
                    logger.d(error.message)
                } else {
                    logger.e(error.message ?: "Unknown error", exception)
                }
            }
        }
}
