plugins {
    // This line uses the alias defined in your version catalog (libs.versions.toml)
    alias(libs.plugins.android.application)
    // This plugin is for Google Services (like Firebase)
    id("com.google.gms.google-services")
    // The previous line 'id ("com.android.application")' was removed as it was a duplicate.
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 33
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Firebase platform BOM — đồng bộ version
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))

    // ✅ Các thư viện Firebase cần thiết
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")

    // ✅ THƯ VIỆN UI CƠ BẢN
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2")//chuyen khoan vao vi

    // =========================================================================
    // 🛠️ TÍCH HỢP CAMERA VÀ LPR (NHẬN DẠNG BIỂN SỐ)
    // =========================================================================

    // 1. CAMERA X (Dùng cho LPRScannerActivity)
    // ĐÃ SỬA: Dùng 'val' thay vì 'def'
    val camerax_version = "1.3.3"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")

    // CameraX View
    implementation("androidx.camera:camera-view:$camerax_version")

    // 2. TFLite Task Library (Cần thiết nếu bạn dùng mô hình LPR TFLite tùy chỉnh)
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // HOẶC ML Kit (Nếu bạn muốn dùng thử Nhận dạng Văn bản cơ bản của Google ML Kit)
    // Lưu ý: ML Kit chỉ nhận dạng ký tự chung, không được tối ưu cho định dạng biển số Việt Nam
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // =========================================================================

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}