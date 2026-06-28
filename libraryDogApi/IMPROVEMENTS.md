# Possible Improvements — libraryDogApi

This document lists candidate improvements for the `libraryDogApi` module. It is
analysis only; none of the items below have been applied. Each entry states the
observation, why it matters, and a suggested direction. Items are grouped by
theme and ordered roughly by impact within each group.

## Correctness and robustness

### 1. Response body is parsed twice on every request — RESOLVED
The body string was tokenized twice: once as `DogApiStatusResult` inside
`validateStatus(body)` and again as the target type `T`. `getAndLog` now parses
the raw body into a `JsonElement` exactly once and decodes both the status check
and the typed result from that already-parsed tree:

```kotlin
val element = DogJson.parseToJsonElement(response.bodyAsText())
validateStatus(element)                          // reads the parsed tree
return DogJson.decodeFromJsonElement<T>(element) // reuses the same tree
```

This removes the duplicate string-tokenization pass while preserving all
existing semantics (success short-circuit, `RemoteApiError` mapping, and
`SerializationError` on malformed JSON). A further refinement would be to read
`status` directly from the typed result and drop `DogApiStatusResult` entirely,
since every result type already carries a `status` field.

### 2. ContentNegotiation is installed but not used for response decoding — RESOLVED
The shared client installs `ContentNegotiation { json(DogJson) }`, yet responses
were read with `bodyAsText()` and parsed manually, so the plugin contributed
nothing on the response path. `getAndLog` now reads the body through
`response.body<JsonElement>()`, which uses the installed plugin (configured with
`DogJson`) to parse the response. The typed result is then decoded from that same
`JsonElement` via `decodeFromJsonElement<T>`, preserving the single-parse fix
from item 1. Malformed JSON now surfaces as a `JsonConvertException`, which the
error mapper already routes to `SerializationError`.

### 3. Base URL with a trailing slash produces malformed URLs — RESOLVED
The constructor now normalises `baseUrl` once via `baseUrl.trimEnd('/')` and
stores it in a private property, so a caller passing
`createDefault(baseUrl = "https://example.com/api/")` no longer yields a double
slash. A regression test (`trailing slash in base URL does not produce a double
slash`) asserts the exact constructed URL.

### 4. Breed names are concatenated into the URL without encoding — RESOLVED
Breed and sub-breed names now pass through a private `String.toPathSegment()`
helper that lower-cases and percent-encodes each segment via Ktor's
`encodeURLPathPart()`. URL safety is therefore explicit at the call site and no
longer depends on `BreedNameValidator`'s character rules, so it remains correct
if the validator's restrictions are later relaxed.

## API design

### 5. `MockDogApiClient` cannot distinguish the two `randomImage` overloads — RESOLVED
A separate `randomImageForBreedResult` field now backs `randomImage(breed)`,
while `randomImageResult` backs the no-argument `randomImage()`. The
breed-specific overload falls back to `randomImageResult` when
`randomImageForBreedResult` is not supplied, so existing call sites keep working.
A further refinement would be per-breed result maps for finer control.

### 6. `MockDogApiClient` records nothing
The mock returns canned results but does not capture the arguments it was called
with or how many times. Consumers verifying that, for example, `breedImages`
was invoked with the expected breed must wrap it themselves. Recording calls
(invocation count and last arguments) would make it a more complete test double.

### 7. `HttpError` does not retain the response body
`DogApiError.HttpError` carries the status code and a generic message but not the
server's response body. For diagnosing 4xx/5xx responses from the Dog CEO API,
exposing the body (or a truncated form) would aid debugging without leaking it
into logs by default.

### 8. No retry or backoff for transient failures — RESOLVED
The shared client now installs Ktor's `HttpRequestRetry` plugin with
`retryOnExceptionOrServerErrors(maxRetries = DEFAULT_MAX_RETRIES)` and
`exponentialDelay()`. All operations are idempotent GET requests, so retrying
transient I/O errors and 5xx responses is safe; 4xx responses (such as an invalid
breed) are not retried. The retry count is exposed as the public constant
`DEFAULT_MAX_RETRIES` (2). A regression test confirms a transient 500 is retried
and then succeeds. Callers who inject their own `HttpClient` remain in full
control of retry behaviour.

### 9. No client-side caching of the breeds list
`breeds()` returns a comparatively static dataset that many consumers fetch
repeatedly. An optional in-memory cache (with a configurable TTL) would reduce
redundant network calls. This should remain opt-in to keep the default behaviour
predictable.

## iOS / multiplatform integration

### 10. Callback extensions create an unmanaged `CoroutineScope` per call
Each extension in `DogApiExtensions.kt` launches into a freshly created
`CoroutineScope(Dispatchers.Main)` that is never cancelled as a scope:

```kotlin
public fun DogApiClient.breeds(onComplete: (Result<List<Breed>>) -> Unit): Job =
    CoroutineScope(Dispatchers.Main).launch { onComplete(breeds()) }
```

The returned `Job` allows cancelling the individual request, which is the
documented contract, but the enclosing scope leaks. Sharing a single
library-owned scope (for example a `SupervisorJob` on the main dispatcher), or
documenting the lifecycle explicitly, would tighten structured concurrency on
iOS.

### 11. Callbacks are not invoked on cancellation
When the returned `Job` is cancelled, `onComplete` is never called. Swift
callers relying solely on the callback receive no terminal signal. Consider
documenting this clearly, or optionally delivering a cancellation `Result` so
callers can clean up.

## Validation

### 12. `isLetterOrDigit` accepts non-ASCII characters — RESOLVED
`BreedNameValidator` now validates against an explicit ASCII set (`a-z`, `A-Z`,
`0-9`, `-`) via a private `Char.isAsciiAllowed()` helper, replacing the
Unicode-aware `isLetterOrDigit()`. Names such as "pügs" are now rejected locally
with the "can only contain" message instead of failing remotely. A regression
test covers the non-ASCII case.

### 13. No length bound on breed names
The validator does not cap length. A defensive upper bound would prevent
pathological inputs from reaching the network layer.

## Resource management

### 14. The shared `HttpClient` is never closed
`sharedClient` is a lazily created singleton with no `close()` path. For a
long-lived mobile process this is acceptable, but the absence of any teardown
hook should be documented, and a `close()`/`AutoCloseable` affordance considered
for hosts that create and dispose clients (for example tests or short-lived
tools). Caller-injected clients are already the caller's responsibility.

## Documentation consistency

### 15. README version may drift from the build — RESOLVED
The installation snippet still shows `libraryDogApi:0.9` (which currently matches
`libs.versions.libraryDogApi`), but the README now states that
`gradle/libs.versions.toml` is the source of truth and directs readers to the
latest release for the current coordinate, reducing the chance of silent drift.

### 16. README `DogApi(httpClient)` example omits status-validation setup — RESOLVED
The integration-testing snippet now sets `expectSuccess = true` on the client,
matching the production shared client and the test helper. This makes the example
behave like the tested configuration, where non-2xx responses throw and are
mapped to typed errors.

## Testing

### 17. `MockDogApiClient` and the iOS callback extensions are untested
The common test suite thoroughly covers `DogApi` and `BreedNameValidator`, but
`MockDogApiClient` has no tests, and `DogApiExtensions` (iosMain) has none.
Adding coverage for the mock's default-failure behaviour and for the callback
bridges (where the main dispatcher can be controlled) would close the gap.

### 18. No test asserts URL construction — PARTIAL
The trailing-slash regression test added for item 3 now asserts an exact
constructed URL (`https://example.com/api/breeds/list/all`). Further coverage
asserting the encoded path for a breed and a sub-breed (item 4) would close this
fully.

---

## Suggested prioritisation

- **High impact, low effort:** all resolved (items 1, 2, 5, 15, 16).
- **Robustness:** items 3, 4, 8, and 12 resolved.
- **Nice to have:** items 6, 7, 9, 10, 11, 13, 14, 17, 18.