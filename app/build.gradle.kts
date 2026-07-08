import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

fun resolveApiBaseUrl(): String {
    localProperties.getProperty("api.base.url")?.trim()?.takeIf { it.isNotEmpty() }?.let { url ->
        return if (url.endsWith("/")) url else "$url/"
    }
    val mode = localProperties.getProperty("dev.api.mode")?.trim()?.lowercase() ?: "emulator"
    val host = localProperties.getProperty("dev.api.host")?.trim()?.takeIf { it.isNotEmpty() }
    val resolvedHost = when (mode) {
        "physical", "device", "real" -> host ?: error(
            "local.properties: set dev.api.host=<LAN-IP-máy-Mac> khi dev.api.mode=physical"
        )
        else -> "10.0.2.2"
    }
    return "http://$resolvedHost:5001/api/"
}

val apiBaseUrl = resolveApiBaseUrl()

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.veggo.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.veggo.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.core.splashscreen)
    implementation(libs.androidx.biometric)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // MVVM
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Room local cache (SQLite)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Retrofit API for backend connected to MongoDB Atlas
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Firebase services: Auth, Firestore, Storage, FCM
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)

    // Image loading from Firebase Storage/API image URLs
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)


    implementation("com.google.firebase:firebase-firestore-ktx:25.1.1")

    // Google & Facebook Login
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    // 16.3.0: stable AccessToken format for FirebaseAuth (17.x can break signInWithCredential)
    implementation("com.facebook.android:facebook-login:16.3.0")

    // Thư viện Room Database để chạy SQLite
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

// Thư viện Gson để đọc file JSON tự động
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.mindrot:jbcrypt:0.4")

    // Socket.IO (support chat)
    implementation("io.socket:socket.io-client:2.1.0") {
        exclude(group = "org.json", module = "json")
    }
}
