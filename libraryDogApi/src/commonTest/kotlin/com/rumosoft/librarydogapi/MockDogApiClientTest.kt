package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.Breed
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MockDogApiClientTest {

    @Test
    fun `returns the configured values`() = runTest {
        val mock = MockDogApiClient(
            breeds = listOf(Breed("husky", listOf("siberian"))),
            randomImage = "https://example.com/any.jpg",
            listSubBreeds = listOf("siberian"),
        )

        mock.breeds().single().name shouldBe "husky"
        mock.randomImage() shouldBe "https://example.com/any.jpg"
        mock.listSubBreeds("husky") shouldBe listOf("siberian")
    }

    @Test
    fun `error makes every endpoint throw`() = runTest {
        val mock = MockDogApiClient(
            breeds = listOf(Breed("husky", emptyList())),
            error = DogApiError.NetworkError("offline"),
        )

        shouldThrow<DogApiError.NetworkError> { mock.breeds() }
        shouldThrow<DogApiError.NetworkError> { mock.randomImage() }
    }

    @Test
    fun `an unconfigured endpoint throws UnknownError naming it`() = runTest {
        val error = shouldThrow<DogApiError.UnknownError> { MockDogApiClient().listSubBreeds("husky") }

        error.message!! shouldContain "listSubBreeds"
    }

    @Test
    fun `breed-specific randomImage falls back to the no-argument stub`() = runTest {
        MockDogApiClient(randomImage = "https://example.com/any.jpg")
            .randomImage("husky") shouldBe "https://example.com/any.jpg"

        MockDogApiClient(
            randomImage = "https://example.com/any.jpg",
            randomImageForBreed = "https://example.com/husky.jpg",
        ).randomImage("husky") shouldBe "https://example.com/husky.jpg"
    }
}
