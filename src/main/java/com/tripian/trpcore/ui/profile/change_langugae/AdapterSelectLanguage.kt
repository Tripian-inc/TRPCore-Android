package com.tripian.trpcore.ui.profile.change_langugae

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.util.widget.TextView

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterSelectLanguage(val context: Context, val items: List<Pair<String, String>>) :
    RecyclerView.Adapter<AdapterSelectLanguage.SelectLanguage>() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    abstract fun onClickedItem(langCode: Pair<String, String>)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectLanguage {
        return SelectLanguage(inflater.inflate(R.layout.item_select_language, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: SelectLanguage, position: Int) {
        val item = items[position]

        with(holder) {
            tvName.text = item.second

            itemView.setOnClickListener { onClickedItem(item) }
        }
    }

    class SelectLanguage(vi: View) : RecyclerView.ViewHolder(vi) {
        val tvName: TextView = vi.findViewById(R.id.tvName)
    }
}