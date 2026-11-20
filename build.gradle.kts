/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

ext {
    val versionMajor = 1
    val versionMinor = 1
    val versionPatch = 1

    set("versionMajor", versionMajor)
    set("versionMinor", versionMinor)
    set("versionPatch", versionPatch)
    set("computedVersionCode", versionMajor * 10000 + versionMinor * 100 + versionPatch)
    set("computedVersionName", "$versionMajor.$versionMinor.$versionPatch")
}
