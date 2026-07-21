package com.tripian.trpcore.ui.timeline.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.tripian.trpcore.databinding.ViewTimelineDayFilterBinding
import com.tripian.trpcore.ui.timeline.adapter.DayFilterAdapter
import com.tripian.trpcore.util.extensions.isPastDay
import java.util.Date

/**
 * TimelineDayFilterView
 * Horizontal scrollable day selector
 */
class TimelineDayFilterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * How the day filter treats past days: [TIMELINE] renders them muted but tappable
     * (cells enforce read-only), [ADD_PLAN] renders them muted and swallows taps.
     */
    enum class Mode { TIMELINE, ADD_PLAN }

    private val binding: ViewTimelineDayFilterBinding =
        ViewTimelineDayFilterBinding.inflate(LayoutInflater.from(context), this, true)
    private val adapter: DayFilterAdapter
    private var onDaySelectedListener: ((Int) -> Unit)? = null

    /** Defaults to [Mode.TIMELINE]; AddPlan callers flip this to [Mode.ADD_PLAN]. */
    var mode: Mode = Mode.TIMELINE

    /**
     * IANA timezone of the trip's city, forwarded to the adapter so "past day"
     * rendering matches the AddPlan flow instead of falling back to the device clock.
     */
    var timeZoneId: String? = null
        set(value) {
            field = value
            adapter.timeZoneId = value
        }

    /**
     * Extra day indices that should be disabled (greyed out, taps ignored). Used
     * by the AddPlan availability sweep to grey out days where no slots exist.
     */
    private val unavailableDayIndices: MutableSet<Int> = mutableSetOf()

    init {

        adapter = DayFilterAdapter { position ->
            if (!isPositionSelectable(position)) return@DayFilterAdapter
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
        binding.rvDays.scrollToPosition(index)
    }

    fun setOnDaySelectedListener(listener: (Int) -> Unit) {
        onDaySelectedListener = listener
    }

    /**
     * Sets the indices of days that have no available slots — they are visually
     * disabled and tap-suppressed.
     */
    fun setUnavailableDayIndices(indices: Set<Int>) {
        unavailableDayIndices.clear()
        unavailableDayIndices.addAll(indices)
    }

    private fun isPositionSelectable(position: Int): Boolean {
        if (position in unavailableDayIndices) return false
        if (mode == Mode.ADD_PLAN) {
            val day = adapter.dayAt(position) ?: return true
            if (day.isPastDay()) return false
        }
        return true
    }
}
