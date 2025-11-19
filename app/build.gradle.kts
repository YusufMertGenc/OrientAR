plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android { //testarda
    namespace = "com.example.ardeneme"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ardeneme"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

// ARCore sürümünü kilitle – SceneView ile çakışmasın
configurations.configureEach {
    resolutionStrategy { force("com.google.ar:core:1.44.0") }
}

dependencies {
    implementation("com.google.ar:core:1.44.0")

    // SceneView – View tabanlı AR
    val sceneviewVersion = "2.3.0"
    implementation("io.github.sceneview:arsceneview:$sceneviewVersion")
    implementation("io.github.sceneview:sceneview:$sceneviewVersion")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
}
