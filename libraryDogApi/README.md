# Dog API Kotlin Multiplatform Library

A Kotlin Multiplatform library providing easy access to the [Dog CEO API](https://dog.ceo/dog-api/).

## Features

✅ **Multiplatform** - Works on Android, iOS, and other Kotlin platforms  
✅ **Type-safe** - Strongly typed APIs with proper Result types  
✅ **Typed Error Handling** - Specific error types (NetworkError, HttpError, etc.) for better error handling  
✅ **Testable** - Protocol-based design for easy mocking  
✅ **iOS-friendly** - Both async/await and callback patterns supported  
✅ **Well-documented** - Comprehensive documentation for all public APIs  

## Installation

### Gradle (Kotlin)

```kotlin
dependencies {
    implementation("com.rumosoft.dogapi:libraryDogApi:0.9")
}
```

### Swift Package Manager (iOS)

The library is published via SPM through kmmbridge. Check your SPM configuration.

## Usage

### Android / Kotlin

#### Basic Usage

```kotlin
import com.rumosoft.librarydogapi.DogApi

// Create API client
val api = DogApi.createDefault()

// Fetch all breeds
val breedsResult = api.breeds()
breedsResult.onSuccess { breeds ->
    breeds.forEach { breed ->
        println("${breed.name}: ${breed.subBreeds}")
    }
}

// Get random image
val imageResult = api.randomImage()
imageResult.onSuccess { imageUrl ->
    println("Random dog image: $imageUrl")
}

// Get breed-specific image
val huskyImageResult = api.randomImage("husky")

// Get all images for a breed
val allHuskyImages = api.breedImages("husky")

// List sub-breeds
val subBreeds = api.listSubBreeds("hound")
```

#### Dependency Injection

For better testability, use the `DogApiClient` interface:

```kotlin
class BreedRepository(
    private val dogApi: DogApiClient  // Use interface, not concrete class
) {
    suspend fun getBreeds() = dogApi.breeds()
}

// In production
val repository = BreedRepository(DogApi.createDefault())

// In tests
val repository = BreedRepository(MockDogApiClient(
    breedsResult = Result.success(listOf(
        Breed("husky", emptyList())
    ))
))
```

#### Custom Base URL (for testing or alternative endpoints)

```kotlin
val api = DogApi.createDefault(baseUrl = "https://my-test-server.com/api")
```

### iOS / Swift

#### Using Async/Await (Recommended)

With SKIE enabled, you can use Swift's native async/await:

```swift
import LibraryDogApi

let api = DogApi.Companion().createDefault()

Task {
    do {
        // Fetch breeds
        let breedsResult = try await api.breeds()
        if let breeds = breedsResult.getOrNull() {
            for breed in breeds {
                print("\(breed.name): \(breed.subBreeds)")
            }
        }
        
        // Get random image
        let imageResult = try await api.randomImage()
        if let imageUrl = imageResult.getOrNull() {
            print("Random dog: \(imageUrl)")
        }
        
        // Get breed-specific image
        let huskyResult = try await api.randomImage(breed: "husky")
        
    } catch {
        print("Error: \(error)")
    }
}
```

#### Using Completion Handlers (Alternative)

Extension functions provide callback-based APIs:

```swift
import LibraryDogApi

let api = DogApi.Companion().createDefault()

// Fetch breeds with callback
DogApiExtensionsKt.breeds(api) { result in
    if let breeds = result.getOrNull() {
        for breed in breeds {
            print("\(breed.name)")
        }
    }
}

// Random image with callback
DogApiExtensionsKt.randomImage(api) { result in
    if let imageUrl = result.getOrNull() {
        print("Image: \(imageUrl)")
    }
}
```

#### Protocol-based Dependency Injection

```swift
class BreedViewModel {
    private let dogApi: DogApiClient
    
    init(dogApi: DogApiClient) {
        self.dogApi = dogApi
    }
    
    func loadBreeds() async {
        let result = try? await dogApi.breeds()
        // Handle result
    }
}

// In production
let viewModel = BreedViewModel(dogApi: DogApi.Companion().createDefault())

// In tests
let mockApi = MockDogApiClient(
    breedsResult: KotlinResult(value: [
        Breed(name: "husky", subBreeds: [])
    ])
)
let viewModel = BreedViewModel(dogApi: mockApi)
```

## API Reference

### DogApiClient

The main interface for accessing the Dog API:

- `breeds()` - Get all breeds with their sub-breeds
- `randomImage()` - Get a random dog image URL
- `randomImage(breed: String)` - Get a random image for a specific breed
- `breedImages(breed: String)` - Get all images for a breed
- `subBreedImages(breed: String, subBreed: String)` - Get all images for a sub-breed
- `listSubBreeds(breed: String)` - Get all sub-breeds for a breed

All methods return `Result<T>` for safe error handling.

### MockDogApiClient

A mock implementation for testing:

```kotlin
val mockApi = MockDogApiClient(
    breedsResult = Result.success(listOf(Breed("husky", emptyList()))),
    randomImageResult = Result.success("https://example.com/dog.jpg")
)
```

### DogApiError

Custom error types for better error handling:

- `NetworkError` - Connection or network issues
- `HttpError` - HTTP status code errors
- `SerializationError` - JSON parsing errors
- `InvalidBreedError` - Invalid breed name
- `UnknownError` - Unexpected errors

## Testing

### Unit Testing

```kotlin
class DogApiTest {
    @Test
    fun testBreedsSuccess() = runTest {
        val mockApi = MockDogApiClient(
            breedsResult = Result.success(listOf(
                Breed("husky", listOf("siberian"))
            ))
        )
        
        val result = mockApi.breeds()
        assertTrue(result.isSuccess)
        assertEquals("husky", result.getOrNull()?.first()?.name)
    }
}
```

### Integration Testing

The library includes mock engines for testing with Ktor:

```kotlin
val mockEngine = MockEngine { request ->
    respond(
        content = """{"message": {...}, "status": "success"}""",
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json")
    )
}

val httpClient = HttpClient(mockEngine) {
    install(ContentNegotiation) {
        json()
    }
}

val api = DogApi(httpClient)
```

## Architecture
- **Interface-first design**: `DogApiClient` interface makes testing easy
- **Result-based error handling**: All methods return `Result<T>`
- **Typed errors**: Uses `DogApiError` sealed class for specific, type-safe error handling
- **Dependency injection friendly**: Constructor injection supported
- **Platform-specific extensions**: iOS callbacks in addition to suspend functions
- **Efficient resource management**: Uses a shared HttpClient for optimal performance

## Best Practices

### 1. Always Use the Interface

```kotlin
// ✅ Good - testable
class BreedViewModel(private val api: DogApiClient)

// ❌ Bad - harder to test
class BreedViewModel(private val api: DogApi)
```

### 2. Handle Errors Properly

#### Kotlin/Android

```kotlin
val result = api.breeds()
result.onSuccess { breeds ->
    // Handle success
}.onFailure { error ->
    when (error) {
        is DogApiError.NetworkError -> // Handle network issues
        is DogApiError.HttpError -> // Handle HTTP errors (status code available)
        is DogApiError.SerializationError -> // Handle parsing errors
        else -> // Handle other errors
    }
}
```

#### iOS/Swift

```swift
do {
    let result = try await api.breeds()
    // Handle success
} catch let error as DogApiError {
    switch error {
    case .networkError(let message, _):
        // Show retry or offline mode
    case .httpError(let statusCode, let message):
        // Handle HTTP errors
    case .serializationError(let message, _):
        // Handle parsing errors
    default:
        // Handle other errors
    }
}
```

### 3. Use Mocks in Tests

```kotlin
val mockApi = MockDogApiClient(
    breedsResult = Result.success(testBreeds)
)
```

## Contributing

Issues and pull requests are welcome!

## License

This project is licensed under the MIT License.

