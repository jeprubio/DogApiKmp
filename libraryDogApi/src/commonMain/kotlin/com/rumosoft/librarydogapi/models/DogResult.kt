package com.rumosoft.librarydogapi.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
public data class Breed(
    val name: String,
    val subBreeds: List<String>,
)

@Serializable
internal data class DogApiStatusResult(
    val status: String,
    val message: JsonElement? = null,
)

@Serializable
internal data class BreedsResult(
    val message: Map<String, List<String>>,
    val status: String
)

@Serializable
internal data class SubBreedsResult(
    val message: List<String>,
    val status: String
)

@Serializable
internal data class RandomImageResult(
    val message: String,
    val status: String
)

@Serializable
internal data class BreedImagesResult(
    val message: List<String>,
    val status: String
)
