plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.maisha.game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.maisha.game"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        // Replace with hosted HTTPS URL before Play Store submission (see docs/PRIVACY_POLICY.md).
        buildConfigField(
            "String",
            "PRIVACY_POLICY_URL",
            "\"https://REPLACE-WITH-HOSTED-PRIVACY-POLICY-URL\""
        )
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.play.services.ads)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.tracing)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:${libs.versions.robolectric.get()}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.coroutinesTest.get()}")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
