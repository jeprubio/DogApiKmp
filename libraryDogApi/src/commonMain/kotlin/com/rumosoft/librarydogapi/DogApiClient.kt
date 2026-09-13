package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.Breed
import kotlin.coroutines.cancellation.CancellationException

/**
 * Protocol/Interface for the Dog API client.
 * This allows for easy mocking and testing, especially useful for iOS developers
 * who prefer protocol-based dependency injection.
 *
 * Every function either returns its value directly or throws a [DogApiError]. This keeps the
 * contract idiomatic on both platforms:
 *
 * - **Kotlin** — wrap any call in `runCatching { }` if you prefer a `Result`:
 *   ```kotlin
 *   val result: Result<List<Breed>> = runCatching { api.breeds() }
 *   ```
 * - **Swift** — the functions arrive as native `async throws`, so `try await` and a
 *   `catch` over [DogApiError] work without any wrapper layer.
 *
 * Returning `kotlin.Result` from these functions is deliberately avoided: `Result` is an inline
 * value class, which Kotlin/Native erases to `Any?` when exporting to Objective-C. That would
 * strip every type from the Swift API.
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
