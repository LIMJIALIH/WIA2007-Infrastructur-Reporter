import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
}

val localPropsFile = rootProject.file("local.properties")
val localProps = Properties().apply {
    if (localPropsFile.exists()) {
        load(FileInputStream(localPropsFile))
    }
}
val geminiKeyFromLocal: String = localProps.getProperty("GEMINI_API_KEY", "")

android {
    namespace = "com.example.infrastructurereporter"
    compileSdk = 36

    buildFeatures {
        buildConfig = true   // <-- ENABLE custom BuildConfig fields
    }

    defaultConfig {
        applicationId = "com.example.infrastructurereporter"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Expose key from local.properties to BuildConfig for debug/dev.
        // Must be a Java string literal, so wrap in quotes.
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKeyFromLocal\"")
    }

    buildTypes {
        getByName("debug") {
            // debug uses value from defaultConfig (geminiKeyFromLocal)
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Override release to an empty string (do NOT ship real key)
            buildConfigField("String", "GEMINI_API_KEY", "\"\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/*.SF"
            excludes += "/META-INF/*.DSA"
            excludes += "/META-INF/*.RSA"
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.google.genai:google-genai:1.29.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
