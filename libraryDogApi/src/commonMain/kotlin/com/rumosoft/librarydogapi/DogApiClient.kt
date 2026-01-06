package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.Breed

/**
 * Protocol/Interface for the Dog API client.
 * This allows for easy mocking and testing, especially useful for iOS developers
 * who prefer protocol-based dependency injection.
 */
public interface DogApiClient {

    /**
     * Fetches all available dog breeds with their sub-breeds.
     * @return Result containing a list of Breed objects or an error
     */
    public suspend fun breeds(): Result<List<Breed>>

    /**
     * Fetches a random dog image URL from any breed.
     * @return Result containing the image URL string or an error
     */
    public suspend fun randomImage(): Result<String>

    /**
     * Fetches a random dog image URL for a specific breed.
     * @param breed The breed name (case-insensitive)
     * @return Result containing the image URL string or an error
     */
    public suspend fun randomImage(breed: String): Result<String>

    /**
     * Fetches all images for a specific breed.
     * @param breed The breed name (case-insensitive)
     * @return Result containing a list of image URL strings or an error
     */
    public suspend fun breedImages(breed: String): Result<List<String>>

    /**
     * Fetches all images for a specific sub-breed.
     * @param breed The breed name (case-insensitive)
     * @param subBreed The sub-breed name (case-insensitive)
     * @return Result containing a list of image URL strings or an error
     */
    public suspend fun subBreedImages(breed: String, subBreed: String): Result<List<String>>

    /**
     * Fetches all sub-breeds for a specific breed.
     * @param breed The breed name (case-insensitive)
     * @return Result containing a list of sub-breed names or an error
     */
    public suspend fun listSubBreeds(breed: String): Result<List<String>>
}

