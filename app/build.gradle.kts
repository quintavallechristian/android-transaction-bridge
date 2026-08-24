plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.transactionbridge"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.transactionbridge"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
