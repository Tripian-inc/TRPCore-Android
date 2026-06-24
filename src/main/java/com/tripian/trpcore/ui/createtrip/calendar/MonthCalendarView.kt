package com.tripian.trpcore.ui.createtrip.calendar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import com.tripian.trpcore.databinding.ViewMonthCalendarBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Inline month calendar with start..end range selection, matching the create-trip
 * "Select a date" mockup (no modal dialog). Monday-first grid, past days disabled,
 * prev/next month paging (cannot page before the current month). Uses
 * [java.util.Calendar] (minSdk 24, so no java.time).
 */
class MonthCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private val binding = ViewMonthCalendarBinding.inflate(LayoutInflater.from(context), this)
    private val dayAdapter = CalendarDayAdapter { onDayTapped(it) }
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    private val today: Calendar = Calendar.getInstance().stripTime()
    private val currentMonth: Calendar = (today.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }

    private var startMillis: Long? = null
    private var endMillis: Long? = null

    /** Notified whenever the selected range changes (end may be null). */
    var onRangeChanged: ((start: Long?, end: Long?) -> Unit)? = null

    init {
        orientation = VERTICAL
        binding.rvDays.layoutManager = GridLayoutManager(context, 7)
        binding.rvDays.adapter = dayAdapter
        binding.ivPrevMonth.setOnClickListener { shiftMonth(-1) }
        binding.ivNextMonth.setOnClickListener { shiftMonth(1) }
        render()
    }

    fun getStart(): Long? = startMillis
    fun getEnd(): Long? = endMillis

    private fun shiftMonth(delta: Int) {
        // Don't page before the current (today's) month.
        if (delta < 0 && !canGoPrev()) return
        currentMonth.add(Calendar.MONTH, delta)
        render()
    }

    private fun canGoPrev(): Boolean {
        val prev = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        // Allowed only if the previous month is still >= today's month.
        return !(prev.get(Calendar.YEAR) < today.get(Calendar.YEAR) ||
            (prev.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                prev.get(Calendar.MONTH) < today.get(Calendar.MONTH)))
    }

    private fun onDayTapped(millis: Long) {
        val s = startMillis
        val e = endMillis
        when {
            s == null || e != null -> { startMillis = millis; endMillis = null }
            millis < s -> { startMillis = millis }
            else -> { endMillis = millis }
        }
        render()
        onRangeChanged?.invoke(startMillis, endMillis)
    }

    private fun render() {
        binding.tvMonthLabel.text = monthFormat.format(currentMonth.time)
        binding.ivPrevMonth.visibility = if (canGoPrev()) View.VISIBLE else View.INVISIBLE
        dayAdapter.setDays(buildDays())
    }

    private fun buildDays(): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        val month = (currentMonth.clone() as Calendar)
        val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
        // Monday-first leading offset.
        val firstDow = month.get(Calendar.DAY_OF_WEEK) // SUNDAY=1..SATURDAY=7
        val offset = (firstDow + 5) % 7
        val totalCells = 42

        repeat(offset) { days.add(emptyCell()) }
        for (d in 1..daysInMonth) {
            val cal = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, d) }.stripTime()
            val m = cal.timeInMillis
            days.add(
                CalendarDay(
                    timeInMillis = m,
                    dayNumber = d,
                    isEmpty = false,
                    enabled = m >= today.timeInMillis,
                    state = stateFor(m)
                )
            )
        }
        while (days.size < totalCells) days.add(emptyCell())
        return days
    }

    private fun stateFor(m: Long): DayState {
        val s = startMillis
        val e = endMillis
        return when {
            s != null && e != null -> when {
                m == s && m == e -> DayState.SINGLE
                m == s -> DayState.START
                m == e -> DayState.END
                m > s && m < e -> DayState.IN_RANGE
                else -> DayState.NONE
            }
            s != null && e == null -> if (m == s) DayState.SINGLE else DayState.NONE
            else -> DayState.NONE
        }
    }

    private fun emptyCell() = CalendarDay(0L, 0, isEmpty = true, enabled = false, state = DayState.NONE)

    private fun Calendar.stripTime(): Calendar = apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
