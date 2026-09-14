# Dog API Kotlin Multiplatform Library

A Kotlin Multiplatform library providing easy access to the [Dog CEO API](https://dog.ceo/dog-api/).

## Features

✅ **Multiplatform** - Android and iOS (`iosArm64`, `iosSimulatorArm64`)  
✅ **Type-safe** - Strongly typed APIs on Kotlin *and* Swift  
✅ **Typed Error Handling** - Specific error types (NetworkError, HttpError, etc.) for better error handling  
✅ **Testable** - Protocol-based design for easy mocking  
✅ **iOS-friendly** - Native Swift `async throws` with typed results and `Task` cancellation  
✅ **Clean iOS surface** - Ktor stays internal, so the framework exports no Ktor types  
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

Not published yet. The build configures kmmbridge's `spm()`, but `publishing` only targets
`mavenLocal()`, so no Swift Package is available to depend on. For now, build the framework from
this repository:

```bash
./gradlew :libraryDogApi:assembleLibraryDogApiXCFramework
```

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

#### Configuration

`createDefault` covers the common case and reuses a process-wide HTTP client, so it needs no
teardown:

```kotlin
val api = DogApi.createDefault()

// Point at an alternative endpoint or a local test server:
val local = DogApi.createDefault(baseUrl = "https://my-test-server.com/api")
```

For timeouts and retries, pass a `DogApiConfig` to `create`. That instance builds its own HTTP
client, so create it once and reuse it rather than calling `create` per request:

```kotlin
val api = DogApi.create(
    DogApiConfig(
        requestTimeoutMillis = 5_000L,
        maxRetries = 0,          // 0 disables retries
    )
)
```

| `DogApiConfig` parameter | Default |
| --- | --- |
| `baseUrl` | `https://dog.ceo/api` (`DogApi.DEFAULT_BASE_URL`) |
| `logger` | `NoOpDogApiLogger` (silent) |
| `connectTimeoutMillis` | `15_000` (`DogApi.DEFAULT_CONNECT_TIMEOUT_MS`) |
| `requestTimeoutMillis` | `30_000` (`DogApi.DEFAULT_REQUEST_TIMEOUT_MS`) |
| `socketTimeoutMillis` | `15_000` (`DogApi.DEFAULT_SOCKET_TIMEOUT_MS`) |
| `maxRetries` | `2` (`DogApi.DEFAULT_MAX_RETRIES`) — retries I/O errors and 5xx only, never 4xx |

From Swift the same API applies, with defaults filled in:

```swift
let api = DogApi.companion.create(
    config: DogApiConfig(requestTimeoutMillis: 5_000, maxRetries: 0)
)
```

> **Ktor is not part of the public API.** Passing your own `HttpClient` is not supported: it
> exported 74 `Ktor_*` types into the iOS framework header and contradicted Ktor being an
> `implementation` dependency. For anything `DogApiConfig` cannot express — a different engine,
> certificate pinning, a custom Ktor plugin — implement `DogApiClient` directly.

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

`DogApiLogger` is exported as an Objective-C protocol, so implement it in Swift directly — no
Kotlin bridge class needed:

```swift
import os
import LibraryDogApi

final class OSLogLogger: DogApiLogger {
    private let log = OSLog(subsystem: "com.example.app", category: "DogApi")

    func d(msg: String) {
        os_log("%{public}@", log: log, type: .debug, msg)
    }

    func e(msg: String, throwable: KotlinThrowable?) {
        os_log("%{public}@ %{public}@", log: log, type: .error, msg, throwable?.message ?? "")
    }
}

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

A `DogApiClient` implementation for tests. Each endpoint takes the value it should return, and
`error` makes every endpoint throw. An endpoint with no stub throws `UnknownError` naming it, so
an unconfigured call fails loudly rather than returning something misleading. For finer control —
one endpoint succeeding while another fails — implement `DogApiClient` directly.

```kotlin
val mockApi = MockDogApiClient(
    breeds = listOf(Breed("husky", emptyList())),
    randomImage = "https://example.com/dog.jpg",
)

// `error` makes every endpoint throw:
val failing = MockDogApiClient(error = DogApiError.NetworkError("offline"))
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
        val mockApi = MockDogApiClient(error = DogApiError.NetworkError("offline"))

        assertFailsWith<DogApiError.NetworkError> { mockApi.breeds() }
    }
}
```

### Testing Against a Fake HTTP Layer

Substituting a Ktor `MockEngine` is not possible from outside the library, because `DogApi` no
longer accepts an `HttpClient` (see [Configuration](#configuration)). Test against the interface
instead — which is what the interface is for.

Use `MockDogApiClient` for canned responses, or implement `DogApiClient` when you need
behaviour rather than fixed values:

```kotlin
class FakeDogApi(private val calls: MutableList<String> = mutableListOf()) : DogApiClient {
    override suspend fun breeds(): List<Breed> {
        calls += "breeds"
        return listOf(Breed("husky", listOf("siberian")))
    }

    override suspend fun breedImages(breed: String): List<String> {
        calls += "breedImages($breed)"
        if (breed == "unknown") throw DogApiError.InvalidBreedError(breed)
        return listOf("https://example.com/$breed.jpg")
    }

    // …remaining members
}
```

If you genuinely need to exercise the real HTTP path — for example against a local server —
point `baseUrl` at it:

```kotlin
val api = DogApi.createDefault(baseUrl = "http://localhost:8080/api")
```

## Architecture
- **Interface-first design**: `DogApiClient` interface makes testing easy
- **Exception-based error handling**: methods return their value or throw, which maps cleanly to
  Swift `throws`. `kotlin.Result` is deliberately avoided because Kotlin/Native erases inline
  value classes to `Any?` in the Objective-C export
- **Typed errors**: Uses `DogApiError` sealed class for specific, type-safe error handling, and
  SKIE turns it into an exhaustively switchable Swift enum via `onEnum(of:)`
- **Dependency injection friendly**: depend on the `DogApiClient` interface
- **Implementation details stay internal**: Ktor appears in no public signature, so the iOS
  framework exports no `Ktor_*` types
- **Configurable without leaking**: `DogApiConfig` exposes timeouts, retries, base URL and logger
- **Efficient resource management**: `createDefault` shares one HttpClient process-wide

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

