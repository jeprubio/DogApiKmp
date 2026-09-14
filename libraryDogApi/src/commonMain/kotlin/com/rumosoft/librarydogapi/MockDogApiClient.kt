package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.Breed
import kotlin.coroutines.cancellation.CancellationException

/**
 * Mock implementation of DogApiClient for testing purposes.
 * This is especially useful for iOS developers who want to test their code
 * without making actual network calls.
 *
 * Each endpoint takes the value to return or the [DogApiError] to throw; the error wins when
 * both are given, and an endpoint with neither throws [DogApiError.UnknownError]. The
 * breed-specific [randomImage] falls back to [randomImage] when [randomImageForBreed] is unset.
 *
 * ```kotlin
 * val mockApi = MockDogApiClient(breeds = listOf(Breed("husky", emptyList())))
 * val failing = MockDogApiClient(breedsError = DogApiError.NetworkError("offline"))
 * ```
 */
public class MockDogApiClient(
    private val breeds: List<Breed>? = null,
    private val breedsError: DogApiError? = null,
    private val randomImage: String? = null,
    private val randomImageError: DogApiError? = null,
    private val randomImageForBreed: String? = null,
    private val randomImageForBreedError: DogApiError? = null,
    private val breedImages: List<String>? = null,
    private val breedImagesError: DogApiError? = null,
    private val subBreedImages: List<String>? = null,
    private val subBreedImagesError: DogApiError? = null,
    private val listSubBreeds: List<String>? = null,
    private val listSubBreedsError: DogApiError? = null,
) : DogApiClient {

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun breeds(): List<Breed> =
        stub("breeds()", breeds, breedsError)

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun randomImage(): String =
        stub("randomImage()", randomImage, randomImageError)

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun randomImage(breed: String): String =
        stub(
            name = "randomImage(breed)",
            value = randomImageForBreed ?: randomImage,
            error = randomImageForBreedError,
        )

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun breedImages(breed: String): List<String> =
        stub("breedImages()", breedImages, breedImagesError)

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun subBreedImages(breed: String, subBreed: String): List<String> =
        stub("subBreedImages()", subBreedImages, subBreedImagesError)

    @Throws(DogApiError::class, CancellationException::class)
    override suspend fun listSubBreeds(breed: String): List<String> =
        stub("listSubBreeds()", listSubBreeds, listSubBreedsError)

    private fun <T> stub(name: String, value: T?, error: DogApiError?): T {
        error?.let { throw it }
        return value ?: throw DogApiError.UnknownError("Mock not configured for $name")
    }
}
