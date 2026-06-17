package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.BreedImagesResult
import com.rumosoft.librarydogapi.models.BreedsResult
import com.rumosoft.librarydogapi.models.RandomImageResult
import com.rumosoft.librarydogapi.models.SubBreedsResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json.Default.encodeToString

class DogApiMock {
    private enum class State { UNSET, SUCCESS, FAILURE }

    private var state: State = State.UNSET

    val engine = MockEngine { request ->
        respond(
            content = getResponseFor(request),
            status = if (isSuccess()) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }

    fun givenSuccess() {
        state = State.SUCCESS
    }

    fun givenFailure() {
        state = State.FAILURE
    }

    private fun isSuccess(): Boolean {
        check(state != State.UNSET) { "Mock has not been initialized" }
        return state == State.SUCCESS
    }

    private fun getResponseFor(request: HttpRequestData): String {
        val url = request.url.toString()
        return when {
            url.contains("random") -> encodeToString(
                RandomImageResult.serializer(),
                RandomImageResult(
                    message = "https://images.dog.ceo/breeds/pug/n02110958_1234.jpg",
                    status = "success"
                )
            )
            url.endsWith("/images") -> encodeToString(
                BreedImagesResult.serializer(),
                BreedImagesResult(
                    message = listOf("breedImage1"),
                    status = "success"
                )
            )
            url.endsWith("/list") -> encodeToString(
                SubBreedsResult.serializer(),
                SubBreedsResult(
                    message = listOf("subBreed1"),
                    status = "success"
                )
            )
            else -> encodeToString(
                BreedsResult.serializer(),
                BreedsResult(
                    message = mapOf("breed" to listOf("subBreed")),
                    status = "success"
                )
            )
        }
    }
}