import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.weather.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.weather.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 56
        versionName = "1.8.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // 正确位置：splits 必须与 defaultConfig 平级，放在 android 闭包下
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
        }
    }

    // 动态设置 versionCode 并解决多架构文件名冲突
    val abiVersionCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2)

    applicationVariants.all {
        val variant = this
        variant.outputs.forEach { output ->
            val abiOutput = output as? com.android.build.gradle.internal.api.ApkVariantOutputImpl
            if (abiOutput != null) {
                val abiFilter = abiOutput.getFilter(com.android.build.OutputFile.ABI)
                if (abiFilter != null) {
                    val abiCode = abiVersionCodes[abiFilter] ?: 0
                    abiOutput.versionCodeOverride = abiCode * 100000 + variant.versionCode
                }
                val abiName = abiFilter ?: "universal"
                abiOutput.outputFileName = "Weather-v${variant.versionName}-${abiName}-${variant.buildType.name}.apk"
            }
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = localProperties.getProperty("RELEASE_KEYSTORE_PATH") ?: "app.jks"
            storeFile = file(keystorePath)
            storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                ?: project.findProperty("RELEASE_STORE_PASSWORD") as String?
                        ?: System.getenv("RELEASE_STORE_PASSWORD")
                        ?: ""
            keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                ?: project.findProperty("RELEASE_KEY_ALIAS") as String?
                        ?: System.getenv("RELEASE_KEY_ALIAS")
                        ?: ""
            keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
                ?: project.findProperty("RELEASE_KEY_PASSWORD") as String?
                        ?: System.getenv("RELEASE_KEY_PASSWORD")
                        ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true  // 开启 R8 代码压缩与混淆
            isShrinkResources = true // 开启无用资源裁剪
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false  // 开启 R8 代码压缩与混淆
            isShrinkResources = false // 开启无用资源裁剪
            // 临时开启调试能力，方便用 Android Studio 抓 Logcat 和附加断点
            isDebuggable = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/*.version"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // AndroidX 核心与生命周期
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack Compose & Material 3 / Material 2 (PullRefresh)
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")

    // 新一代开源拖拽排序库 (Reorderable)
    implementation("sh.calvin.reorderable:reorderable:1.3.1")

    // 网络与 JSON 解析
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JWT 签名与认证 (Ed25519 / EdDSA)
    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")
    implementation("com.google.crypto.tink:tink:1.13.0")

    // 协程支持
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // 后台定期刷新与省电调度 (WorkManager)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // 图片加载 (Coil)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // MapLibre 原生地图 SDK
    implementation("org.maplibre.gl:android-sdk:11.5.1")

    // 测试套件
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}