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
- 🎯 Type-safe APIs with Result types
- 🧪 Fully testable with protocol-based design
- 📱 iOS-friendly with async/await and callback patterns
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
