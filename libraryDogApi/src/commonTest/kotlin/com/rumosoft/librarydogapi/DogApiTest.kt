package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.Breed
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
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

    @Test
    fun `breeds returns the decoded breeds`() = test {
        dogApiMock.givenSuccess()

        val breeds = sut.breeds()

        breeds.shouldNotBeEmpty()
        val breed = breeds.first()
        breed.name shouldBe "breed"
        breed.subBreeds shouldContainExactly listOf("subBreed")
    }

    @Test
    fun `breeds throws on server error`() = test {
        dogApiMock.givenFailure()

        shouldThrow<DogApiError> { sut.breeds() }
    }

    @Test
    fun `breedImages returns the image list`() = test {
        dogApiMock.givenSuccess()

        sut.breedImages("pug").first() shouldBe "breedImage1"
    }

    @Test
    fun `breedImages throws on server error`() = test {
        dogApiMock.givenFailure()

        shouldThrow<DogApiError> { sut.breedImages("pug") }
    }

    @Test
    fun `randomImage returns the image URL`() = test {
        dogApiMock.givenSuccess()

        sut.randomImage().shouldNotBeEmpty()
    }

    @Test
    fun `randomImage throws on server error`() = test {
        dogApiMock.givenFailure()

        shouldThrow<DogApiError> { sut.randomImage() }
    }

    @Test
    fun `randomImage for breed returns the image URL`() = test {
        dogApiMock.givenSuccess()

        sut.randomImage("pug").shouldNotBeEmpty()
    }

    @Test
    fun `randomImage for breed throws on server error`() = test {
        dogApiMock.givenFailure()

        shouldThrow<DogApiError> { sut.randomImage("pug") }
    }

    @Test
    fun `listSubBreeds returns the sub-breed list`() = test {
        dogApiMock.givenSuccess()

        val subBreeds = sut.listSubBreeds("hound")

        subBreeds.shouldNotBeEmpty()
        subBreeds.first() shouldBe "subBreed1"
    }

    @Test
    fun `listSubBreeds throws on server error`() = test {
        dogApiMock.givenFailure()

        shouldThrow<DogApiError> { sut.listSubBreeds("hound") }
    }

    @Test
    fun `subBreedImages returns the image list`() = test {
        dogApiMock.givenSuccess()

        sut.subBreedImages("hound", "afghan").shouldNotBeEmpty()
    }

    @Test
    fun `subBreedImages throws on server error`() = test {
        dogApiMock.givenFailure()

        shouldThrow<DogApiError> { sut.subBreedImages("hound", "afghan") }
    }

    @Test
    fun `server error throws HttpError with status code`() = test {
        dogApiMock.givenFailure()

        val error = shouldThrow<DogApiError.HttpError> { sut.breeds() }

        error.statusCode shouldBe HTTP_INTERNAL_SERVER_ERROR
    }

    @Test
    fun `invalid breed throws InvalidBreedError`() = runTest {
        val api = DogApi(notFoundClient())

        val error = shouldThrow<DogApiError.InvalidBreedError> { api.breedImages(INVALID_BREED) }

        error.breedName shouldBe INVALID_BREED
    }

    @Test
    fun `invalid breed is logged at debug level not error level`() = runTest {
        val logger = CapturingLogger()
        val api = DogApi(notFoundClient(), logger = logger)

        shouldThrow<DogApiError.InvalidBreedError> { api.breedImages(INVALID_BREED) }

        assertTrue(logger.errorMessages.isEmpty(), "InvalidBreedError should not be logged at error level")
        assertTrue(
            logger.debugMessages.any { it.contains(INVALID_BREED) },
            "InvalidBreedError should be logged at debug level",
        )
    }

    @Test
    fun `server error is logged at error level`() = test {
        dogApiMock.givenFailure()

        shouldThrow<DogApiError> { sut.breeds() }

        assertTrue(logger.errorMessages.isNotEmpty(), "Server error should be logged at error level")
    }

    @Test
    fun `invalid breed name is rejected by validation before any request`() = runTest {
        val api = DogApi.createDefault()

        shouldThrow<DogApiError.InvalidBreedError> { api.breedImages("") }
    }

    @Test
    fun `network timeout throws NetworkError`() = runTest {
        val api = DogApi(httpClient(MockEngine { throw ConnectTimeoutException("Connection timed out") }))

        shouldThrow<DogApiError.NetworkError> { api.breeds() }
    }

    @Test
    fun `malformed json throws SerializationError`() = runTest {
        val api = DogApi(
            httpClient(
                MockEngine { _ ->
                    respond(
                        content = "{ not valid json",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            )
        )

        shouldThrow<DogApiError.SerializationError> { api.breeds() }
    }

    @Test
    fun `breeds throws RemoteApiError when API status is error`() = runTest {
        assertRemoteApiError { apiReturningApiError().breeds() }
    }

    @Test
    fun `randomImage throws RemoteApiError when API status is error`() = runTest {
        assertRemoteApiError { apiReturningApiError().randomImage() }
    }

    @Test
    fun `breedImages throws RemoteApiError when API status is error`() = runTest {
        assertRemoteApiError { apiReturningApiError().breedImages("pug") }
    }

    @Test
    fun `listSubBreeds throws RemoteApiError when API status is error`() = runTest {
        assertRemoteApiError { apiReturningApiError().listSubBreeds("hound") }
    }

    @Test
    fun `unexpected exception throws UnknownError`() = runTest {
        val api = DogApi(httpClient(MockEngine { throw IllegalStateException("Unexpected test failure") }))

        shouldThrow<DogApiError.UnknownError> { api.breeds() }
    }

    /**
     * The public contract deliberately does not return `kotlin.Result`, because Kotlin/Native
     * erases inline value classes to `Any?` when exporting to Objective-C, which would strip
     * every type from the Swift API. Kotlin callers who want a `Result` wrap the call instead —
     * this test pins that documented migration path.
     */
    @Test
    fun `runCatching gives Kotlin callers a Result over the throwing API`() = test {
        dogApiMock.givenSuccess()

        val success: Result<List<Breed>> = runCatching { sut.breeds() }

        success.shouldBeSuccess()
        success.getOrNull().shouldNotBeNull().shouldNotBeEmpty()

        dogApiMock.givenFailure()

        val failure = runCatching { sut.breeds() }

        failure.shouldBeFailure()
        failure.exceptionOrNull().shouldBeInstanceOf<DogApiError.HttpError>()
    }

    @Test
    fun `cancellation propagates instead of surfacing as a DogApiError`() = runTest {
        val requestStarted = CompletableDeferred<Unit>()
        val api = DogApi(neverRespondingClient(requestStarted))
        var reachedCodeAfterCall = false

        val job = launch {
            // Called bare on purpose: wrapping this in runCatching would swallow the
            // CancellationException in the *test* and hide what we are asserting.
            api.breeds()
            // Only reachable if the cancellation was swallowed somewhere below, which would mean
            // the calling coroutine keeps running after it was cancelled.
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

    /**
     * `runCatching` is the documented way for Kotlin callers to get a `Result`, but it catches
     * `CancellationException` too. This test pins that caveat so the README keeps warning about
     * it: inside a cancellable scope, catch [DogApiError] explicitly instead.
     */
    @Test
    fun `runCatching around the API also catches cancellation`() = runTest {
        val requestStarted = CompletableDeferred<Unit>()
        val api = DogApi(neverRespondingClient(requestStarted))
        var swallowedCancellation = false

        val job = launch {
            val result = runCatching { api.breeds() }
            swallowedCancellation = result.isFailure
        }
        requestStarted.await()
        job.cancel()
        job.join()

        assertTrue(
            swallowedCancellation,
            "runCatching is expected to swallow cancellation; if this ever stops being true, " +
                "the README guidance about preferring try/catch can be relaxed",
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

        assertTrue(
            logger.errorMessages.isEmpty(),
            "Cancellation is not a failure and must not be logged at error level, " +
                "but was logged as: ${logger.errorMessages}",
        )
    }

    @Test
    fun `transient server error is retried then succeeds`() = runTest {
        var attempts = 0
        val client = HttpClient(
            MockEngine { _ ->
                attempts++
                if (attempts < 2) {
                    respond(
                        content = "",
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                } else {
                    respond(
                        content = """{"message":{"breed":["sub"]},"status":"success"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        ) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(DogJson)
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 2)
                delayMillis { 0L }
            }
        }
        val api = DogApi(client)

        api.breeds().shouldNotBeEmpty()
        attempts shouldBe 2
    }

    @Test
    fun `trailing slash in base URL does not produce a double slash`() = runTest {
        var requestedUrl: String? = null
        val client = httpClient(
            MockEngine { request ->
                requestedUrl = request.url.toString()
                respond(
                    content = """{"message":{},"status":"success"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        )
        val api = DogApi(client, baseUrl = "https://example.com/api/")

        api.breeds()

        requestedUrl shouldBe "https://example.com/api/breeds/list/all"
    }

    private fun test(block: suspend ApiTestScope.() -> Unit) = runTest { ApiTestScope().block() }

    private suspend fun assertRemoteApiError(block: suspend () -> Unit) {
        val error = shouldThrow<DogApiError.RemoteApiError> { block() }
        error.status shouldBe "error"
        error.apiMessage shouldBe "Breed not found"
    }

    /** A client that answers every request with a 404 and an empty body. */
    private fun notFoundClient(): HttpClient = httpClient(
        MockEngine { _ ->
            respond(
                content = "",
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
    )

    /**
     * A client whose engine accepts the request and then never responds, so the only way the
     * call can finish is by being cancelled.
     */
    private fun neverRespondingClient(requestStarted: CompletableDeferred<Unit>): HttpClient =
        httpClient(
            MockEngine { _ ->
                requestStarted.complete(Unit)
                CompletableDeferred<Unit>().await() // never completes; suspends until cancelled
                respond(
                    content = """{"message":{},"status":"success"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        )

    private fun apiReturningApiError(): DogApiClient {
        val client = httpClient(
            MockEngine { _ ->
                respond(
                    content = """{"message":"Breed not found","status":"error"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        )
        return DogApi(client)
    }

    private class ApiTestScope {
        val dogApiMock = DogApiMock()
        val logger = CapturingLogger()
        val httpClient = httpClient(dogApiMock.engine)
        val sut: DogApiClient = DogApi(httpClient, logger = logger)
    }

    companion object {
        private const val INVALID_BREED = "invalidbreed"
        private const val HTTP_INTERNAL_SERVER_ERROR = 500

        fun httpClient(engine: MockEngine): HttpClient = HttpClient(engine) {
            expectSuccess = true  // Make Ktor throw exceptions for non-2xx responses
            install(ContentNegotiation) {
                json(DogJson)
            }
        }
    }

}

private class CapturingLogger : DogApiLogger {
    val debugMessages = mutableListOf<String>()
    val errorMessages = mutableListOf<String>()

    override fun d(msg: String) { debugMessages += msg }
    override fun e(msg: String, throwable: Throwable?) { errorMessages += msg }
}
