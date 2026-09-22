plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.lvlaanu.mediastarremote"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lvlaanu.mediastarremote"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        getByName("debug") {
            // A debug keystore committed with the project, rather than the one
            // AGP auto-generates in ~/.android.
            //
            // The auto-generated one is written by whichever JDK happens to
            // create it first. A JDK new enough to use PBMAC1 produces a
            // keystore that older JDKs cannot read at all, which fails the
            // build with "Algorithm HmacPBE1.2.840.113549.1.5.14 not
            // available" the next time a different JDK signs the APK. That
            // happens routinely when Android Studio uses its bundled runtime
            // and the command line uses JAVA_HOME.
            //
            // This keystore is written with the legacy SHA-1 MAC, which every
            // JDK from 8 onward reads, so the debug build signs identically
            // everywhere. The credentials below are Android's standard public
            // debug credentials and protect nothing; never use this config for
            // a release build.
            val projectDebugKeystore = project.file("keystore/debug.keystore")
            if (projectDebugKeystore.exists()) {
                storeFile = projectDebugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            // R8 is enabled but the app has no reflection-sensitive code beyond
            // kotlinx.serialization, which ships its own consumer rules.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    // Pulled in transitively by lifecycle, but the IR transmit thread depends
    // on asCoroutineDispatcher directly, so declare it rather than inherit it.
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
