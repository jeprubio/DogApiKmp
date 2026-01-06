package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.Breed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * iOS-friendly extension functions that provide callback-based APIs
 * in addition to the suspend function APIs.
 *
 * These functions bridge Kotlin coroutines to Swift closures,
 * making it easier for iOS developers who prefer callback patterns.
 */

/**
 * Fetches all breeds with a completion handler.
 * @param onComplete Callback invoked with Result when the operation completes
 */
public fun DogApiClient.breeds(onComplete: (Result<List<Breed>>) -> Unit) {
    CoroutineScope(Dispatchers.Main).launch {
        val result = breeds()
        onComplete(result)
    }
}

/**
 * Fetches a random image with a completion handler.
 * @param onComplete Callback invoked with Result when the operation completes
 */
public fun DogApiClient.randomImage(onComplete: (Result<String>) -> Unit) {
    CoroutineScope(Dispatchers.Main).launch {
        val result = randomImage()
        onComplete(result)
    }
}

/**
 * Fetches a random image for a breed with a completion handler.
 * @param breed The breed name
 * @param onComplete Callback invoked with Result when the operation completes
 */
public fun DogApiClient.randomImageForBreed(breed: String, onComplete: (Result<String>) -> Unit) {
    CoroutineScope(Dispatchers.Main).launch {
        val result = randomImage(breed)
        onComplete(result)
    }
}

/**
 * Fetches breed images with a completion handler.
 * @param breed The breed name
 * @param onComplete Callback invoked with Result when the operation completes
 */
public fun DogApiClient.breedImages(breed: String, onComplete: (Result<List<String>>) -> Unit) {
    CoroutineScope(Dispatchers.Main).launch {
        val result = breedImages(breed)
        onComplete(result)
    }
}

/**
 * Fetches sub-breed images with a completion handler.
 * @param breed The breed name
 * @param subBreed The sub-breed name
 * @param onComplete Callback invoked with Result when the operation completes
 */
public fun DogApiClient.subBreedImages(
    breed: String,
    subBreed: String,
    onComplete: (Result<List<String>>) -> Unit
) {
    CoroutineScope(Dispatchers.Main).launch {
        val result = subBreedImages(breed, subBreed)
        onComplete(result)
    }
}

/**
 * Lists sub-breeds with a completion handler.
 * @param breed The breed name
 * @param onComplete Callback invoked with Result when the operation completes
 */
public fun DogApiClient.listSubBreeds(breed: String, onComplete: (Result<List<String>>) -> Unit) {
    CoroutineScope(Dispatchers.Main).launch {
        val result = listSubBreeds(breed)
        onComplete(result)
    }
}

