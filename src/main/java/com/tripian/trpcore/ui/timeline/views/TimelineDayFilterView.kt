package com.tripian.trpcore.ui.timeline.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.tripian.trpcore.databinding.ViewTimelineDayFilterBinding
import com.tripian.trpcore.ui.timeline.adapter.DayFilterAdapter
import java.util.*

/**
 * TimelineDayFilterView
 * Horizontal scrollable day selector
 */
class TimelineDayFilterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewTimelineDayFilterBinding
    private val adapter: DayFilterAdapter
    private var onDaySelectedListener: ((Int) -> Unit)? = null

    init {
        binding = ViewTimelineDayFilterBinding.inflate(LayoutInflater.from(context), this, true)

        adapter = DayFilterAdapter { position ->
            onDaySelectedListener?.invoke(position)
        }

        binding.rvDays.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = this@TimelineDayFilterView.adapter
            setHasFixedSize(true)
        }
    }

    fun setDays(days: List<Date>) {
        adapter.setDays(days)
    }

    fun setSelectedDay(index: Int) {
        adapter.setSelectedPosition(index)
        // Scroll to selected
        binding.rvDays.scrollToPosition(index)
    }

    fun setOnDaySelectedListener(listener: (Int) -> Unit) {
        onDaySelectedListener = listener
    }
}
