package com.tripian.trpcore.ui.timeline.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ItemDayFilterBinding
import com.tripian.trpcore.util.extensions.appLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DayFilterAdapter
 * Adapter for horizontal day selector
 * Format: 3 rows - Day letter, Day number, Month abbreviation
 */
class DayFilterAdapter(
    private val onDaySelected: (Int) -> Unit
) : RecyclerView.Adapter<DayFilterAdapter.DayViewHolder>() {

    private var days: List<Date> = emptyList()
    private var selectedPosition: Int = 0

    fun setDays(newDays: List<Date>) {
        days = newDays
        notifyDataSetChanged()
    }

    fun setSelectedPosition(position: Int) {
        val oldPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(oldPosition)
        notifyItemChanged(selectedPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemDayFilterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position], position, position == selectedPosition)
    }

    override fun getItemCount(): Int = days.size

    inner class DayViewHolder(
        private val binding: ItemDayFilterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(date: Date, position: Int, isSelected: Boolean) {
            // Use app language for locale instead of system default
            val locale = Locale.forLanguageTag(appLanguage)

            // Day letter (first letter of day name, e.g., "M" for Monday)
            val dayLetterFormat = SimpleDateFormat("EEEEE", locale)
            binding.tvDayLetter.text = dayLetterFormat.format(date).uppercase(locale)

            // Day number (e.g., "13")
            val dayNumberFormat = SimpleDateFormat("d", locale)
            binding.tvDayNumber.text = dayNumberFormat.format(date)

            // Month abbreviation (3 letters, e.g., "may")
            val monthFormat = SimpleDateFormat("MMM", locale)
            binding.tvMonth.text = monthFormat.format(date).lowercase(locale)

            val context = binding.root.context

            // Selection state - apply background and text styles
            if (isSelected) {
                binding.llDayContainer.setBackgroundResource(R.drawable.bg_day_filter_selected)
                // Selected: all text primary color, day number bold
                val fgColor = ContextCompat.getColor(context, R.color.trp_text_primary)
                binding.tvDayLetter.setTextColor(fgColor)
                binding.tvDayNumber.setTextColor(fgColor)
                binding.tvMonth.setTextColor(fgColor)
                // Day number bold for selected
                ResourcesCompat.getFont(context, R.font.bold)?.let {
                    binding.tvDayNumber.typeface = it
                }
            } else {
                binding.llDayContainer.setBackgroundResource(R.drawable.bg_day_filter_unselected)
                // Unselected: day letter fgWeak, others fg, all medium
                val fgWeakColor = ContextCompat.getColor(context, R.color.trp_fgWeak)
                val fgColor = ContextCompat.getColor(context, R.color.trp_text_primary)
                binding.tvDayLetter.setTextColor(fgWeakColor)
                binding.tvDayNumber.setTextColor(fgColor)
                binding.tvMonth.setTextColor(fgColor)
                // Day number medium for unselected
                ResourcesCompat.getFont(context, R.font.medium)?.let {
                    binding.tvDayNumber.typeface = it
                }
            }

            // Click listener
            binding.llDayContainer.setOnClickListener {
                if (position != selectedPosition) {
                    onDaySelected(position)
                }
            }
        }
    }
}
