package com.rumosoft.librarydogapi

import com.rumosoft.librarydogapi.models.Breed
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class MockDogApiClientTest {

    @Test
    fun `returns the configured values`() = runTest {
        val mock = MockDogApiClient(
            breeds = listOf(Breed("husky", listOf("siberian"))),
            randomImage = "https://example.com/any.jpg",
            breedImages = listOf("https://example.com/husky.jpg"),
            subBreedImages = listOf("https://example.com/siberian.jpg"),
            listSubBreeds = listOf("siberian"),
        )

        mock.breeds().single().name shouldBe "husky"
        mock.randomImage() shouldBe "https://example.com/any.jpg"
        mock.breedImages("husky") shouldContainExactly listOf("https://example.com/husky.jpg")
        mock.subBreedImages("husky", "siberian") shouldContainExactly
            listOf("https://example.com/siberian.jpg")
        mock.listSubBreeds("husky") shouldContainExactly listOf("siberian")
    }

    @Test
    fun `throws the configured error`() = runTest {
        val mock = MockDogApiClient(breedsError = DogApiError.NetworkError("offline"))

        val error = shouldThrow<DogApiError.NetworkError> { mock.breeds() }

        error.message shouldBe "offline"
    }

    @Test
    fun `error takes precedence over a configured value`() = runTest {
        val mock = MockDogApiClient(
            breeds = listOf(Breed("husky", emptyList())),
            breedsError = DogApiError.HttpError(500, "boom"),
        )

        shouldThrow<DogApiError.HttpError> { mock.breeds() }
    }

    @Test
    fun `an unconfigured endpoint throws UnknownError naming the endpoint`() = runTest {
        val mock = MockDogApiClient()

        val error = shouldThrow<DogApiError.UnknownError> { mock.listSubBreeds("husky") }

        assertTrue(
            error.message!!.contains("listSubBreeds"),
            "The error should name the unconfigured endpoint, but was: ${error.message}",
        )
    }

    @Test
    fun `breed-specific randomImage falls back to the no-argument stub`() = runTest {
        val mock = MockDogApiClient(randomImage = "https://example.com/any.jpg")

        mock.randomImage("husky") shouldBe "https://example.com/any.jpg"
    }

    @Test
    fun `breed-specific randomImage prefers its own stub when both are set`() = runTest {
        val mock = MockDogApiClient(
            randomImage = "https://example.com/any.jpg",
            randomImageForBreed = "https://example.com/husky.jpg",
        )

        mock.randomImage() shouldBe "https://example.com/any.jpg"
        mock.randomImage("husky") shouldBe "https://example.com/husky.jpg"
    }

    @Test
    fun `the mock satisfies the DogApiClient contract so it can be injected`() = runTest {
        val injected: DogApiClient = MockDogApiClient(breeds = emptyList())

        injected.breeds() shouldBe emptyList()
    }
}
