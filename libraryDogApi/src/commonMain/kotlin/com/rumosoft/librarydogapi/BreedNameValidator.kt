package com.rumosoft.librarydogapi

/**
 * Validator for breed and sub-breed names.
 * Encapsulates validation rules to ensure breed names follow the expected format.
 */
internal object BreedNameValidator {

    /**
     * Validates a breed or sub-breed name.
     *
     * @param name The breed name to validate
     * @param type The type of name being validated (e.g., "breed", "sub-breed") for error messages
     * @return InvalidBreedError if validation fails, null if valid
     */
    fun validate(name: String, type: String = "breed"): DogApiError.InvalidBreedError? {
        return when {
            name.isBlank() ->
                DogApiError.InvalidBreedError(name, "The $type name cannot be blank")
            name.any { it.isWhitespace() } ->
                DogApiError.InvalidBreedError(name, "The $type name cannot contain spaces")
            !name.all { it.isLetterOrDigit() || it == '-' } ->
                DogApiError.InvalidBreedError(name, "The $type name can only contain letters, numbers, and hyphens")
            else -> null
        }
    }
}
