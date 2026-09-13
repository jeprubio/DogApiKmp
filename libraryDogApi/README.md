# Dog API Kotlin Multiplatform Library

A Kotlin Multiplatform library providing easy access to the [Dog CEO API](https://dog.ceo/dog-api/).

## Features

✅ **Multiplatform** - Works on Android, iOS, and other Kotlin platforms  
✅ **Type-safe** - Strongly typed APIs on Kotlin *and* Swift  
✅ **Typed Error Handling** - Specific error types (NetworkError, HttpError, etc.) for better error handling  
✅ **Testable** - Protocol-based design for easy mocking  
✅ **iOS-friendly** - Native Swift `async throws` with typed results, plus callback patterns  
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

// Every call returns its value or throws a DogApiError
try {
    val breeds: List<Breed> = api.breeds()
    breeds.forEach { breed ->
        println("${breed.name}: ${breed.subBreeds}")
    }

    val imageUrl: String = api.randomImage()
    println("Random dog image: $imageUrl")

    val huskyImage: String = api.randomImage("husky")
    val allHuskyImages: List<String> = api.breedImages("husky")
    val subBreeds: List<String> = api.listSubBreeds("hound")
} catch (error: DogApiError) {
    println("Failed: ${error.message}")
}
```

#### Prefer a `Result`?

Wrap the call. The library does not return `kotlin.Result` itself, because `Result` is an inline
value class that Kotlin/Native erases to `Any?` when exporting to Objective-C, which would strip
every type from the Swift API:

```kotlin
val breeds: Result<List<Breed>> = runCatching { api.breeds() }
breeds.onSuccess { /* … */ }.onFailure { /* … */ }
```

> **Inside a cancellable coroutine, catch `DogApiError` instead.** `runCatching` also catches
> `CancellationException`, so using it in a `viewModelScope` or a `LaunchedEffect` swallows
> cancellation and lets the coroutine continue after it was cancelled.

#### Dependency Injection

For better testability, use the `DogApiClient` interface:

```kotlin
class BreedRepository(
    private val dogApi: DogApiClient  // Use interface, not concrete class
) {
    suspend fun getBreeds(): List<Breed> = dogApi.breeds()
}

// In production
val repository = BreedRepository(DogApi.createDefault())

// In tests
val repository = BreedRepository(
    MockDogApiClient(breeds = listOf(Breed("husky", emptyList())))
)
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

let api = DogApi.companion.createDefault(logger: OSLogLogger())
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

With SKIE enabled, every function arrives as native `async throws` with fully typed results —
`[Breed]`, `String`, `[String]` — not `Any?`:

```swift
import LibraryDogApi

let api: DogApiClient = DogApi.companion.createDefault()

Task {
    do {
        let breeds: [Breed] = try await api.breeds()
        for breed in breeds {
            print("\(breed.name): \(breed.subBreeds)")
        }

        let imageUrl: String = try await api.randomImage()
        print("Random dog: \(imageUrl)")

        let husky: String = try await api.randomImage(breed: "husky")
        let huskyImages: [String] = try await api.breedImages(breed: "husky")
        let subBreeds: [String] = try await api.listSubBreeds(breed: "hound")
        let afghan: [String] = try await api.subBreedImages(breed: "hound", subBreed: "afghan")
    } catch let error as DogApiError {
        print("Dog API failed: \(error.message ?? "")")
    } catch {
        print("Unexpected: \(error)")
    }
}
```

Cancelling the enclosing Swift `Task` cancels the request and surfaces as `CancellationError`,
not as a Dog API failure:

```swift
let task = Task {
    do {
        _ = try await api.breeds()
    } catch is CancellationError {
        print("cancelled")
    } catch {
        print("failed: \(error)")
    }
}
task.cancel()
```

#### Using Completion Handlers (Alternative)

Extension functions provide callback-based APIs. With SKIE enabled, these are exposed as native
Swift extensions directly on the `DogApiClient` protocol.

Each callback receives either a value or a `DogApiError`, never both. Each function returns a
`Job` so you can cancel the request before it completes — for example when a view is dismissed.
The callback is **not** invoked for a cancelled request.

```swift
import LibraryDogApi

let api = DogApi.companion.createDefault()

// Fetch breeds with callback — store the Job to cancel later
let breedsJob = api.breeds { breeds, error in
    if let error {
        print("failed: \(error.message ?? "")")
        return
    }
    for breed in breeds ?? [] {
        print(breed.name)
    }
}

// Random image with callback
let imageJob = api.randomImage { imageUrl, error in
    print(imageUrl ?? error?.message ?? "")
}

// Random image for a specific breed with callback
let huskyJob = api.randomImageForBreed(breed: "husky") { imageUrl, error in
    print(imageUrl ?? error?.message ?? "")
}

// Cancel any in-flight request (e.g. in deinit or onDisappear).
// The cast is required because `cause` is an optional Kotlin type.
breedsJob.cancel(cause: nil as KotlinCancellationException?)
```

> Prefer `async/await` where you can. It gives you cancellation through the Swift `Task` API and
> avoids the bridged `Job` type entirely.

#### Protocol-based Dependency Injection

```swift
final class BreedViewModel {
    private let dogApi: DogApiClient

    init(dogApi: DogApiClient) {
        self.dogApi = dogApi
    }

    func loadBreeds() async -> [Breed] {
        do {
            return try await dogApi.breeds()
        } catch {
            return []
        }
    }
}

// In production
let viewModel = BreedViewModel(dogApi: DogApi.companion.createDefault())

// In tests
let viewModel = BreedViewModel(
    dogApi: MockDogApiClient(breeds: [Breed(name: "husky", subBreeds: ["siberian"])])
)
```

#### Logging

See the [Logging](#logging) section under *Android / Kotlin* above — the iOS-only and KMP scenarios are both covered there.

## API Reference

### DogApiClient

The main interface for accessing the Dog API:

- `breeds(): List<Breed>` - Get all breeds with their sub-breeds
- `randomImage(): String` - Get a random dog image URL
- `randomImage(breed: String): String` - Get a random image for a specific breed
- `breedImages(breed: String): List<String>` - Get all images for a breed
- `subBreedImages(breed: String, subBreed: String): List<String>` - Get all images for a sub-breed
- `listSubBreeds(breed: String): List<String>` - Get all sub-breeds for a breed

Every method returns its value or throws a [`DogApiError`](#dogapierror). Kotlin callers who want
a `Result` wrap the call in `runCatching { }`; Swift callers get `async throws` and `catch`.

### DogApiLogger

A simple logging interface with two methods:

- `d(msg: String)` — debug-level message (requests, responses, expected user errors such as `InvalidBreedError`)
- `e(msg: String, throwable: Throwable?)` — error-level message (network failures, server errors, parsing errors)

The default implementation is `NoOpDogApiLogger`, which silently discards all output. See the [Logging](#logging) section above for usage examples.

### MockDogApiClient

A `DogApiClient` implementation for tests. Each endpoint is stubbed by a pair of parameters: the
value to return, or the `DogApiError` to throw. The error wins when both are given, and an
endpoint with neither throws `UnknownError` naming the endpoint, so an unconfigured call fails
loudly rather than returning something misleading.

```kotlin
val mockApi = MockDogApiClient(
    breeds = listOf(Breed("husky", emptyList())),
    randomImage = "https://example.com/dog.jpg",
)

val failing = MockDogApiClient(
    breedsError = DogApiError.NetworkError("offline"),
)
```

The two `randomImage` overloads are configured independently: `randomImage` backs the
no-argument overload and `randomImageForBreed` backs the breed-specific one, which falls back to
`randomImage` when `randomImageForBreed` is not supplied.

From Swift the same parameters are fully typed, and unspecified ones can be omitted:

```swift
let mockApi = MockDogApiClient(
    breeds: [Breed(name: "husky", subBreeds: [])],
    randomImage: "https://example.com/dog.jpg"
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

This section covers testing *your* code against this library. To run the library's own test
suite, see [Running the Tests](../README.md#running-the-tests) in the root README.

### Unit Testing

```kotlin
class DogApiTest {
    @Test
    fun testBreedsSuccess() = runTest {
        val mockApi = MockDogApiClient(
            breeds = listOf(Breed("husky", listOf("siberian")))
        )

        val breeds = mockApi.breeds()

        assertEquals("husky", breeds.first().name)
    }

    @Test
    fun testBreedsFailure() = runTest {
        val mockApi = MockDogApiClient(
            breedsError = DogApiError.NetworkError("offline")
        )

        assertFailsWith<DogApiError.NetworkError> { mockApi.breeds() }
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
- **Exception-based error handling**: methods return their value or throw, which maps cleanly to
  Swift `throws`. `kotlin.Result` is deliberately avoided because Kotlin/Native erases inline
  value classes to `Any?` in the Objective-C export
- **Typed errors**: Uses `DogApiError` sealed class for specific, type-safe error handling, and
  SKIE turns it into an exhaustively switchable Swift enum via `onEnum(of:)`
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
try {
    val breeds = api.breeds()
    // Handle success
} catch (error: DogApiError) {
    when (error) {
        is DogApiError.NetworkError -> {} // Handle network issues
        is DogApiError.HttpError -> {} // Handle HTTP errors (error.statusCode available)
        is DogApiError.RemoteApiError -> {} // Handle Dog CEO API-level errors
        is DogApiError.SerializationError -> {} // Handle parsing errors
        is DogApiError.InvalidBreedError -> {} // Handle a bad breed name
        is DogApiError.UnknownError -> {} // Handle anything else
    }
}
```

Catching `DogApiError` rather than `Throwable` (or using `runCatching`) keeps coroutine
cancellation propagating, which is what you want inside a `viewModelScope` or `LaunchedEffect`.

#### iOS/Swift

SKIE exposes the sealed `DogApiError` hierarchy through `onEnum(of:)`, which gives an
exhaustive Swift `switch`. Each case carries the concrete subclass, so its own properties —
`statusCode`, `breedName`, `status`, `apiMessage` — are available directly:

```swift
do {
    let breeds = try await api.breeds()
    // Handle success
} catch let error as DogApiError {
    switch onEnum(of: error) {
    case .networkError(let e):
        print("offline: \(e.message ?? "")")          // show retry or offline mode
    case .httpError(let e):
        print("HTTP \(e.statusCode)")                 // status code is typed Int32
    case .invalidBreedError(let e):
        print("no such breed: \(e.breedName)")
    case .remoteApiError(let e):
        print("\(e.status): \(e.apiMessage ?? "")")
    case .serializationError(let e):
        print("parse failure: \(e.message ?? "")")
    case .unknownError(let e):
        print("unknown: \(e.message ?? "")")
    }
}
```

### 3. Use Mocks in Tests

```kotlin
val mockApi = MockDogApiClient(breeds = testBreeds)
```

## Contributing

Issues and pull requests are welcome!

## License

This project is licensed under the MIT License.

