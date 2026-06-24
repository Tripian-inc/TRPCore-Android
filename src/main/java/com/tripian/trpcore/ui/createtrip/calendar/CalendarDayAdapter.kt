package com.tripian.trpcore.ui.createtrip.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ItemCalendarDayBinding

/** Selection state of a single calendar cell. */
enum class DayState { NONE, IN_RANGE, START, END, SINGLE }

/** One cell in the month grid. Empty cells pad the leading/trailing week. */
data class CalendarDay(
    val timeInMillis: Long,
    val dayNumber: Int,
    val isEmpty: Boolean,
    val enabled: Boolean,
    val state: DayState
)

/**
 * Renders a single month's 7-column day grid for [MonthCalendarView]. Cells are
 * rebuilt on every selection/month change (the grid is tiny), so a plain
 * notifyDataSetChanged is fine.
 */
class CalendarDayAdapter(
    private val onDayClicked: (Long) -> Unit
) : RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder>() {

    private val days = mutableListOf<CalendarDay>()

    fun setDays(newDays: List<CalendarDay>) {
        days.clear()
        days.addAll(newDays)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) =
        holder.bind(days[position])

    override fun getItemCount(): Int = days.size

    inner class DayViewHolder(
        private val binding: ItemCalendarDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(day: CalendarDay) {
            val ctx = binding.root.context
            if (day.isEmpty) {
                binding.tvDay.text = ""
                binding.bandLeft.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                binding.bandRight.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                binding.tvDay.background = null
                binding.root.setOnClickListener(null)
                binding.root.isClickable = false
                return
            }

            binding.tvDay.text = day.dayNumber.toString()

            // Connecting band between endpoints. START fills only the right half,
            // END only the left half, so nothing shows left of START / right of END.
            // The orange endpoint circles cap the band.
            val rangeColor = ctx.getColor(R.color.trp_date_range_bg)
            val transparent = android.graphics.Color.TRANSPARENT
            val leftOn = day.state == DayState.IN_RANGE || day.state == DayState.END
            val rightOn = day.state == DayState.IN_RANGE || day.state == DayState.START
            binding.bandLeft.setBackgroundColor(if (leftOn) rangeColor else transparent)
            binding.bandRight.setBackgroundColor(if (rightOn) rangeColor else transparent)

            val isEndpoint = day.state == DayState.START ||
                day.state == DayState.END || day.state == DayState.SINGLE
            binding.tvDay.setBackgroundResource(
                if (isEndpoint) R.drawable.trp_bg_date_endpoint else 0
            )

            val textColor = when {
                isEndpoint -> R.color.trp_white
                !day.enabled -> R.color.trp_timeline_text_tertiary
                else -> R.color.trp_text_primary
            }
            binding.tvDay.setTextColor(ctx.getColor(textColor))

            if (day.enabled) {
                binding.root.setOnClickListener { onDayClicked(day.timeInMillis) }
            } else {
                binding.root.setOnClickListener(null)
                binding.root.isClickable = false
            }
        }
    }
}
