package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.Breed
import com.rumosoft.librarydogapi.models.BreedImagesResult
import com.rumosoft.librarydogapi.models.BreedsResult
import com.rumosoft.librarydogapi.models.SubBreedsResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
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
    private val baseUrl: String = DEFAULT_BASE_URL
) : DogApiClient {

    public companion object {
        public const val DEFAULT_BASE_URL: String = "https://dog.ceo/api"
        internal const val HTTP_NOT_FOUND: Int = 404

        /**
         * Shared HttpClient instance used by createDefault().
         * This client is reused across all default DogApi instances for efficiency.
         */
        private val sharedClient: HttpClient by lazy {
            HttpClient {
                expectSuccess = true  // Throw exceptions for non-2xx responses
                install(ContentNegotiation) {
                    json()
                }
            }
        }

        /**
         * Creates a DogApi instance with default configuration using a shared HttpClient.
         * This is the simplest way to get started.
         */
        public fun createDefault(baseUrl: String = DEFAULT_BASE_URL): DogApi {
            return DogApi(sharedClient, baseUrl)
        }
    }

    override suspend fun breeds(): Result<List<Breed>> = safeApiCall {
        val response: HttpResponse = client.get("$baseUrl/breeds/list/all")
        response.body<BreedsResult>().message.map { (breed, subBreeds) ->
            Breed(name = breed, subBreeds = subBreeds)
        }
    }

    override suspend fun randomImage(): Result<String> = safeApiCall {
        client.get("$baseUrl/breeds/image/random").body()
    }

    override suspend fun randomImage(breed: String): Result<String> = safeApiCall(breed) {
        client.get("$baseUrl/breed/${breed.lowercase()}/images/random").body()
    }

    override suspend fun breedImages(breed: String): Result<List<String>> = safeApiCall(breed) {
        client.get("$baseUrl/breed/${breed.lowercase()}/images")
            .body<BreedImagesResult>().message
    }

    override suspend fun subBreedImages(breed: String, subBreed: String): Result<List<String>> = safeApiCall(breed) {
        client.get("$baseUrl/breed/${breed.lowercase()}/${subBreed.lowercase()}/images")
            .body<BreedImagesResult>().message
    }

    override suspend fun listSubBreeds(breed: String): Result<List<String>> = safeApiCall(breed) {
        client.get("$baseUrl/breed/${breed.lowercase()}/list")
            .body<SubBreedsResult>().message
    }
}

/**
 * Wraps API calls with error handling, converting exceptions to typed DogApiError instances.
 * Returns only Result.success or Result.failure, never throws.
 */
private suspend inline fun <T> safeApiCall(block: suspend () -> T): Result<T> {
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
            }
        }
}

/**
 * Wraps breed-specific API calls with error handling.
 * Converts 404 errors to InvalidBreedError for better semantic error handling.
 */
private suspend inline fun <T> safeApiCall(breedName: String, block: suspend () -> T): Result<T> {
    return runCatching { block() }
        .recoverCatching { exception ->
            throw when (exception) {
                is ClientRequestException -> {
                    if (exception.response.status.value == DogApi.HTTP_NOT_FOUND) {
                        DogApiError.InvalidBreedError(breedName, "Breed '$breedName' not found")
                    } else {
                        DogApiError.HttpError(exception.response.status.value, "Client error: ${exception.message}")
                    }
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
            }
        }
}

