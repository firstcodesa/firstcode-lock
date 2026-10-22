plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "sa.firstcode.lock"
    compileSdk = 35
    defaultConfig {
        applicationId = "sa.firstcode.lock"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
    lint { abortOnError = true; checkReleaseBuilds = true }
}
dependencies { testImplementation("junit:junit:4.13.2") }
