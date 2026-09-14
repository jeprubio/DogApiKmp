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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Default implementation of the Dog API client.
 *
 * This class provides access to the Dog CEO API (https://dog.ceo/dog-api/).
 * It implements DogApiClient for better testability and dependency injection.
 *
 * Use [createDefault], or [create] with a [DogApiConfig] for custom timeouts and retries.
 *
 * The constructor is internal on purpose: taking a Ktor `HttpClient` exported the whole Ktor
 * type graph into the iOS framework.
 */
public class DogApi internal constructor(
    private val client: HttpClient,
    baseUrl: String = DEFAULT_BASE_URL,
    private val logger: DogApiLogger = NoOpDogApiLogger,
) : DogApiClient {

    /** Trailing slashes removed so path concatenation never doubles up. */
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
         * @param baseUrl Override the base URL (useful for testing against a local server).
         * @param logger Defaults to [NoOpDogApiLogger] (silent).
         */
        public fun createDefault(
            baseUrl: String = DEFAULT_BASE_URL,
            logger: DogApiLogger = NoOpDogApiLogger,
        ): DogApiClient = DogApi(sharedClient, baseUrl, logger)

        /**
         * Creates an instance with its own HttpClient. Like the shared one it lives for the rest
         * of the process, so create it once rather than per call.
         */
        public fun create(config: DogApiConfig = DogApiConfig()): DogApiClient =
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
    override suspend fun randomImage(breed: String): String = forBreed(breed) {
        logger.d("Fetching random image for breed '$breed'")
        getAndLog<RandomImageResult>("$baseUrl/breed/${breed.toPathSegment()}/images/random").message
    }

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun breedImages(breed: String): List<String> = forBreed(breed) {
        logger.d("Fetching all images for breed '$breed'")
        getAndLog<BreedImagesResult>("$baseUrl/breed/${breed.toPathSegment()}/images").message
    }

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun subBreedImages(breed: String, subBreed: String): List<String> =
        forBreed(breed, subBreed) {
            logger.d("Fetching images for sub-breed '$breed/$subBreed'")
            val url = "$baseUrl/breed/${breed.toPathSegment()}/${subBreed.toPathSegment()}/images"
            getAndLog<BreedImagesResult>(url).message
        }

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun listSubBreeds(breed: String): List<String> = forBreed(breed) {
        logger.d("Fetching sub-breeds for '$breed'")
        getAndLog<SubBreedsResult>("$baseUrl/breed/${breed.toPathSegment()}/list").message
    }

    /** Validates the names, then runs [block] with 404 mapped to an invalid breed. */
    private suspend inline fun <T> forBreed(
        breed: String,
        subBreed: String? = null,
        block: suspend () -> T,
    ): T {
        val invalid = BreedNameValidator.validate(breed)
            ?: subBreed?.let { BreedNameValidator.validate(it, "sub-breed") }
        invalid?.let { throw it }
        return runApiCall(breedName = breed, logger = logger, block = block)
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

    /**
     * On failure the API puts a string in `message` where the payload goes, so this must run
     * before decoding the body, or the error surfaces as a parse failure.
     */
    private fun validateStatus(element: JsonElement) {
        val body = element as? JsonObject ?: return
        val status = (body["status"] as? JsonPrimitive)?.contentOrNull
        if (status == null || status == "success") return

        val message = body["message"]
        val apiMessage = (message as? JsonPrimitive)?.contentOrNull ?: message?.toString()
        throw DogApiError.RemoteApiError(status = status, apiMessage = apiMessage)
    }
}

/**
 * Converts failures into a typed [DogApiError] before rethrowing.
 *
 * [CancellationException] is rethrown untouched and unlogged: mapping it would let a cancelled
 * coroutine carry on as if the call had merely failed.
 *
 * @param breedName When non-null, 404 maps to [DogApiError.InvalidBreedError], logged at debug.
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
