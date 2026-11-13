/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("Insets")

package org.prauga.clock.common

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.padForNavigationBar() {
    val initialBottom = paddingBottom
    (this as? ViewGroup)?.clipToPadding = false
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars =
            insets.getInsets(
                WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout()
            )
        view.setPadding(
            view.paddingLeft,
            view.paddingTop,
            view.paddingRight,
            initialBottom + bars.bottom,
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
