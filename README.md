# Dog API KMP

A Kotlin Multiplatform project demonstrating how to create and publish a shared library for Android and iOS.

## 📚 Project Structure

The library lives in `/libraryDogApi`. The rest is a sample app that consumes it.

### `/libraryDogApi` - the library
A Kotlin Multiplatform library for the Dog CEO API, targeting Android and iOS.

### `/composeApp` - shared sample UI
A Compose Multiplatform *library* module holding the sample screens shared by both apps:
- Multiple screens showcasing different API endpoints
- Navigation with Jetpack Navigation 3
- Example integration patterns

### `/androidApp` - Android application
The runnable Android app. Thin wrapper that hosts `composeApp`'s UI and initialises Napier.

### `/iosApp` - iOS application
The Xcode project. Hosts the same Compose UI through `MainViewController`.

**✨ Library features:**
- 🔄 Multiplatform support (Android, iOS)
- 🎯 Type-safe APIs on Kotlin and Swift alike
- 🧪 Fully testable with protocol-based design
- 📱 iOS-friendly: native Swift `async throws` with typed results and `Task` cancellation
- 📖 Comprehensive documentation
- 🚀 Publishable via Maven and Swift Package Manager (kmmbridge)

**📖 [View Library Documentation →](./libraryDogApi/README.md)**

### Running the Android App
1. In Android Studio select any Android emulator and the **androidApp** run configuration once synced.
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
| `./gradlew :libraryDogApi:allTests` | Library tests: `DogApiTest`, `BreedNameValidatorTest`, `MockDogApiClientTest` |
| `./gradlew :composeApp:allTests` | Compose UI tests: `MainScreenTest`, `BreedInputTest` |

While iterating, narrow the run to a class or a test:

```bash
./gradlew :libraryDogApi:iosSimulatorArm64Test --tests "com.rumosoft.librarydogapi.DogApiTest"
```

Reports land in `<module>/build/reports/tests/allTests/index.html`.

Two things worth knowing. `libraryDogApi` runs its suite twice — on the JVM
(`testAndroidHostTest`) and the iOS simulator — so both the OkHttp and Darwin engines are
covered; `composeApp` runs on iOS only, because its Compose tests would need Robolectric to run
as Android host tests. And a `--tests` filter that matches nothing still reports
`BUILD SUCCESSFUL`, so check the test count before trusting a green run.

## 📦 Using the Library

> **Not published to a public repository yet.** Publish locally with
> `./gradlew :libraryDogApi:publishToMavenLocal` and add `mavenLocal()` to the consumer's
> repositories, or build from source.

### In Your Kotlin Project
```kotlin
dependencies {
    implementation("com.rumosoft.dogapi:libraryDogApi:0.9")
}
```

### In Your iOS Project
kmmbridge's `spm()` can generate a Swift Package, but no package is published yet. For now,
consume the framework directly from an Xcode build of this repository.

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
