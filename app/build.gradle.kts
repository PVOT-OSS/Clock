/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

plugins { alias(libs.plugins.android.application) }

android {
    namespace = "org.prauga.clock"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.prauga.clock"
        minSdk = 31
        targetSdk = 36

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

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.junit)
}

tasks.withType<JavaCompile>().configureEach { options.compilerArgs.add("-Xlint:unchecked") }
