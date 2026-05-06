plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 36
    namespace = "com.example.grabstandalone"

    defaultConfig {
        applicationId = "com.example.grabstandalone"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "META-INF/atomicfu.kotlin_module"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    // ── Realtek SDK (local AAR/JAR files) ──────────────────────────────────
    implementation(files("libs/rtk-core-ktx-1.7.83.jar"))
    implementation(files("libs/rtk-dfu-3.14.36.jar"))
    implementation(files("libs/rtk-support-1.7.89.aar"))
    implementation(files("libs/rtk-support-debugger-1.2.41.aar"))
    implementation(files("libs/rtk-androidx-core-1.0.26.jar"))
    implementation(files("libs/rtk-audioconnect-core-1.9.10.jar"))
    implementation(files("libs/rtk-audioconnect-common-1.15.10.aar"))
    implementation(files("libs/rtk-audioconnect-smartwear-1.8.33.aar"))

    // ── AndroidX ───────────────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.4")

    // ── Material ───────────────────────────────────────────────────────────
    implementation("com.google.android.material:material:1.13.0")

    // ── Preference (required by rtk-support-debugger AAR) ─────────────────
    implementation("androidx.preference:preference-ktx:1.2.1")

    // ── Gson (required by Realtek SDK) ────────────────────────────────────
    implementation("com.google.code.gson:gson:2.11.0")

    // ── RecyclerView ───────────────────────────────────────────────────────
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // ── OkHttp (required by Realtek SDK) ──────────────────────────────────
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ── Coroutines ─────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.3.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
