import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun secret(key: String): String = localProperties.getProperty(key, "").trim()

android {
    namespace = "com.example.nearbydiscount"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.nearbydiscount"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // 密钥不写入版本库，只在本地构建时注入
        buildConfigField("String", "MEITUAN_LBS_KEY", "\"${secret("meituan.lbs.key")}\"")
        buildConfigField("String", "DEALS_BRIDGE_URL", "\"${secret("deals.bridge.url")}\"")
        buildConfigField("boolean", "ENABLE_DEMO_DATA", secret("enable.demo.data").toBoolean())
        manifestPlaceholders["appName"] = "附近优惠"

        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            val store = secret("release.keystore.file")
            if (store.isNotBlank()) {
                storeFile = rootProject.file(store)
                storePassword = secret("release.keystore.password")
                keyAlias = secret("release.key.alias")
                keyPassword = secret("release.key.password")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (secret("release.keystore.file").isNotBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    testImplementation(kotlin("test"))
    debugImplementation(libs.androidx.compose.ui.tooling)
}
