import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)

    androidLibrary {
        compileSdk { version = release(libs.versions.android.compileSdk.get().toInt())}
        minSdk { version = release(libs.versions.android.minSdk.get().toInt())}
        namespace = "com.telefonica.dogapi.composeapp"
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        
        androidMain.dependencies {
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(project(":libraryDogApi"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3.multiplatform)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.napier)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.transitions)
        }

        // Adds common test dependencies
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
        }

        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.ui.test.junit4.android)
        }
    }
}
