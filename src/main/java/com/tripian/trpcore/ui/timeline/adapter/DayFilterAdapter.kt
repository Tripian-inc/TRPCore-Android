package com.tripian.trpcore.ui.timeline.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ItemDayFilterBinding
import com.tripian.trpcore.util.extensions.appLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DayFilterAdapter
 * Horizontal gün seçici için adapter
 * Format: "DayName dd/MM" (örn: "Miércoles 13/05")
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
            val context = binding.root.context

            // Use app language for locale instead of system default
            val locale = Locale.forLanguageTag(appLanguage)
            val dayFormat = SimpleDateFormat("EEEE dd/MM", locale)

            // Format: "DayName dd/MM" with capitalized first letter
            val formattedDate = dayFormat.format(date).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(locale) else it.toString()
            }
            binding.tvDayLabel.text = formattedDate

            // Selection state
            if (isSelected) {
                binding.tvDayLabel.setBackgroundResource(R.drawable.bg_day_filter_selected)
                binding.tvDayLabel.setTextColor(
                    ContextCompat.getColor(context, R.color.primary)
                )
            } else {
                binding.tvDayLabel.setBackgroundResource(R.drawable.bg_day_filter_unselected)
                binding.tvDayLabel.setTextColor(
                    ContextCompat.getColor(context, R.color.borderActive)
                )
            }

            // Click listener
            binding.tvDayLabel.setOnClickListener {
                if (position != selectedPosition) {
                    onDaySelected(position)
                }
            }
        }
    }
}
