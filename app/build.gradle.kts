/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.pvot.android.compose)
}

val versionMajor = rootProject.extra["versionMajor"] as Int
val versionMinor = rootProject.extra["versionMinor"] as Int
val versionPatch = rootProject.extra["versionPatch"] as Int
val computedVersionCode = rootProject.extra["computedVersionCode"] as Int
val computedVersionName = rootProject.extra["computedVersionName"] as String

android {
    namespace = "org.prauga.clock"
    compileSdk { version = release(37) { minorApiLevel = 1 } }

    defaultConfig {
        applicationId = "org.prauga.clock"
        minSdk = 31
        targetSdk = 36
        versionCode = computedVersionCode
        versionName = computedVersionName

        buildConfigField("int", "VERSION_MAJOR", "$versionMajor")
        buildConfigField("int", "VERSION_MINOR", "$versionMinor")
        buildConfigField("int", "VERSION_PATCH", "$versionPatch")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug { applicationIdSuffix = ".dev" }
        release {
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
        }
    }

    androidResources { generateLocaleConfig = true }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        lintConfig = file("lint.xml")
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.loader)
    implementation(libs.pvot.design.system)
    implementation(libs.pvot.design.system.views)
    implementation(libs.pvot.core.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.junit)
}

tasks.withType<JavaCompile>().configureEach { options.compilerArgs.add("-Xlint:unchecked") }
