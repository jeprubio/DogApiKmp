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

> The published version is defined by `libraryDogApi` in
> `gradle/libs.versions.toml`, which is the source of truth. Use the value from
> the latest release rather than assuming the snippet above is current.

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

#### Logging

By default the library is silent (`NoOpDogApiLogger`). Pass a `DogApiLogger` to `createDefault` to see request/response logs. Each call logs its intent and the HTTP status at debug level; unexpected errors are logged at error level (`InvalidBreedError` — a user typing a wrong breed — is intentionally logged at debug level only).

**Android-only app (Kotlin)**

Use Android's built-in `Log` directly:

```kotlin
val api = DogApi.createDefault(
    logger = object : DogApiLogger {
        override fun d(msg: String) = Log.d("DogApi", msg)
        override fun e(msg: String, throwable: Throwable?) = Log.e("DogApi", msg, throwable)
    }
)
```

**iOS-only app (consuming the XCFramework from Swift)**

`DogApiLogger` is a Kotlin interface, which Swift cannot implement directly. Create a small bridge class in the `iosMain` Kotlin source set of the library (or your own thin wrapper framework):

```kotlin
// iosMain/kotlin/com/rumosoft/librarydogapi/OSLogLogger.kt
import platform.Foundation.NSLog

class OSLogLogger : DogApiLogger {
    override fun d(msg: String) = NSLog("[DogApi] %s", msg)
    override fun e(msg: String, throwable: Throwable?) = NSLog("[DogApi] ERROR %s", msg)
}
```

Then pass it from Swift:

```swift
import LibraryDogApi

let api = DogApi.Companion().createDefault(logger: OSLogLogger())
```

**Kotlin Multiplatform app (shared Kotlin code)**

Use [Napier](https://github.com/AAkira/Napier), which works on both Android and iOS from shared Kotlin code:

```kotlin
// In commonMain (or wherever you create the DogApi instance)
val api = DogApi.createDefault(
    logger = object : DogApiLogger {
        override fun d(msg: String) = Napier.d(msg, tag = "DogApi")
        override fun e(msg: String, throwable: Throwable?) = Napier.e(msg, throwable, tag = "DogApi")
    }
)
```

Napier must be initialised once per platform before any logs are emitted:

```kotlin
// Android — Application.onCreate()
Napier.base(DebugAntilog())
```

```swift
// iOS — iOSApp.swift or AppDelegate
NapierProxyKt.debugBuild()
```

**Expected output (all platforms)**

```
D/DogApi: Fetching all images for breed 'husky'
D/DogApi: GET https://dog.ceo/api/breed/husky/images → 200
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

Extension functions provide callback-based APIs. With SKIE enabled, these are exposed as native Swift extensions directly on the `DogApiClient` protocol.

Each function returns a `Job` so you can cancel the request before it completes — for example when a view is dismissed.

```swift
import LibraryDogApi

let api = DogApi.Companion().createDefault()

// Fetch breeds with callback — store the Job to cancel later
let breedsJob = api.breeds { [weak self] result in
    if let breeds = result.getOrNull() {
        for breed in breeds {
            print("\(breed.name)")
        }
    }
}

// Random image with callback
let imageJob = api.randomImage { [weak self] result in
    if let imageUrl = result.getOrNull() {
        print("Image: \(imageUrl)")
    }
}

// Random image for a specific breed with callback
let huskyJob = api.randomImageForBreed(breed: "husky") { [weak self] result in
    if let imageUrl = result.getOrNull() {
        print("Husky image: \(imageUrl)")
    }
}

// Cancel any in-flight request (e.g. in deinit or onDisappear)
breedsJob.cancel(message: nil)
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
```

#### Logging

See the [Logging](#logging) section under *Android / Kotlin* above — the iOS-only and KMP scenarios are both covered there.

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

### DogApiLogger

A simple logging interface with two methods:

- `d(msg: String)` — debug-level message (requests, responses, expected user errors such as `InvalidBreedError`)
- `e(msg: String, throwable: Throwable?)` — error-level message (network failures, server errors, parsing errors)

The default implementation is `NoOpDogApiLogger`, which silently discards all output. See the [Logging](#logging) section above for usage examples.

### MockDogApiClient


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
- `RemoteApiError` - Dog CEO API-level errors returned in a response body
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
    expectSuccess = true
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
        is DogApiError.RemoteApiError -> // Handle Dog CEO API-level errors
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

