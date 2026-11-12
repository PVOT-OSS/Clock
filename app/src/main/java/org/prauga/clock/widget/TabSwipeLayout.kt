/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs
import org.prauga.clock.uidata.UiDataModel

/**
 * Holds the tab fragments and switches to the next or previous tab on a horizontal swipe. Swipes
 * that start near the screen edges are left to the system back gesture, and mostly vertical moves
 * to the scrolling content.
 */
class TabSwipeLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minFling = ViewConfiguration.get(context).scaledMinimumFlingVelocity * 4
    private val edge = (48 * resources.displayMetrics.density).toInt()
    private var downX = 0f
    private var downY = 0f
    private var tracking = false
    private var velocity: VelocityTracker? = null

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                tracking = downX > edge && downX < width - edge
                velocity?.recycle()
                velocity = VelocityTracker.obtain().apply { addMovement(event) }
            }
            MotionEvent.ACTION_MOVE -> {
                velocity?.addMovement(event)
                val dx = abs(event.x - downX)
                val dy = abs(event.y - downY)
                return tracking && dx > touchSlop * 2 && dx > dy * 2
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> tracking = false
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        velocity?.addMovement(event)
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val tracker = velocity
            tracker?.computeCurrentVelocity(1000)
            val dx = event.x - downX
            val fast = abs(tracker?.xVelocity ?: 0f) > minFling
            if (abs(dx) > width / 4 || fast) {
                // Swiping towards the start shows the next tab; mirrored in RTL.
                val forward = (dx < 0) != (layoutDirection == LAYOUT_DIRECTION_RTL)
                switchTab(if (forward) 1 else -1)
            }
            tracking = false
        }
        return true
    }

    private fun switchTab(step: Int) {
        val model = UiDataModel.getUiDataModel()
        val tabs = UiDataModel.Tab.entries
        val next = model.selectedTab.ordinal + step
        if (next in tabs.indices) {
            model.setSelectedTab(tabs[next])
        }
    }
}
