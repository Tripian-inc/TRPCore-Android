package com.tripian.trpcore.util

import androidx.recyclerview.widget.RecyclerView

/**
 * Created by semihozkoroglu on 19.02.2022.
 */
interface StartDragListener {
    fun requestDrag(viewHolder: RecyclerView.ViewHolder)
}