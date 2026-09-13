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
 * bridged to Swift's native `async/await`, throw a catchable [DogApiError], and can be
 * cancelled via the Swift `Task` API. These callback extensions are provided for cases where
 * async/await is not available or convenient.
 *
 * Each callback receives either a value or a [DogApiError], never both:
 *
 * ```swift
 * let job = api.breeds { breeds, error in
 *     if let error { print("failed: \(error.message ?? "")"); return }
 *     print(breeds ?? [])
 * }
 * ```
 *
 * **Cancellation:** each function returns a [Job]. Call `job.cancel(cause: nil)` to cancel the
 * in-flight request before it completes — for example when the view is dismissed. The callback
 * is **not** invoked when the request is cancelled.
 */

/**
 * Fetches all breeds with a completion handler.
 * @param onComplete Callback invoked with the breeds, or with a [DogApiError] on failure.
 *   Called on the main thread. Not called if the returned [Job] is cancelled.
 * @return A [Job] that can be cancelled to abort the request.
 */
public fun DogApiClient.breeds(onComplete: (List<Breed>?, DogApiError?) -> Unit): Job =
    CoroutineScope(Dispatchers.Main).launch {
        complete(onComplete) { breeds() }
    }

/**
 * Fetches a random image with a completion handler.
 * @param onComplete Callback invoked with the image URL, or with a [DogApiError] on failure.
 *   Called on the main thread. Not called if the returned [Job] is cancelled.
 * @return A [Job] that can be cancelled to abort the request.
 */
public fun DogApiClient.randomImage(onComplete: (String?, DogApiError?) -> Unit): Job =
    CoroutineScope(Dispatchers.Main).launch {
        complete(onComplete) { randomImage() }
    }

/**
 * Fetches a random image for a breed with a completion handler.
 * @param breed The breed name.
 * @param onComplete Callback invoked with the image URL, or with a [DogApiError] on failure.
 *   Called on the main thread. Not called if the returned [Job] is cancelled.
 * @return A [Job] that can be cancelled to abort the request.
 */
public fun DogApiClient.randomImageForBreed(
    breed: String,
    onComplete: (String?, DogApiError?) -> Unit,
): Job =
    CoroutineScope(Dispatchers.Main).launch {
        complete(onComplete) { randomImage(breed) }
    }

/**
 * Fetches all images for a breed with a completion handler.
 * @param breed The breed name.
 * @param onComplete Callback invoked with the image URLs, or with a [DogApiError] on failure.
 *   Called on the main thread. Not called if the returned [Job] is cancelled.
 * @return A [Job] that can be cancelled to abort the request.
 */
public fun DogApiClient.breedImages(
    breed: String,
    onComplete: (List<String>?, DogApiError?) -> Unit,
): Job =
    CoroutineScope(Dispatchers.Main).launch {
        complete(onComplete) { breedImages(breed) }
    }

/**
 * Fetches all images for a sub-breed with a completion handler.
 * @param breed The breed name.
 * @param subBreed The sub-breed name.
 * @param onComplete Callback invoked with the image URLs, or with a [DogApiError] on failure.
 *   Called on the main thread. Not called if the returned [Job] is cancelled.
 * @return A [Job] that can be cancelled to abort the request.
 */
public fun DogApiClient.subBreedImages(
    breed: String,
    subBreed: String,
    onComplete: (List<String>?, DogApiError?) -> Unit,
): Job =
    CoroutineScope(Dispatchers.Main).launch {
        complete(onComplete) { subBreedImages(breed, subBreed) }
    }

/**
 * Lists sub-breeds for a breed with a completion handler.
 * @param breed The breed name.
 * @param onComplete Callback invoked with the sub-breed names, or with a [DogApiError] on
 *   failure. Called on the main thread. Not called if the returned [Job] is cancelled.
 * @return A [Job] that can be cancelled to abort the request.
 */
public fun DogApiClient.listSubBreeds(
    breed: String,
    onComplete: (List<String>?, DogApiError?) -> Unit,
): Job =
    CoroutineScope(Dispatchers.Main).launch {
        complete(onComplete) { listSubBreeds(breed) }
    }

/**
 * Runs [block] and reports the outcome through [onComplete] exactly once.
 *
 * Only [DogApiError] is caught, so a [kotlinx.coroutines.CancellationException] keeps
 * propagating and the callback is not invoked for a cancelled request.
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
