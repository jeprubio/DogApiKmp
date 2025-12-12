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
import io.ktor.utils.io.core.Closeable

public class DogApi(
    private val client: HttpClient,
    private val baseUrl: String = "https://dog.ceo/api"
) : Closeable {

    override fun close(): Unit = client.close()

    public companion object {
        public fun createDefault(): DogApi {
            val defaultClient = HttpClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            return DogApi(defaultClient)
        }
    }

    public suspend fun breeds(): Result<List<Breed>> {
        val response: HttpResponse = client.get("$baseUrl/breeds/list/all")
        return response.asKotlinResult<BreedsResult>().map { result ->
            result.message.map { (breed, subBreeds) ->
                Breed(
                    name = breed,
                    subBreeds = subBreeds
                )
            }
        }
    }

    public suspend fun randomImage(): Result<String> {
        val response = client.get("$baseUrl/breeds/image/random")
        return response.asKotlinResult()
    }

    public suspend fun randomImage(breed: String): Result<String> {
        val response = client.get("$baseUrl/breed/${breed.lowercase()}/images/random")
        return response.asKotlinResult()
    }

    public suspend fun breedImages(breed: String): Result<List<String>> {
        val response: HttpResponse = client.get("$baseUrl/breed/${breed.lowercase()}/images")
        return response.asKotlinResult<BreedImagesResult>().map { result ->
            result.message
        }
    }

    public suspend fun subBreedImages(breed: String, subBreed: String): Result<List<String>> {
        val response =
            client.get("$baseUrl/breed/${breed.lowercase()}/${subBreed.lowercase()}/images")
        return response.asKotlinResult<BreedImagesResult>().map { it.message }
    }

    public suspend fun listSubBreeds(breed: String): Result<List<String>> {
        val response: HttpResponse = client.get("$baseUrl/breed/${breed.lowercase()}/list")
        return response.asKotlinResult<SubBreedsResult>().map { result ->
            result.message
        }
    }
}

private suspend inline fun <reified T> HttpResponse.asKotlinResult(): Result<T> {
    return if (status.isSuccess()) {
        Result.success(body<T>())
    } else {
        Result.failure(Exception("Request failed with status: $status"))
    }
}
