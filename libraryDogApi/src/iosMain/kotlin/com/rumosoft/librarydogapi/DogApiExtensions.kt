package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.Breed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Callback-based wrappers around [DogApiClient] for Swift callers who cannot use async/await.
 *
 * Every callback runs on the main thread and receives either a value or a [DogApiError], never
 * both. Every function returns a [Job]; cancelling it aborts the request and the callback is
 * then never invoked.
 *
 * ```swift
 * let job = api.breeds { breeds, error in
 *     if let error { print(error.message ?? ""); return }
 *     print(breeds ?? [])
 * }
 * job.cancel(cause: nil as KotlinCancellationException?)
 * ```
 *
 * Prefer async/await where available: SKIE bridges the suspend functions natively and gives
 * cancellation through the Swift `Task` API, avoiding the bridged [Job] entirely.
 */

/** Fetches all breeds. */
public fun DogApiClient.breeds(onComplete: (List<Breed>?, DogApiError?) -> Unit): Job =
    CoroutineScope(Dispatchers.Main).launch {
        complete(onComplete) { breeds() }
    }

/** Fetches a random image from any breed. */
public fun DogApiClient.randomImage(onComplete: (String?, DogApiError?) -> Unit): Job =
    CoroutineScope(Dispatchers.Main).launch {
        complete(onComplete) { randomImage() }
    }

/** Fetches a random image for [breed]. */
public fun DogApiClient.randomImageForBreed(
    breed: String,
    onComplete: (String?, DogApiError?) -> Unit,
): Job =
    CoroutineScope(Dispatchers.Main).launch {
        complete(onComplete) { randomImage(breed) }
    }

/** Fetches all images for [breed]. */
public fun DogApiClient.breedImages(
    breed: String,
    onComplete: (List<String>?, DogApiError?) -> Unit,
): Job =
    CoroutineScope(Dispatchers.Main).launch {
        complete(onComplete) { breedImages(breed) }
    }

/** Fetches all images for [subBreed] of [breed]. */
public fun DogApiClient.subBreedImages(
    breed: String,
    subBreed: String,
    onComplete: (List<String>?, DogApiError?) -> Unit,
): Job =
    CoroutineScope(Dispatchers.Main).launch {
        complete(onComplete) { subBreedImages(breed, subBreed) }
    }

/** Lists the sub-breeds of [breed]. */
public fun DogApiClient.listSubBreeds(
    breed: String,
    onComplete: (List<String>?, DogApiError?) -> Unit,
): Job =
    CoroutineScope(Dispatchers.Main).launch {
        complete(onComplete) { listSubBreeds(breed) }
    }

/**
 * Reports the outcome of [block] through [onComplete] exactly once. Only [DogApiError] is
 * caught, so cancellation keeps propagating and leaves the callback uncalled.
 */
private suspend fun <T> complete(
    onComplete: (T?, DogApiError?) -> Unit,
    block: suspend () -> T,
) {
    val value = try {
        block()
    } catch (error: DogApiError) {
        onComplete(null, error)
        return
    }
    onComplete(value, null)
}
