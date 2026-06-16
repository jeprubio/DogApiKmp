package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.Breed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * iOS-friendly extension functions that provide callback-based APIs
 * in addition to the suspend function APIs.
 *
 * **Prefer async/await** — With SKIE enabled, all suspend functions on [DogApiClient] are
 * bridged to Swift's native `async/await` and can be cancelled via the Swift `Task` API.
 * These callback extensions are provided for cases where async/await is not available or
 * convenient.
 *
 * **Cancellation:** each function returns a [Job]. Call `job.cancel()` to cancel the
 * in-flight request before it completes — for example when the view is dismissed:
 *
 * ```swift
 * let job = api.breeds { [weak self] result in
 *     self?.handleResult(result)
 * }
 * // later, if the view disappears:
 * job.cancel(message: nil)
 * ```
 */

/**
 * Fetches all breeds with a completion handler.
 * @param onComplete Callback invoked with Result when the operation completes.
 *   Called on the main thread.
 * @return A [Job] that can be cancelled to abort the request.
 */
public fun DogApiClient.breeds(onComplete: (Result<List<Breed>>) -> Unit): Job =
    CoroutineScope(Dispatchers.Main).launch {
        onComplete(breeds())
    }

/**
 * Fetches a random image with a completion handler.
 * @param onComplete Callback invoked with Result when the operation completes.
 *   Called on the main thread.
 * @return A [Job] that can be cancelled to abort the request.
 */
public fun DogApiClient.randomImage(onComplete: (Result<String>) -> Unit): Job =
    CoroutineScope(Dispatchers.Main).launch {
        onComplete(randomImage())
    }

/**
 * Fetches a random image for a breed with a completion handler.
 * @param breed The breed name.
 * @param onComplete Callback invoked with Result when the operation completes.
 *   Called on the main thread.
 * @return A [Job] that can be cancelled to abort the request.
 */
public fun DogApiClient.randomImageForBreed(
    breed: String,
    onComplete: (Result<String>) -> Unit,
): Job =
    CoroutineScope(Dispatchers.Main).launch {
        onComplete(randomImage(breed))
    }

/**
 * Fetches all images for a breed with a completion handler.
 * @param breed The breed name.
 * @param onComplete Callback invoked with Result when the operation completes.
 *   Called on the main thread.
 * @return A [Job] that can be cancelled to abort the request.
 */
public fun DogApiClient.breedImages(
    breed: String,
    onComplete: (Result<List<String>>) -> Unit,
): Job =
    CoroutineScope(Dispatchers.Main).launch {
        onComplete(breedImages(breed))
    }

/**
 * Fetches all images for a sub-breed with a completion handler.
 * @param breed The breed name.
 * @param subBreed The sub-breed name.
 * @param onComplete Callback invoked with Result when the operation completes.
 *   Called on the main thread.
 * @return A [Job] that can be cancelled to abort the request.
 */
public fun DogApiClient.subBreedImages(
    breed: String,
    subBreed: String,
    onComplete: (Result<List<String>>) -> Unit,
): Job =
    CoroutineScope(Dispatchers.Main).launch {
        onComplete(subBreedImages(breed, subBreed))
    }

/**
 * Lists sub-breeds for a breed with a completion handler.
 * @param breed The breed name.
 * @param onComplete Callback invoked with Result when the operation completes.
 *   Called on the main thread.
 * @return A [Job] that can be cancelled to abort the request.
 */
public fun DogApiClient.listSubBreeds(
    breed: String,
    onComplete: (Result<List<String>>) -> Unit,
): Job =
    CoroutineScope(Dispatchers.Main).launch {
        onComplete(listSubBreeds(breed))
    }
