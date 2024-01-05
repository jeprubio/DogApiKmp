package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.BreedImagesResult
import com.rumosoft.librarydogapi.models.BreedsResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json.Default.encodeToString

class DogApiMock {
    private var isSuccess: Boolean? = null
        get() = field ?: throw IllegalStateException("Mock has not beet initialized")

    val engine = MockEngine { request ->
        respond(
            content = getResponseFor(request),
            status = if (isSuccess == true) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }

    fun givenSuccess() {
        isSuccess = true
    }

    fun givenFailure() {
        isSuccess = false
    }

    private fun getResponseFor(request: HttpRequestData): String {
        return when (request.url.toString()) {
            "https://dog.ceo/api/breed/pug/images" -> {
                encodeToString(
                    BreedImagesResult.serializer(), BreedImagesResult(
                        message = listOf(
                            "breedImage1"
                        ),
                        status = "success"
                    )
                )
            }
            else -> {
                encodeToString(
                    BreedsResult.serializer(), BreedsResult(
                        message = mapOf("breed" to listOf("subBreed")),
                        status = "success"
                    )
                )
            }
        }
    }
}