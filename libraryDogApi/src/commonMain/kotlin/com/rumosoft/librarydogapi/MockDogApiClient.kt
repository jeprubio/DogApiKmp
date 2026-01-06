package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.Breed

/**
 * Mock implementation of DogApiClient for testing purposes.
 * This is especially useful for iOS developers who want to test their code
 * without making actual network calls.
 *
 * Example usage in tests:
 * ```
 * val mockApi = MockDogApiClient(
 *     breedsResult = Result.success(listOf(Breed("husky", emptyList()))),
 *     randomImageResult = Result.success("https://images.dog.ceo/breeds/husky/n02110185_1469.jpg")
 * )
 * // Use mockApi in your tests
 * ```
 */
public class MockDogApiClient(
    private val breedsResult: Result<List<Breed>>? = null,
    private val randomImageResult: Result<String>? = null,
    private val breedImagesResult: Result<List<String>>? = null,
    private val subBreedImagesResult: Result<List<String>>? = null,
    private val listSubBreedsResult: Result<List<String>>? = null
) : DogApiClient {

    override suspend fun breeds(): Result<List<Breed>> {
        return breedsResult ?: Result.failure(
            DogApiError.UnknownError("Mock not configured for breeds()")
        )
    }

    override suspend fun randomImage(): Result<String> {
        return randomImageResult ?: Result.failure(
            DogApiError.UnknownError("Mock not configured for randomImage()")
        )
    }

    override suspend fun randomImage(breed: String): Result<String> {
        return randomImageResult ?: Result.failure(
            DogApiError.UnknownError("Mock not configured for randomImage(breed)")
        )
    }

    override suspend fun breedImages(breed: String): Result<List<String>> {
        return breedImagesResult ?: Result.failure(
            DogApiError.UnknownError("Mock not configured for breedImages()")
        )
    }

    override suspend fun subBreedImages(breed: String, subBreed: String): Result<List<String>> {
        return subBreedImagesResult ?: Result.failure(
            DogApiError.UnknownError("Mock not configured for subBreedImages()")
        )
    }

    override suspend fun listSubBreeds(breed: String): Result<List<String>> {
        return listSubBreedsResult ?: Result.failure(
            DogApiError.UnknownError("Mock not configured for listSubBreeds()")
        )
    }
}

