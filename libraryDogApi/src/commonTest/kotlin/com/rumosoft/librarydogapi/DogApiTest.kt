package com.rumosoft.librarydogapi

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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

            assertTrue { results.isSuccess }
            assertEquals("breed", results.getOrNull()?.first()?.name)
            assertEquals(listOf("subBreed"), results.getOrNull()?.first()?.subBreeds)
        }
    }

    @Test
    fun `breeds returns failure on server error`() = test {
        runTest {
            dogApiMock.givenFailure()
            val results = sut.breeds()

            assertTrue { results.isFailure }
        }
    }

    @Test
    fun `breedImages returns success with image list`() = test {
        runTest {
            dogApiMock.givenSuccess()
            val results = sut.breedImages("pug")

            assertTrue { results.isSuccess }
            assertEquals("breedImage1", results.getOrNull()?.first())
        }
    }

    @Test
    fun `breedImages returns failure on server error`() = test {
        runTest {
            dogApiMock.givenFailure()
            val results = sut.breedImages("pug")

            assertTrue { results.isFailure }
        }
    }

    @Test
    fun `server error returns HttpError with status code`() = test {
        runTest {
            dogApiMock.givenFailure()
            val results = sut.breeds()

            assertTrue { results.isFailure }
            val error = results.exceptionOrNull()
            assertTrue { error is DogApiError.HttpError }
            assertEquals(HTTP_INTERNAL_SERVER_ERROR, (error as DogApiError.HttpError).statusCode)
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

            assertTrue { results.isFailure }
            val error = results.exceptionOrNull()
            assertTrue { error is DogApiError.InvalidBreedError }
            assertEquals(INVALID_BREED, (error as DogApiError.InvalidBreedError).breedName)
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
