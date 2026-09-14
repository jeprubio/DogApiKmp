package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.Breed
import com.rumosoft.librarydogapi.models.BreedImagesResult
import com.rumosoft.librarydogapi.models.BreedsResult
import com.rumosoft.librarydogapi.models.DogApiStatusResult
import com.rumosoft.librarydogapi.models.RandomImageResult
import com.rumosoft.librarydogapi.models.SubBreedsResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.encodeURLPathPart
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.kotlinx.json.json
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Default implementation of the Dog API client.
 *
 * This class provides access to the Dog CEO API (https://dog.ceo/dog-api/).
 * It implements DogApiClient for better testability and dependency injection.
 *
 * Use [createDefault] for the shared client, or [create] with a [DogApiConfig] for custom
 * timeouts and retries.
 *
 * ```kotlin
 * val api = DogApi.createDefault()
 * val breeds = api.breeds()
 * ```
 *
 * The constructor is internal on purpose: accepting a Ktor `HttpClient` exported the whole Ktor
 * type graph into the iOS framework. Use [DogApiConfig], or implement [DogApiClient] for
 * anything it cannot express.
 */
public class DogApi internal constructor(
    private val client: HttpClient,
    baseUrl: String = DEFAULT_BASE_URL,
    private val logger: DogApiLogger = NoOpDogApiLogger,
) : DogApiClient {

    /** Base URL with any trailing slashes removed so path concatenation never doubles up. */
    private val baseUrl: String = baseUrl.trimEnd('/')

    public companion object {
        public const val DEFAULT_BASE_URL: String = "https://dog.ceo/api"
        internal const val HTTP_NOT_FOUND: Int = 404

        public const val DEFAULT_CONNECT_TIMEOUT_MS: Long = 15_000L
        public const val DEFAULT_REQUEST_TIMEOUT_MS: Long = 30_000L
        public const val DEFAULT_SOCKET_TIMEOUT_MS: Long  = 15_000L

        /**
         * Number of retries applied by the shared client to transient failures
         * (I/O exceptions and 5xx server errors). All operations are idempotent
         * GET requests, so retrying is safe. 4xx responses are never retried.
         */
        public const val DEFAULT_MAX_RETRIES: Int = 2

        /** Reused across all [createDefault] instances, and never closed. */
        private val sharedClient: HttpClient by lazy { buildClient(DogApiConfig()) }

        /**
         * Creates an instance backed by the shared HttpClient.
         *
         * @param baseUrl Override the base URL (useful for testing against a local server).
         * @param logger Defaults to [NoOpDogApiLogger] (silent).
         */
        public fun createDefault(
            baseUrl: String = DEFAULT_BASE_URL,
            logger: DogApiLogger = NoOpDogApiLogger,
        ): DogApi = DogApi(sharedClient, baseUrl, logger)

        /**
         * Creates an instance with its own HttpClient configured by [config]. Like the shared
         * client, it lives for the rest of the process, so create it once rather than per call.
         */
        public fun create(config: DogApiConfig = DogApiConfig()): DogApi =
            DogApi(buildClient(config), config.baseUrl, config.logger)

        private fun buildClient(config: DogApiConfig): HttpClient = HttpClient {
            expectSuccess = true  // Throw exceptions for non-2xx responses
            install(ContentNegotiation) {
                json(DogJson)
            }
            install(HttpTimeout) {
                connectTimeoutMillis = config.connectTimeoutMillis
                requestTimeoutMillis = config.requestTimeoutMillis
                socketTimeoutMillis  = config.socketTimeoutMillis
            }
            if (config.maxRetries > 0) {
                install(HttpRequestRetry) {
                    retryOnExceptionOrServerErrors(maxRetries = config.maxRetries)
                    exponentialDelay()
                }
            }
        }
    }

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun breeds(): List<Breed> = runApiCall(logger = logger) {
        logger.d("Fetching all breeds")
        val url = "$baseUrl/breeds/list/all"
        getAndLog<BreedsResult>(url).message.map { (breed, subBreeds) ->
            Breed(name = breed, subBreeds = subBreeds)
        }
    }

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun randomImage(): String = runApiCall(logger = logger) {
        logger.d("Fetching random image")
        val url = "$baseUrl/breeds/image/random"
        getAndLog<RandomImageResult>(url).message
    }

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun randomImage(breed: String): String {
        BreedNameValidator.validate(breed)?.let { throw it }
        return runApiCall(breedName = breed, logger = logger) {
            logger.d("Fetching random image for breed '$breed'")
            val url = "$baseUrl/breed/${breed.toPathSegment()}/images/random"
            getAndLog<RandomImageResult>(url).message
        }
    }

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun breedImages(breed: String): List<String> {
        BreedNameValidator.validate(breed)?.let { throw it }
        return runApiCall(breedName = breed, logger = logger) {
            logger.d("Fetching all images for breed '$breed'")
            val url = "$baseUrl/breed/${breed.toPathSegment()}/images"
            getAndLog<BreedImagesResult>(url).message
        }
    }

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun subBreedImages(breed: String, subBreed: String): List<String> {
        val validationError = BreedNameValidator.validate(breed)
            ?: BreedNameValidator.validate(subBreed, "sub-breed")
        if (validationError != null) throw validationError

        return runApiCall(breedName = breed, logger = logger) {
            logger.d("Fetching images for sub-breed '$breed/$subBreed'")
            val url = "$baseUrl/breed/${breed.toPathSegment()}/${subBreed.toPathSegment()}/images"
            getAndLog<BreedImagesResult>(url).message
        }
    }

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun listSubBreeds(breed: String): List<String> {
        BreedNameValidator.validate(breed)?.let { throw it }
        return runApiCall(breedName = breed, logger = logger) {
            logger.d("Fetching sub-breeds for '$breed'")
            val url = "$baseUrl/breed/${breed.toPathSegment()}/list"
            getAndLog<SubBreedsResult>(url).message
        }
    }

    /**
     * Normalises a breed or sub-breed name into a safe URL path segment:
     * lower-cased and percent-encoded. Encoding is applied explicitly here so URL
     * safety does not rely on [BreedNameValidator]'s character rules.
     */
    private fun String.toPathSegment(): String = lowercase().encodeURLPathPart()

    /**
     * Helper to make HTTP GET request, log the request/response, and parse the body.
     */
    private suspend inline fun <reified T> getAndLog(url: String): T {
        val response: HttpResponse = client.get(url)
        logger.d("GET $url → ${response.status.value}")
        val element = response.body<JsonElement>()
        validateStatus(element)
        return DogJson.decodeFromJsonElement<T>(element)
    }

    private fun validateStatus(element: JsonElement) {
        val statusResult = DogJson.decodeFromJsonElement<DogApiStatusResult>(element)
        if (statusResult.status == "success") return

        val apiMessage = (statusResult.message as? JsonPrimitive)?.contentOrNull
            ?: statusResult.message?.toString()
        throw DogApiError.RemoteApiError(status = statusResult.status, apiMessage = apiMessage)
    }
}

/**
 * Converts any failure into a typed [DogApiError] before rethrowing, so every exception leaving
 * the public API is one the contract declares.
 *
 * [CancellationException] is rethrown untouched and unlogged: mapping it would break structured
 * concurrency, letting a cancelled coroutine carry on as if the call had merely failed.
 *
 * @param breedName When non-null, 404 responses map to [DogApiError.InvalidBreedError] and are
 *   logged at debug level; everything else is logged at error level.
 */
private suspend inline fun <T> runApiCall(
    breedName: String? = null,
    logger: DogApiLogger = NoOpDogApiLogger,
    block: suspend () -> T,
): T =
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        val error = throwable.toDogApiError(breedName)
        if (error is DogApiError.InvalidBreedError) {
            logger.d(error.message ?: "Unknown error")
        } else {
            logger.e(error.message ?: "Unknown error", throwable)
        }
        throw error
    }

/**
 * Maps a [Throwable] to a typed [DogApiError].
 *
 * @param breedName When non-null, a 404 [ClientRequestException] is mapped to
 *   [DogApiError.InvalidBreedError] instead of [DogApiError.HttpError].
 */
private fun Throwable.toDogApiError(breedName: String? = null): DogApiError = when (this) {
    is DogApiError ->
        this
    is ClientRequestException ->
        if (breedName != null && response.status.value == DogApi.HTTP_NOT_FOUND)
            DogApiError.InvalidBreedError(breedName, "Breed '$breedName' not found")
        else
            DogApiError.HttpError(response.status.value, "Client error: $message")
    is ServerResponseException ->
        DogApiError.HttpError(response.status.value, "Server error: $message")
    is ConnectTimeoutException ->
        DogApiError.NetworkError("Connection timeout", this)
    is SocketTimeoutException ->
        DogApiError.NetworkError("Request timeout", this)
    is JsonConvertException, is SerializationException ->
        DogApiError.SerializationError("Failed to parse response", this)
    else ->
        DogApiError.UnknownError("Request failed: $message", this)
}
