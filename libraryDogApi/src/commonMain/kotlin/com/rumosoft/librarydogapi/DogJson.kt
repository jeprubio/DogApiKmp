package com.rumosoft.librarydogapi

import kotlinx.serialization.json.Json

internal val DogJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}
