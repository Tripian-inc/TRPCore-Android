package com.tripian.trpcore.ui.timeline.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ItemDayFilterBinding
import com.tripian.trpcore.util.extensions.appLanguage
import com.tripian.trpcore.util.extensions.isPastDay
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

    /**
     * When true, past days render muted and tap events are swallowed —
     * used by the AddPlan flow to prevent picking a day in the past.
     */
    var disablePastDays: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

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

    /** Returns the Date at [position] or null if the index is out of range. */
    fun dayAt(position: Int): Date? = days.getOrNull(position)

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
            val isPast = date.isPastDay()
            val isPastDisabled = disablePastDays && isPast

            // Background: same drawable for past/future — selected gets the border
            // ring, unselected has no border. Past days do NOT dim the container
            // (we dim individual texts instead) so the selection border keeps its
            // full opacity, "default black" look.
            if (isSelected) {
                binding.llDayContainer.setBackgroundResource(R.drawable.bg_day_filter_selected)
            } else {
                binding.llDayContainer.setBackgroundResource(R.drawable.bg_day_filter_unselected)
            }

            // Text styling: past days keep the muted "unselected" typography even
            // when selected — only the border ring indicates the selection.
            val applyActiveText = isSelected && !isPast

            if (applyActiveText) {
                // Selected (future): all text primary color, day number bold
                val fgColor = ContextCompat.getColor(context, R.color.trp_text_primary)
                binding.tvDayLetter.setTextColor(fgColor)
                binding.tvDayNumber.setTextColor(fgColor)
                binding.tvMonth.setTextColor(fgColor)
                ResourcesCompat.getFont(context, R.font.bold)?.let {
                    binding.tvDayNumber.typeface = it
                }
            } else {
                // Unselected, OR past (with or without selection): day letter fgWeak,
                // others fg, all medium.
                val fgWeakColor = ContextCompat.getColor(context, R.color.trp_fgWeak)
                val fgColor = ContextCompat.getColor(context, R.color.trp_text_primary)
                binding.tvDayLetter.setTextColor(fgWeakColor)
                binding.tvDayNumber.setTextColor(fgColor)
                binding.tvMonth.setTextColor(fgColor)
                ResourcesCompat.getFont(context, R.font.medium)?.let {
                    binding.tvDayNumber.typeface = it
                }
            }

            // Past days dim only their text. The container (and therefore the
            // selection border) stays at full opacity.
            val textAlpha = if (isPast) 0.4f else 1.0f
            binding.tvDayLetter.alpha = textAlpha
            binding.tvDayNumber.alpha = textAlpha
            binding.tvMonth.alpha = textAlpha
            binding.llDayContainer.alpha = 1.0f
            binding.llDayContainer.isEnabled = !isPastDisabled

            // Click listener
            binding.llDayContainer.setOnClickListener {
                if (isPastDisabled) return@setOnClickListener
                if (position != selectedPosition) {
                    onDaySelected(position)
                }
            }
        }
    }
}
