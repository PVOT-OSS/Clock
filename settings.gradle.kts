/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

pluginManagement {
    includeBuild("PvotLib/build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Clock"

includeBuild("PvotLib") {
    dependencySubstitution {
        substitute(module("com.prauga.pvot:design-system")).using(project(":design-system"))
        substitute(module("com.prauga.pvot:core-ui")).using(project(":core-ui"))
    }
}

include(":app")
