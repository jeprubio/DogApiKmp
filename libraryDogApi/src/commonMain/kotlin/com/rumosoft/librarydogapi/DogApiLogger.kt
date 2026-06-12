package com.rumosoft.librarydogapi

/**
 * Simple logger interface for the Dog API library.
 *
 * Implement this interface and pass it to [DogApi] (or [DogApi.createDefault])
 * to receive request-level debug messages and mapped error events.
 *
 * By default [NoOpDogApiLogger] is used, which discards all output, keeping
 * the library silent until the consumer explicitly opts in.
 *
 * Example – forwarding to Napier on Android/iOS:
 * ```kotlin
 * val api = DogApi.createDefault(
 *     logger = object : DogApiLogger {
 *         override fun d(msg: String) = Napier.d(msg, tag = "DogApi")
 *         override fun e(msg: String, throwable: Throwable?) =
 *             Napier.e(msg, throwable, tag = "DogApi")
 *     }
 * )
 * ```
 */
public interface DogApiLogger {
    /** Log a debug-level message. */
    public fun d(msg: String)

    /** Log an error-level message, optionally with the causing [throwable]. */
    public fun e(msg: String, throwable: Throwable? = null)
}

/**
 * Default no-op logger that silently discards all messages.
 * Used when no logger is provided to keep the library quiet out of the box.
 */
public object NoOpDogApiLogger : DogApiLogger {
    override fun d(msg: String): Unit = Unit
    override fun e(msg: String, throwable: Throwable?): Unit = Unit
}

