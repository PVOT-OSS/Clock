/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.ringtone

import android.content.res.ColorStateList
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.prauga.clock.ItemAdapter
import org.prauga.clock.R
import org.prauga.clock.ThemeUtils

class RecordSoundHolder : ItemAdapter.ItemHolder<Uri>(null, RecyclerView.NO_ID) {
    override fun getItemViewType(): Int = RecordSoundViewHolder.VIEW_TYPE_RECORD
}

class RecordSoundViewHolder private constructor(itemView: View) :
    ItemAdapter.ItemViewHolder<RecordSoundHolder>(itemView), View.OnClickListener {

    init {
        itemView.setOnClickListener(this)
        itemView.findViewById<View>(R.id.sound_image_selected).visibility = View.GONE
        itemView.findViewById<TextView>(R.id.ringtone_name).apply {
            setText(R.string.record_new_sound)
            alpha = 0.63f
        }
        itemView.findViewById<ImageView>(R.id.ringtone_image).apply {
            setImageResource(R.drawable.ic_mic)
            setBackgroundResource(R.drawable.bg_circle_white)
            backgroundTintList =
                ColorStateList.valueOf(ThemeUtils.resolveColor(context, R.attr.colorPrimary))
        }
    }

    override fun onClick(view: View) = notifyItemClicked(CLICK_RECORD)

    class Factory(private val inflater: LayoutInflater) : ItemAdapter.ItemViewHolder.Factory {
        override fun createViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ItemAdapter.ItemViewHolder<*> =
            RecordSoundViewHolder(inflater.inflate(R.layout.ringtone_item_sound, parent, false))
    }

    companion object {
        const val VIEW_TYPE_RECORD = Int.MIN_VALUE + 1
        const val CLICK_RECORD = VIEW_TYPE_RECORD
    }
}
