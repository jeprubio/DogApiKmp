package com.rumosoft.librarydogapi

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class BreedNameValidatorTest {

    @Test
    fun `accepts letters digits and hyphens`() {
        BreedNameValidator.validate("pug").shouldBeNull()
        BreedNameValidator.validate("german-shepherd").shouldBeNull()
        BreedNameValidator.validate("dog123").shouldBeNull()
    }

    @Test
    fun `rejects a blank name`() {
        BreedNameValidator.validate("   ").shouldNotBeNull()
            .message.shouldNotBeNull() shouldContain "cannot be blank"
    }

    @Test
    fun `rejects a name containing whitespace`() {
        BreedNameValidator.validate("golden retriever").shouldNotBeNull()
            .message.shouldNotBeNull() shouldContain "cannot contain spaces"
    }

    @Test
    fun `rejects disallowed characters`() {
        BreedNameValidator.validate("pug@home").shouldNotBeNull()
            .message.shouldNotBeNull() shouldContain "can only contain"
        BreedNameValidator.validate("golden_retriever").shouldNotBeNull()
            .message.shouldNotBeNull() shouldContain "can only contain"
    }

    /** Dog CEO slugs are ASCII, so "pügs" must fail locally rather than 404 remotely. */
    @Test
    fun `rejects non-ASCII letters`() {
        BreedNameValidator.validate("pügs").shouldNotBeNull()
            .message.shouldNotBeNull() shouldContain "can only contain"
    }

    @Test
    fun `reports the type and the offending name`() {
        val error = BreedNameValidator.validate("bad name", "sub-breed").shouldNotBeNull()

        error.message.shouldNotBeNull() shouldContain "sub-breed"
        error.breedName shouldBe "bad name"
    }
}
