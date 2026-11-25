import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// Read local.properties on the developer's machine only. Never commit the REST API key.
val localProps = Properties()
val localFile = rootProject.file("local.properties")
if (localFile.exists()) localFile.inputStream().use { localProps.load(it) }
val restKey = (localProps.getProperty("KAKAO_REST_API_KEY") ?: "").trim()
val nativeMapKey = (localProps.getProperty("KAKAO_NATIVE_APP_KEY") ?: "").trim()
val safeNativeKey = nativeMapKey.replace("\\", "\\\\").replace("\"", "\\\"")
val teamHazardUrl = (localProps.getProperty("TEAM_HAZARD_API_URL") ?: "").trim()
val safeTeamUrl = teamHazardUrl.replace("\\", "\\\\").replace("\"", "\\\"")
val safeRestKey = restKey.replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.example.bicyclesafety"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.bicyclesafety"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "KAKAO_REST_API_KEY", "\"$safeRestKey\"")
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$safeNativeKey\"")
        buildConfigField("String", "TEAM_HAZARD_API_URL", "\"$safeTeamUrl\"")
        ndk {
            abiFilters += "arm64-v8a"
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Design team Compose export (Java activity and native KakaoMap are retained).
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.13.0")

    implementation("com.kakao.maps.open:android:2.15.2")
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
