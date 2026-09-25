import co.touchlab.skie.configuration.DefaultArgumentInterop
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.serialization)
    id("maven-publish")
    alias(libs.plugins.kmmBridge)
    alias(libs.plugins.skie)
}

group = "com.rumosoft.dogapi"
version = libs.versions.libraryDogApi.get()

publishing {
    repositories {
        mavenLocal()
    }
}

kotlin {
    explicitApi()

    jvmToolchain(17)
    withSourcesJar(publish = true)

    android {
        namespace = "com.rumosoft.dogapikmp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTest {}
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "LibraryDogApi"
            isStatic = true
            binaryOption("bundleId", "com.rumosoft.dogapi.LibraryDogApi")
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.engine.defaults)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

kmmbridge {
    buildType.set(NativeBuildType.RELEASE)
    frameworkName.set("LibraryDogApi")

    mavenPublishArtifacts()
    spm()
}

skie {
    isEnabled = true
    build {
        produceDistributableFramework()
    }
    features {
        group {
            // Generates Swift overloads for Kotlin default arguments, so Swift callers can write
            // `DogApi.companion.createDefault()` and `MockDogApiClient(breeds:)` instead of
            // having to pass every parameter explicitly.
            DefaultArgumentInterop.Enabled(true)
            DefaultArgumentInterop.MaximumDefaultArgumentCount(12)
        }
    }
}
