package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.Breed
import kotlin.coroutines.cancellation.CancellationException

/**
 * Protocol/Interface for the Dog API client.
 * This allows for easy mocking and testing, especially useful for iOS developers
 * who prefer protocol-based dependency injection.
 *
 * Functions return their value or throw a [DogApiError]. `kotlin.Result` is avoided because
 * Kotlin/Native erases it to `Any?` in the Objective-C export, which would untype the Swift API.
 * Kotlin callers who want one use `runCatching { api.breeds() }`.
 */
public interface DogApiClient {

    /**
     * Fetches all available dog breeds with their sub-breeds.
     * @return the list of breeds with their sub-breeds
     * @throws DogApiError if the request fails
     */
    @Throws(DogApiError::class, CancellationException::class)
    public suspend fun breeds(): List<Breed>

    /**
     * Fetches a random dog image URL from any breed.
     * @return the image URL
     * @throws DogApiError if the request fails
     */
    @Throws(DogApiError::class, CancellationException::class)
    public suspend fun randomImage(): String

    /**
     * Fetches a random dog image URL for a specific breed.
     * @param breed The breed name (case-insensitive)
     * @return the image URL
     * @throws DogApiError.InvalidBreedError if [breed] is not a valid breed name
     * @throws DogApiError if the request fails
     */
    @Throws(DogApiError::class, CancellationException::class)
    public suspend fun randomImage(breed: String): String

    /**
     * Fetches all images for a specific breed.
     * @param breed The breed name (case-insensitive)
     * @return the image URLs
     * @throws DogApiError.InvalidBreedError if [breed] is not a valid breed name
     * @throws DogApiError if the request fails
     */
    @Throws(DogApiError::class, CancellationException::class)
    public suspend fun breedImages(breed: String): List<String>

    /**
     * Fetches all images for a specific sub-breed.
     * @param breed The breed name (case-insensitive)
     * @param subBreed The sub-breed name (case-insensitive)
     * @return the image URLs
     * @throws DogApiError.InvalidBreedError if [breed] or [subBreed] is not a valid name
     * @throws DogApiError if the request fails
     */
    @Throws(DogApiError::class, CancellationException::class)
    public suspend fun subBreedImages(breed: String, subBreed: String): List<String>

    /**
     * Fetches all sub-breeds for a specific breed.
     * @param breed The breed name (case-insensitive)
     * @return the sub-breed names
     * @throws DogApiError.InvalidBreedError if [breed] is not a valid breed name
     * @throws DogApiError if the request fails
     */
    @Throws(DogApiError::class, CancellationException::class)
    public suspend fun listSubBreeds(breed: String): List<String>
}
