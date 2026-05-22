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
version = "0.9"

publishing {
    repositories {
        mavenLocal()
    }
}

kotlin {
    explicitApi()

    jvmToolchain(17)
    // withSourcesJar(publish = false)

    android {
        namespace = "com.rumosoft.dogapikmp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "LibraryDogApi"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization)
            implementation(libs.napier)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

kmmbridge {
    buildType.set(NativeBuildType.DEBUG)
    frameworkName.set("LibraryDogApi")

    mavenPublishArtifacts()
    spm()
}

skie {
    isEnabled = true
    build {
        produceDistributableFramework()
    }
}
