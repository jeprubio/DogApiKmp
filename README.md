# Dog API KMP

A Kotlin Multiplatform project demonstrating how to create and publish a shared library for Android and iOS.

## 📚 Project Structure

This project consists of two main modules:

### `/composeApp` - Sample Application
A Compose Multiplatform application that demonstrates how to use the Dog API library. It includes:
- Multiple screens showcasing different API endpoints
- Navigation with Voyager
- Shared UI code for Android and iOS
- Example integration patterns

### `/libraryDogApi` - Shared Library
A production-ready Kotlin Multiplatform library for accessing the Dog CEO API.

**✨ Features:**
- 🔄 Multiplatform support (Android, iOS)
- 🎯 Type-safe APIs on Kotlin and Swift alike
- 🧪 Fully testable with protocol-based design
- 📱 iOS-friendly: native Swift `async throws` with typed results, plus callback patterns
- 📖 Comprehensive documentation
- 🚀 Published via Maven and Swift Package Manager

**📖 [View Library Documentation →](./libraryDogApi/README.md)**

### Running the Android App
1. In Android Studio select any android emulator and composeApp in the run configurations once synced.
2. Press the Play button.

### Running the iOS App
1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Select a simulator or device
3. Run the project

### Running the Tests

Tests live in the `commonTest` source sets of the two Kotlin Multiplatform modules and use
`kotlin.test` with Kotest assertions. Run everything from the project root:

```bash
./gradlew allTests
```

Or one module at a time:

| Command | What it runs |
| --- | --- |
| `./gradlew :libraryDogApi:allTests` | Library tests: `DogApiTest`, `BreedNameValidatorTest` |
| `./gradlew :composeApp:allTests` | Compose UI tests: `MainScreenTest`, `BreedInputTest` |

While iterating, narrow the run to a single class or test:

```bash
# One class
./gradlew :libraryDogApi:iosSimulatorArm64Test \
    --tests "com.rumosoft.librarydogapi.DogApiTest"

# One test, or a group of tests by prefix
./gradlew :libraryDogApi:iosSimulatorArm64Test \
    --tests "com.rumosoft.librarydogapi.DogApiTest.cancellation*"
```

Results are written to `<module>/build/reports/tests/allTests/index.html`, with the raw XML in
`<module>/build/test-results/`.

#### Two things to know before you trust a green build

**The tests currently run on iOS only.** `allTests` resolves to `iosSimulatorArm64Test` and
nothing else, because neither module enables Android host tests. Gradle reports this on every
invocation:

```
WARNING: The 'commonTest' source directory exists, but android host tests are not enabled.
To enable android host tests, add `withHostTest {}` to your android target configuration
in the Gradle build file.
```

The consequence is that the shared tests never exercise the Android OkHttp engine. Adding
`withHostTest {}` to each module's `android { }` block would run the same suite on the JVM as
well. Running `iosSimulatorArm64Test` requires macOS with Xcode installed; the `iosArm64`
target can only be compiled and linked, never executed on the host.

**A `--tests` filter that matches nothing passes silently.** Kotlin/Native test tasks do not
fail on an empty selection, so a typo in the filter reports `BUILD SUCCESSFUL` after running
zero tests. Check the test count in the report before concluding that a change is covered.

`./gradlew :androidApp:testDebugUnitTest` exists but reports `NO-SOURCE`, because `androidApp`
has no unit tests; its `androidTest` directory contains only a template instrumented test.

## 📦 Using the Library

### In Your Kotlin Project
```kotlin
dependencies {
    implementation("com.rumosoft.dogapi:libraryDogApi:0.9")
}
```

### In Your iOS Project
The library is available via Swift Package Manager. See the [library documentation](./libraryDogApi/README.md) for details.

## 📖 Learn More

- **Library API Documentation**: [libraryDogApi/README.md](./libraryDogApi/README.md)
- **Dog CEO API**: https://dog.ceo/dog-api/
- **Kotlin Multiplatform**: https://kotlinlang.org/docs/multiplatform.html
- **Compose Multiplatform**: https://www.jetbrains.com/compose-multiplatform/

## 🎯 Project Goals

This project was created as a proof of concept to demonstrate:
- Creating a reusable Kotlin Multiplatform library
- Best practices for iOS-friendly API design
- Testing strategies for multiplatform code
- Publishing and consuming KMP libraries

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License.
