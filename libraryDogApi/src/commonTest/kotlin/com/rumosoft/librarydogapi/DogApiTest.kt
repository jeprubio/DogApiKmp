package com.rumosoft.librarydogapi

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DogApiTest {

    @Test
    fun `breeds api call test`() = test {
        runBlocking {
            dogApiMock.givenSuccess()
            val results = sut.breeds()
            println(results)

            assertTrue { results.isSuccess }
            assertEquals("breed", results.getOrNull()?.first()?.name)
            assertEquals(listOf("subBreed"), results.getOrNull()?.first()?.subBreeds)
        }
    }

    @Test
    fun `breeds api call failure test`() = test {
        runBlocking {
            dogApiMock.givenFailure()
            val results = sut.breeds()
            println(results)

            assertTrue { results.isFailure }
        }
    }

    @Test
    fun `breed images test`() = test {
        runBlocking {
            dogApiMock.givenSuccess()
            val results = sut.breedImages("pug")
            println(results)

            assertTrue { results.isSuccess }
            assertEquals("breedImage1", results.getOrNull()?.first())
        }
    }

    @Test
    fun `breed images failure test`() = test {
        runBlocking {
            dogApiMock.givenFailure()
            val results = sut.breedImages("pug")
            println(results)

            assertTrue { results.isFailure }
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
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
        val sut: DogApi = DogApi(httpClient)
    }
}
