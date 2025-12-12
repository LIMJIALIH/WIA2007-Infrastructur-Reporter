plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.infrastructureproject"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.infrastructureproject"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    // Gemini AI dependencies
    implementation("com.google.ai.client.generativeai:generativeai:0.2.2")
    implementation("com.google.guava:guava:31.0.1-android")
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    // For Hugging Face API calls
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    // For JSON parsing (MUST HAVE for Hugging Face)
    implementation("org.json:json:20230227")
    // For HTTP requests
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    // For JSON parsing (REQUIRED)
    implementation("org.json:json:20230227")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}

