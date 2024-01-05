package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.Breed
import com.rumosoft.librarydogapi.models.BreedImagesResult
import com.rumosoft.librarydogapi.models.BreedsResult
import com.rumosoft.librarydogapi.models.SubBreedsResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json

class DogApi(
    private val client: HttpClient
) {
    companion object {
        fun createDefault(): DogApi {
            val defaultClient = HttpClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            return DogApi(defaultClient)
        }
    }

    suspend fun breeds(): Result<List<Breed>> {
        val response: HttpResponse = client.get("https://dog.ceo/api/breeds/list/all")
        return response.asKotlinResult<BreedsResult>().map { result ->
            result.message.map { (breed, subBreeds) ->
                Breed(
                    name = breed,
                    subBreeds = subBreeds
                )
            }
        }
    }

    suspend fun randomImage(): Result<String> {
        val response = client.get("https://dog.ceo/api/breeds/image/random")
        return response.asKotlinResult()
    }

    suspend fun randomImage(breed: String): Result<String> {
        val response = client.get("https://dog.ceo/api/breed/${breed.lowercase()}/images/random")
        return response.asKotlinResult()
    }

    suspend fun breedImages(breed: String): Result<List<String>> {
        val response: HttpResponse = client.get("https://dog.ceo/api/breed/${breed.lowercase()}/images")
        return response.asKotlinResult<BreedImagesResult>().map { result ->
            result.message
        }
    }

    suspend fun subBreedImages(breed: String, subBreed: String): String {
        val response =
            client.get("https://dog.ceo/api/breed/${breed.lowercase()}/${subBreed.lowercase()}/images")
        return response.body()
    }

    suspend fun listSubBreeds(breed: String): List<String> {
        val result: SubBreedsResult =
            client.get("https://dog.ceo/api/breed/${breed.lowercase()}/list").body()
        return result.message
    }
}

private suspend inline fun <reified T> HttpResponse.asKotlinResult(): Result<T> {
    return if (status.isSuccess()) {
        Result.success(body<T>())
    } else {
        Result.failure(Exception("Request failed with status: $status"))
    }
}
