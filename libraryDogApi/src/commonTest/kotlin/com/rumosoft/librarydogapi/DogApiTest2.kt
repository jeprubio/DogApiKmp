package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.BreedImagesResult
import com.rumosoft.librarydogapi.models.BreedsResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
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

class DogApiTest2 {

    @Test
    fun `breeds api call test`() {
        runTest {
            val sut = givenSut(
                MockEngine { _ ->
                    respond(
                        content = Json.encodeToString(
                            BreedsResult.serializer(), BreedsResult(
                                message = mapOf("breed" to listOf("subBreed")),
                                status = "success"
                            )
                        ),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            )

            val results = sut.breeds()
            println(results)

            assertTrue { results.isSuccess }
            assertEquals("breed", results.getOrNull()?.first()?.name)
            assertEquals(listOf("subBreed"), results.getOrNull()?.first()?.subBreeds)
        }
    }

    @Test
    fun `breeds api call failure test`() {
        runTest {
            val sut = givenSut(
                MockEngine { _ ->
                    respond(
                        content = "",
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            )

            val results = sut.breeds()
            println(results)

            assertTrue { results.isFailure }
        }
    }

    @Test
    fun `breed images test`() {
        runTest {
            val sut = givenSut(
                MockEngine { _ ->
                    respond(
                        Json.encodeToString(
                            BreedImagesResult.serializer(), BreedImagesResult(
                                message = listOf(
                                    "breedImage1"
                                ),
                                status = "success"
                            )
                        ),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            )

            val results = sut.breedImages("pug")
            println(results)

            assertTrue { results.isSuccess }
            assertEquals("breedImage1", results.getOrNull()?.first())
        }
    }

    @Test
    fun `breed images failure test`() {
        runTest {
            val sut = givenSut(
                MockEngine { _ ->
                    respond(
                        content = "",
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            )

            val results = sut.breedImages("pug")
            println(results)

            assertTrue { results.isFailure }
        }
    }

    private fun givenHttpClient(engine: HttpClientEngine): HttpClient {
        return HttpClient(engine = engine) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    private fun givenSut(engine: HttpClientEngine): DogApi {
        return DogApi(givenHttpClient(engine))
    }
}
