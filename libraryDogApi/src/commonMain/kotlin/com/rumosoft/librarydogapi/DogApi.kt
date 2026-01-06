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

/**
 * Default implementation of the Dog API client.
 *
 * This class provides access to the Dog CEO API (https://dog.ceo/dog-api/).
 * It implements DogApiClient for better testability and dependency injection.
 *
 * The default implementation uses a shared HttpClient for efficiency.
 * You can also provide your own HttpClient instance for custom configuration.
 *
 * Example usage:
 * ```
 * val api = DogApi.createDefault()
 * val breeds = api.breeds().getOrNull()
 * ```
 *
 * For custom HttpClient configuration:
 * ```
 * val customClient = HttpClient { /* your config */ }
 * val api = DogApi(customClient)
 * ```
 *
 * For iOS developers: Use the protocol DogApiClient for dependency injection
 * to make your code more testable.
 */
public class DogApi(
    private val client: HttpClient,
    private val baseUrl: String = DEFAULT_BASE_URL
) : DogApiClient {

    public companion object {
        public const val DEFAULT_BASE_URL: String = "https://dog.ceo/api"

        /**
         * Shared HttpClient instance used by createDefault().
         * This client is reused across all default DogApi instances for efficiency.
         */
        private val sharedClient: HttpClient by lazy {
            HttpClient {
                install(ContentNegotiation) {
                    json()
                }
            }
        }

        /**
         * Creates a DogApi instance with default configuration using a shared HttpClient.
         * This is the simplest way to get started.
         */
        public fun createDefault(baseUrl: String = DEFAULT_BASE_URL): DogApi {
            return DogApi(sharedClient, baseUrl)
        }
    }

    override suspend fun breeds(): Result<List<Breed>> {
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

    override suspend fun randomImage(): Result<String> {
        val response = client.get("$baseUrl/breeds/image/random")
        return response.asKotlinResult()
    }

    override suspend fun randomImage(breed: String): Result<String> {
        val response = client.get("$baseUrl/breed/${breed.lowercase()}/images/random")
        return response.asKotlinResult()
    }

    override suspend fun breedImages(breed: String): Result<List<String>> {
        val response: HttpResponse = client.get("$baseUrl/breed/${breed.lowercase()}/images")
        return response.asKotlinResult<BreedImagesResult>().map { result ->
            result.message
        }
    }

    override suspend fun subBreedImages(breed: String, subBreed: String): Result<List<String>> {
        val response =
            client.get("$baseUrl/breed/${breed.lowercase()}/${subBreed.lowercase()}/images")
        return response.asKotlinResult<BreedImagesResult>().map { it.message }
    }

    override suspend fun listSubBreeds(breed: String): Result<List<String>> {
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
