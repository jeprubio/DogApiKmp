package com.rumosoft.librarydogapi

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DogApiFactoryTest {

    /** Guards against the config defaults drifting away from the published constants. */
    @Test
    fun `DogApiConfig defaults match the documented constants`() {
        val config = DogApiConfig()

        config.baseUrl shouldBe DogApi.DEFAULT_BASE_URL
        config.logger shouldBe NoOpDogApiLogger
        config.connectTimeoutMillis shouldBe DogApi.DEFAULT_CONNECT_TIMEOUT_MS
        config.requestTimeoutMillis shouldBe DogApi.DEFAULT_REQUEST_TIMEOUT_MS
        config.socketTimeoutMillis shouldBe DogApi.DEFAULT_SOCKET_TIMEOUT_MS
        config.maxRetries shouldBe DogApi.DEFAULT_MAX_RETRIES
    }

    /** Breed validation short-circuits before any request, so this stays off the network. */
    @Test
    fun `create builds a working instance`() = runTest {
        val api = DogApi.create(DogApiConfig(maxRetries = 0, requestTimeoutMillis = 1_000L))

        shouldThrow<DogApiError.InvalidBreedError> { api.breedImages("") }
    }
}
