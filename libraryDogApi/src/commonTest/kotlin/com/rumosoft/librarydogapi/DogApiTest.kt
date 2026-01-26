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
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test

class DogApiTest {

    companion object {
        private const val INVALID_BREED = "invalidbreed"
        private const val HTTP_INTERNAL_SERVER_ERROR = 500
    }

    @Test
    fun `breeds returns success with data`() = test {
        runTest {
            dogApiMock.givenSuccess()

            val results = sut.breeds()

            results.shouldBeSuccess()
            val breeds = results.getOrNull().shouldNotBeNull()
            breeds.shouldNotBeEmpty()
            val breed = breeds.first()
            breed.name shouldBe "breed"
            breed.subBreeds shouldContainExactly listOf("subBreed")
        }
    }

    @Test
    fun `breeds returns failure on server error`() = test {
        runTest {
            dogApiMock.givenFailure()

            val results = sut.breeds()

            results.shouldBeFailure()
        }
    }

    @Test
    fun `breedImages returns success with image list`() = test {
        runTest {
            dogApiMock.givenSuccess()

            val results = sut.breedImages("pug")

            results.shouldBeSuccess()
            results.getOrNull()?.first() shouldBe "breedImage1"
        }
    }

    @Test
    fun `breedImages returns failure on server error`() = test {
        runTest {
            dogApiMock.givenFailure()
            val results = sut.breedImages("pug")

            results.shouldBeFailure()
        }
    }

    @Test
    fun `server error returns HttpError with status code`() = test {
        runTest {
            dogApiMock.givenFailure()

            val results = sut.breeds()

            results.shouldBeFailure()
            val error = results.exceptionOrNull()
            error.shouldBeInstanceOf<DogApiError.HttpError>()
            error.statusCode shouldBe HTTP_INTERNAL_SERVER_ERROR
        }
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
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
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
    fun `invalid breed name is rejected by validation`() {
        runTest {
            val api = DogApi.createDefault()

            val result = api.breedImages("")

            assertTrue { result.isFailure }
            val error = result.exceptionOrNull()
            assertTrue { error is DogApiError.InvalidBreedError }
        }
    }


    private fun test(block: TestScope.() -> Unit) {
        TestScope().block()
    }

    private class TestScope {
        val dogApiMock = DogApiMock()
        val httpClient = HttpClient(
            engine = dogApiMock.engine
        ) {
            expectSuccess = true  // Make Ktor throw exceptions for non-2xx responses
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
        val sut: DogApiClient = DogApi(httpClient)
    }
}
