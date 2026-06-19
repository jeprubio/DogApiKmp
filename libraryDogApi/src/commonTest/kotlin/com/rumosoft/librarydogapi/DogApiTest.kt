package com.rumosoft.librarydogapi

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
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
import kotlinx.coroutines.test.runTest
import kotlin.test.assertTrue
import kotlin.test.Test

class DogApiTest {

    @Test
    fun `breeds returns success with data`() = test {
        dogApiMock.givenSuccess()

        val results = sut.breeds()

        results.shouldBeSuccess()
        val breeds = results.getOrNull().shouldNotBeNull()
        breeds.shouldNotBeEmpty()
        val breed = breeds.first()
        breed.name shouldBe "breed"
        breed.subBreeds shouldContainExactly listOf("subBreed")
    }

    @Test
    fun `breeds returns failure on server error`() = test {
        dogApiMock.givenFailure()

        val results = sut.breeds()

        results.shouldBeFailure()
    }

    @Test
    fun `breedImages returns success with image list`() = test {
        dogApiMock.givenSuccess()

        val results = sut.breedImages("pug")

        results.shouldBeSuccess()
        results.getOrNull()?.first() shouldBe "breedImage1"
    }

    @Test
    fun `breedImages returns failure on server error`() = test {
        dogApiMock.givenFailure()
        val results = sut.breedImages("pug")

        results.shouldBeFailure()
    }

    @Test
    fun `randomImage returns success with image URL`() = test {
        dogApiMock.givenSuccess()

        val result = sut.randomImage()

        result.shouldBeSuccess()
        result.getOrNull().shouldNotBeNull()
    }

    @Test
    fun `randomImage returns failure on server error`() = test {
        dogApiMock.givenFailure()

        val result = sut.randomImage()

        result.shouldBeFailure()
    }

    @Test
    fun `randomImage for breed returns success with image URL`() = test {
        dogApiMock.givenSuccess()

        val result = sut.randomImage("pug")

        result.shouldBeSuccess()
        result.getOrNull().shouldNotBeNull()
    }

    @Test
    fun `randomImage for breed returns failure on server error`() = test {
        dogApiMock.givenFailure()

        val result = sut.randomImage("pug")

        result.shouldBeFailure()
    }

    @Test
    fun `listSubBreeds returns success with sub-breed list`() = test {
        dogApiMock.givenSuccess()

        val result = sut.listSubBreeds("hound")

        result.shouldBeSuccess()
        val subBreeds = result.getOrNull().shouldNotBeNull()
        subBreeds.shouldNotBeEmpty()
        subBreeds.first() shouldBe "subBreed1"
    }

    @Test
    fun `listSubBreeds returns failure on server error`() = test {
        dogApiMock.givenFailure()

        val result = sut.listSubBreeds("hound")

        result.shouldBeFailure()
    }

    @Test
    fun `subBreedImages returns success with image list`() = test {
        dogApiMock.givenSuccess()

        val result = sut.subBreedImages("hound", "afghan")

        result.shouldBeSuccess()
        result.getOrNull().shouldNotBeNull().shouldNotBeEmpty()
    }

    @Test
    fun `subBreedImages returns failure on server error`() = test {
        dogApiMock.givenFailure()

        val result = sut.subBreedImages("hound", "afghan")

        result.shouldBeFailure()
    }

    @Test
    fun `server error returns HttpError with status code`() = test {
        dogApiMock.givenFailure()

        val results = sut.breeds()

        results.shouldBeFailure()
        val error = results.exceptionOrNull()
        error.shouldBeInstanceOf<DogApiError.HttpError>()
        error.statusCode shouldBe HTTP_INTERNAL_SERVER_ERROR
    }

    @Test
    fun `invalid breed returns InvalidBreedError`() {
        runTest {
            val client = HttpClient(
                MockEngine { _ ->
                    respond(
                        content = "",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            ) {
                expectSuccess = true
                install(ContentNegotiation) {
                    json(DogJson)
                }
            }
            val api = DogApi(client)

            val results = api.breedImages(INVALID_BREED)

            results.shouldBeFailure()
            val error = results.exceptionOrNull()
            error.shouldBeInstanceOf<DogApiError.InvalidBreedError>()
            error.breedName shouldBe INVALID_BREED
        }
    }

    @Test
    fun `invalid breed is logged at debug level not error level`() {
        runTest {
            val client = HttpClient(
                MockEngine { _ ->
                    respond(
                        content = "",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            ) {
                expectSuccess = true
                install(ContentNegotiation) {
                    json(DogJson)
                }
            }
            val logger = CapturingLogger()
            val api = DogApi(client, logger = logger)

            api.breedImages(INVALID_BREED)

            assertTrue(logger.errorMessages.isEmpty(), "InvalidBreedError should not be logged at error level")
            assertTrue(
                logger.debugMessages.any { it.contains(INVALID_BREED) },
                "InvalidBreedError should be logged at debug level",
            )
        }
    }

    @Test
    fun `server error is logged at error level`() = test {
        dogApiMock.givenFailure()

        sut.breeds()

        assertTrue(logger.errorMessages.isNotEmpty(), "Server error should be logged at error level")
    }

    @Test
    fun `invalid breed name is rejected by validation`() {
        runTest {
            val api = DogApi.createDefault()

            val result = api.breedImages("")

            assertTrue { result.isFailure }
            val error = result.exceptionOrNull()
            assertTrue { error is DogApiError.InvalidBreedError }
        }
    }

    @Test
    fun `network timeout returns NetworkError`() = runTest {
        val client = httpClient(
            MockEngine { _ ->
                throw ConnectTimeoutException("Connection timed out")
            }
        )
        val api = DogApi(client)

        val result = api.breeds()

        result.shouldBeFailure()
        result.exceptionOrNull().shouldBeInstanceOf<DogApiError.NetworkError>()
    }

    @Test
    fun `malformed json returns SerializationError`() = runTest {
        val client = httpClient(
            MockEngine { _ ->
                respond(
                    content = "{ not valid json",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        )
        val api = DogApi(client)

        val result = api.breeds()

        result.shouldBeFailure()
        result.exceptionOrNull().shouldBeInstanceOf<DogApiError.SerializationError>()
    }

    @Test
    fun `breeds returns RemoteApiError when API status is error`() = runTest {
        val api = apiReturningApiError()

        val result = api.breeds()

        result.shouldBeRemoteApiError()
    }

    @Test
    fun `randomImage returns RemoteApiError when API status is error`() = runTest {
        val api = apiReturningApiError()

        val result = api.randomImage()

        result.shouldBeRemoteApiError()
    }

    @Test
    fun `breedImages returns RemoteApiError when API status is error`() = runTest {
        val api = apiReturningApiError()

        val result = api.breedImages("pug")

        result.shouldBeRemoteApiError()
    }

    @Test
    fun `listSubBreeds returns RemoteApiError when API status is error`() = runTest {
        val api = apiReturningApiError()

        val result = api.listSubBreeds("hound")

        result.shouldBeRemoteApiError()
    }

    @Test
    fun `unexpected exception returns UnknownError`() = runTest {
        val client = httpClient(
            MockEngine { _ ->
                throw IllegalStateException("Unexpected test failure")
            }
        )
        val api = DogApi(client)

        val result = api.breeds()

        result.shouldBeFailure()
        result.exceptionOrNull().shouldBeInstanceOf<DogApiError.UnknownError>()
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

        val result = api.breeds()

        result.shouldBeSuccess()
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

    private fun Result<*>.shouldBeRemoteApiError() {
        shouldBeFailure()
        val error = exceptionOrNull().shouldBeInstanceOf<DogApiError.RemoteApiError>()
        error.status shouldBe "error"
        error.apiMessage shouldBe "Breed not found"
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

