plugins {
    alias(libs.plugins.android.application)
}

android {
    // this is the main app package
    namespace = "com.bloxstrap.client"
    compileSdk = 35

    defaultConfig {
        // this needs to match the namespace
        applicationId = "com.bloxstrap.client"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.9"

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
    buildFeatures {
        // this lets us use viewbinding, its pretty neat
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
