plugins {
    id("com.android.application")
}

val releaseSigningVariables = mapOf(
    "storeFile" to System.getenv("ANDROID_SIGNING_STORE_FILE"),
    "storePassword" to System.getenv("ANDROID_SIGNING_STORE_PASSWORD"),
    "keyAlias" to System.getenv("ANDROID_SIGNING_KEY_ALIAS"),
    "keyPassword" to System.getenv("ANDROID_SIGNING_KEY_PASSWORD"),
)

android {
    namespace = "io.github.transactionbridge"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.transactionbridge"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        if (releaseSigningVariables.values.all { !it.isNullOrBlank() }) {
            create("release") {
                storeFile = file(releaseSigningVariables.getValue("storeFile")!!)
                storePassword = releaseSigningVariables.getValue("storePassword")
                keyAlias = releaseSigningVariables.getValue("keyAlias")
                keyPassword = releaseSigningVariables.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

dependencies {
    implementation("io.github.quintavallechristian:transaction-parsers:0.1.0")
    testImplementation("junit:junit:4.13.2")
}
