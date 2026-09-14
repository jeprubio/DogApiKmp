package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.Breed
import kotlin.coroutines.cancellation.CancellationException

/**
 * Mock implementation of DogApiClient for testing purposes.
 * This is especially useful for iOS developers who want to test their code
 * without making actual network calls.
 *
 * Set [error] to make every endpoint throw. Otherwise each endpoint returns its stub, or throws
 * [DogApiError.UnknownError] when it has none. The breed-specific [randomImage] falls back to
 * [randomImage] when [randomImageForBreed] is unset.
 *
 * ```kotlin
 * val mockApi = MockDogApiClient(breeds = listOf(Breed("husky", emptyList())))
 * val failing = MockDogApiClient(error = DogApiError.NetworkError("offline"))
 * ```
 *
 * For finer control — one endpoint succeeding while another fails — implement [DogApiClient].
 */
public class MockDogApiClient(
    private val breeds: List<Breed>? = null,
    private val randomImage: String? = null,
    private val randomImageForBreed: String? = null,
    private val breedImages: List<String>? = null,
    private val subBreedImages: List<String>? = null,
    private val listSubBreeds: List<String>? = null,
    private val error: DogApiError? = null,
) : DogApiClient {

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun breeds(): List<Breed> = stub("breeds()", breeds)

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun randomImage(): String = stub("randomImage()", randomImage)

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun randomImage(breed: String): String =
        stub("randomImage(breed)", randomImageForBreed ?: randomImage)

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun breedImages(breed: String): List<String> =
        stub("breedImages()", breedImages)

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun subBreedImages(breed: String, subBreed: String): List<String> =
        stub("subBreedImages()", subBreedImages)

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun listSubBreeds(breed: String): List<String> =
        stub("listSubBreeds()", listSubBreeds)

    private fun <T> stub(name: String, value: T?): T {
        error?.let { throw it }
        return value ?: throw DogApiError.UnknownError("Mock not configured for $name")
    }
}
