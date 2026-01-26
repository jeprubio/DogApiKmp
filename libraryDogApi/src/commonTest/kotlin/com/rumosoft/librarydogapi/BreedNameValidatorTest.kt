package com.rumosoft.librarydogapi

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test
import kotlin.test.assertTrue

class BreedNameValidatorTest {

    @Test
    fun `valid breed name returns null`() {
        val result = BreedNameValidator.validate("pug")

        result.shouldBeNull()
    }

    @Test
    fun `valid breed name with hyphen returns null`() {
        val result = BreedNameValidator.validate("german-shepherd")

        result.shouldBeNull()
    }

    @Test
    fun `valid breed name with numbers returns null`() {
        val result = BreedNameValidator.validate("dog123")

        result.shouldBeNull()
    }

    @Test
    fun `blank breed name returns InvalidBreedError`() {
        val result = BreedNameValidator.validate("")

        result.shouldNotBeNull()
        result.shouldBeInstanceOf<DogApiError.InvalidBreedError>()
        assertTrue { result.message.contains("cannot be blank") }
    }

    @Test
    fun `whitespace-only breed name returns InvalidBreedError`() {
        val result = BreedNameValidator.validate("   ")

        result.shouldNotBeNull()
        result.shouldBeInstanceOf<DogApiError.InvalidBreedError>()
        assertTrue { result.message.contains("cannot be blank") }
    }

    @Test
    fun `breed name with spaces returns InvalidBreedError`() {
        val result = BreedNameValidator.validate("golden retriever")

        result.shouldNotBeNull()
        result.shouldBeInstanceOf<DogApiError.InvalidBreedError>()
        assertTrue { result.message.contains("cannot contain spaces") }
    }

    @Test
    fun `breed name with tabs returns InvalidBreedError`() {
        val result = BreedNameValidator.validate("golden\tretriever")

        result.shouldNotBeNull()
        result.shouldBeInstanceOf<DogApiError.InvalidBreedError>()
        assertTrue { result.message.contains("cannot contain spaces") }
    }

    @Test
    fun `breed name with special characters returns InvalidBreedError`() {
        val result = BreedNameValidator.validate("pug@home")

        result.shouldNotBeNull()
        result.shouldBeInstanceOf<DogApiError.InvalidBreedError>()
        assertTrue { result.message.contains("can only contain") }
    }

    @Test
    fun `breed name with underscores returns InvalidBreedError`() {
        val result = BreedNameValidator.validate("golden_retriever")

        result.shouldNotBeNull()
        result.shouldBeInstanceOf<DogApiError.InvalidBreedError>()
        assertTrue { result.message.contains("can only contain") }
    }

    @Test
    fun `validation error for sub-breed includes correct type in message`() {
        val result = BreedNameValidator.validate("", "sub-breed")

        result.shouldNotBeNull()
        assertTrue { result.message.contains("sub-breed") }
    }

    @Test
    fun `validation error stores the invalid breed name`() {
        val invalidName = "invalid@breed"
        val result = BreedNameValidator.validate(invalidName)

        result.shouldNotBeNull()
        result.breedName shouldBe invalidName
    }
}
