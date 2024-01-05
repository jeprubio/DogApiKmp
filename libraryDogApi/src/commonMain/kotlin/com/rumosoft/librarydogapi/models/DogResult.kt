package com.rumosoft.librarydogapi.models

import kotlinx.serialization.Serializable

@Serializable
data class BreedsResult(
    val message: Map<String, List<String>>,
    val status: String
)

@Serializable
data class SubBreedsResult(
    val message: List<String>,
    val status: String
)

@Serializable
data class Breed(
    val name: String,
    val subBreeds: List<String>,
)

@Serializable
data class BreedImagesResult(
    val message: List<String>,
    val status: String
)
