plugins {
    id("com.android.application")
}

android {
    namespace = "com.neissa.twofa2notification"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.neissa.twofa2notification"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-auth:22.0.0")
    testImplementation("junit:junit:4.13.2")
}
