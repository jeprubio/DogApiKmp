package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.Breed
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class DogApiTest {

    // --- happy paths: one per endpoint, each a distinct URL and response shape ---

    @Test
    fun `breeds returns the decoded breeds`() = test {
        dogApiMock.givenSuccess()

        val breeds = sut.breeds()

        breeds.shouldNotBeEmpty()
        breeds.first().name shouldBe "breed"
        breeds.first().subBreeds shouldContainExactly listOf("subBreed")
    }

    @Test
    fun `breedImages returns the image list`() = test {
        dogApiMock.givenSuccess()

        sut.breedImages("pug").first() shouldBe "breedImage1"
    }

    @Test
    fun `randomImage returns the image URL`() = test {
        dogApiMock.givenSuccess()

        sut.randomImage().shouldNotBeEmpty()
    }

    @Test
    fun `randomImage for breed returns the image URL`() = test {
        dogApiMock.givenSuccess()

        sut.randomImage("pug").shouldNotBeEmpty()
    }

    @Test
    fun `listSubBreeds returns the sub-breed list`() = test {
        dogApiMock.givenSuccess()

        sut.listSubBreeds("hound").first() shouldBe "subBreed1"
    }

    @Test
    fun `subBreedImages returns the image list`() = test {
        dogApiMock.givenSuccess()

        sut.subBreedImages("hound", "afghan").shouldNotBeEmpty()
    }

    // --- error mapping: all endpoints share one mapper, so one test per error type ---

    @Test
    fun `server error throws HttpError with status code`() = test {
        dogApiMock.givenFailure()

        shouldThrow<DogApiError.HttpError> { sut.breeds() }.statusCode shouldBe 500
    }

    @Test
    fun `404 for a breed throws InvalidBreedError`() = runTest {
        val api = DogApi(notFoundClient())

        shouldThrow<DogApiError.InvalidBreedError> {
            api.breedImages(UNKNOWN_BREED)
        }.breedName shouldBe UNKNOWN_BREED
    }

    @Test
    fun `an invalid breed name is rejected before any request`() = runTest {
        shouldThrow<DogApiError.InvalidBreedError> { DogApi.createDefault().breedImages("") }
    }

    @Test
    fun `network timeout throws NetworkError`() = runTest {
        val api = DogApi(httpClient(MockEngine { throw ConnectTimeoutException("timed out") }))

        shouldThrow<DogApiError.NetworkError> { api.breeds() }
    }

    @Test
    fun `malformed json throws SerializationError`() = runTest {
        val api = DogApi(httpClient(MockEngine { respondJson("{ not valid json") }))

        shouldThrow<DogApiError.SerializationError> { api.breeds() }
    }

    @Test
    fun `an error status in the body throws RemoteApiError`() = runTest {
        val api = DogApi(
            httpClient(MockEngine { respondJson("""{"message":"Breed not found","status":"error"}""") })
        )

        val error = shouldThrow<DogApiError.RemoteApiError> { api.breeds() }

        error.status shouldBe "error"
        error.apiMessage shouldBe "Breed not found"
    }

    @Test
    fun `an unexpected exception throws UnknownError`() = runTest {
        val api = DogApi(httpClient(MockEngine { throw IllegalStateException("boom") }))

        shouldThrow<DogApiError.UnknownError> { api.breeds() }
    }

    // --- logging ---

    @Test
    fun `invalid breed is logged at debug level not error level`() = runTest {
        val logger = CapturingLogger()
        val api = DogApi(notFoundClient(), logger = logger)

        shouldThrow<DogApiError.InvalidBreedError> { api.breedImages(UNKNOWN_BREED) }

        assertTrue(logger.errorMessages.isEmpty(), "should not be logged at error level")
        assertTrue(logger.debugMessages.any { it.contains(UNKNOWN_BREED) }, "should be logged at debug level")
    }

    @Test
    fun `server error is logged at error level`() = test {
        dogApiMock.givenFailure()

        shouldThrow<DogApiError> { sut.breeds() }

        assertTrue(logger.errorMessages.isNotEmpty())
    }

    // --- cancellation ---

    @Test
    fun `cancellation propagates instead of surfacing as a DogApiError`() = runTest {
        val requestStarted = CompletableDeferred<Unit>()
        val api = DogApi(neverRespondingClient(requestStarted))
        var reachedCodeAfterCall = false

        val job = launch {
            api.breeds()
            // Only reachable if cancellation was swallowed.
            reachedCodeAfterCall = true
        }
        requestStarted.await()
        job.cancel()
        job.join()

        assertFalse(
            reachedCodeAfterCall,
            "CancellationException must keep propagating, not be mapped to a DogApiError",
        )
    }

    @Test
    fun `cancellation is not logged as an error`() = runTest {
        val requestStarted = CompletableDeferred<Unit>()
        val logger = CapturingLogger()
        val api = DogApi(neverRespondingClient(requestStarted), logger = logger)

        val job = launch { api.breeds() }
        requestStarted.await()
        job.cancel()
        job.join()

        assertTrue(logger.errorMessages.isEmpty(), "was logged as: ${logger.errorMessages}")
    }

    // --- contract and configuration ---

    /** The documented way for Kotlin callers to get a `Result`. */
    @Test
    fun `runCatching turns the throwing API into a Result`() = test {
        dogApiMock.givenSuccess()

        val success: Result<List<Breed>> = runCatching { sut.breeds() }
        success.getOrNull().shouldNotBeNull().shouldNotBeEmpty()

        dogApiMock.givenFailure()

        val failure = runCatching { sut.breeds() }
        failure.shouldBeFailure()
        failure.exceptionOrNull().shouldBeInstanceOf<DogApiError.HttpError>()
    }

    /** Guards the defaults against drifting from the published constants. */
    @Test
    fun `DogApiConfig defaults match the documented constants`() {
        val config = DogApiConfig()

        config.baseUrl shouldBe DogApi.DEFAULT_BASE_URL
        config.logger shouldBe NoOpDogApiLogger
        config.connectTimeoutMillis shouldBe DogApi.DEFAULT_CONNECT_TIMEOUT_MS
        config.requestTimeoutMillis shouldBe DogApi.DEFAULT_REQUEST_TIMEOUT_MS
        config.socketTimeoutMillis shouldBe DogApi.DEFAULT_SOCKET_TIMEOUT_MS
        config.maxRetries shouldBe DogApi.DEFAULT_MAX_RETRIES
    }

    @Test
    fun `transient server error is retried then succeeds`() = runTest {
        var attempts = 0
        val client = HttpClient(
            MockEngine {
                attempts++
                if (attempts < 2) {
                    respond("", HttpStatusCode.InternalServerError, jsonHeaders)
                } else {
                    respondJson("""{"message":{"breed":["sub"]},"status":"success"}""")
                }
            }
        ) {
            expectSuccess = true
            install(ContentNegotiation) { json(DogJson) }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 2)
                delayMillis { 0L }
            }
        }

        DogApi(client).breeds().shouldNotBeEmpty()
        attempts shouldBe 2
    }

    @Test
    fun `breed and sub-breed names are lower-cased into the path`() = runTest {
        var requestedUrl: String? = null
        val client = httpClient(
            MockEngine { request ->
                requestedUrl = request.url.toString()
                respondJson("""{"message":[],"status":"success"}""")
            }
        )

        DogApi(client, baseUrl = "https://example.com/api").subBreedImages("Hound", "Afghan")

        requestedUrl shouldBe "https://example.com/api/breed/hound/afghan/images"
    }

    @Test
    fun `an invalid sub-breed name is rejected before any request`() = runTest {
        val error = shouldThrow<DogApiError.InvalidBreedError> {
            DogApi.createDefault().subBreedImages("hound", "af ghan")
        }

        error.message!! shouldContain "sub-breed"
    }

    @Test
    fun `trailing slash in base URL does not produce a double slash`() = runTest {
        var requestedUrl: String? = null
        val client = httpClient(
            MockEngine { request ->
                requestedUrl = request.url.toString()
                respondJson("""{"message":{},"status":"success"}""")
            }
        )

        DogApi(client, baseUrl = "https://example.com/api/").breeds()

        requestedUrl shouldBe "https://example.com/api/breeds/list/all"
    }

    private fun test(block: suspend ApiTestScope.() -> Unit) = runTest { ApiTestScope().block() }

    private fun notFoundClient(): HttpClient =
        httpClient(MockEngine { respond("", HttpStatusCode.NotFound, jsonHeaders) })

    /** Accepts the request then never responds, so cancellation is the only way out. */
    private fun neverRespondingClient(requestStarted: CompletableDeferred<Unit>): HttpClient =
        httpClient(
            MockEngine {
                requestStarted.complete(Unit)
                CompletableDeferred<Unit>().await() // never completes
                respondJson("""{"message":{},"status":"success"}""")
            }
        )

    private class ApiTestScope {
        val dogApiMock = DogApiMock()
        val logger = CapturingLogger()
        val sut: DogApiClient = DogApi(httpClient(dogApiMock.engine), logger = logger)
    }

    companion object {
        private const val UNKNOWN_BREED = "invalidbreed"

        fun httpClient(engine: MockEngine): HttpClient = HttpClient(engine) {
            expectSuccess = true // Make Ktor throw for non-2xx responses
            install(ContentNegotiation) { json(DogJson) }
        }
    }
}

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private fun MockRequestHandleScope.respondJson(content: String) =
    respond(content, HttpStatusCode.OK, jsonHeaders)

private class CapturingLogger : DogApiLogger {
    val debugMessages = mutableListOf<String>()
    val errorMessages = mutableListOf<String>()

    override fun d(msg: String) { debugMessages += msg }
    override fun e(msg: String, throwable: Throwable?) { errorMessages += msg }
}
