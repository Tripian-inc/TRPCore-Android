package com.tripian.trpcore.ui.companion

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.util.widget.TextView
import com.tripian.one.api.trip.model.Question

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterQuestions constructor(val context: Context, val items: List<Question>, val answers: List<Int>?) :
    RecyclerView.Adapter<AdapterQuestions.QuestionHolder>() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val adapters = HashMap<Int, RecyclerView.Adapter<*>>()

    abstract fun notified()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionHolder {
        return QuestionHolder(inflater.inflate(R.layout.item_select_question, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: QuestionHolder, position: Int) {
        val item = items[position]

        with(holder) {
            if (TextUtils.isEmpty(item.tmpTitle)) {
                tvTitle.visibility = View.GONE
            } else {
                tvTitle.visibility = View.VISIBLE
                tvTitle.text = item.tmpTitle
            }

            tvName.text = if (!item.isSkipAble) {
                "${item.name} *"
            } else {
                item.name
            }

            val adapter = if (item.isSelectMultiple) {
                object : AdapterOptionsMultiple(context, item.answerList ?: arrayListOf(), answers) {
                    override fun notified() {
                        this@AdapterQuestions.notified()
                    }
                }
            } else {
                object : AdapterOptionsOnce(context, item.answerList ?: arrayListOf(), answers) {
                    override fun notified() {
                        this@AdapterQuestions.notified()
                    }
                }
            }

            adapters[position] = adapter

            rvInnerList.adapter = adapter
        }
    }

    fun getSelectedItems(): Array<Int> {
        val answers = ArrayList<Int>()

        adapters.entries.forEach {
            val item = items[it.key]
            val adapter = adapters[it.key]

            if (adapter != null) {
                answers.addAll(
                    if (item.isSelectMultiple) {
                        (adapter as AdapterOptionsMultiple).getSelectedItems()
                    } else {
                        (adapter as AdapterOptionsOnce).getSelectedItems()
                    }
                )
            }
        }

        return answers.toTypedArray()
    }

    fun isAnswerOK(): Boolean {
        adapters.entries.forEach {
            val item = items[it.key]
            val adapter = adapters[it.key]

            if (adapter != null) {
                if (item.isSelectMultiple) {
                    val ans = (adapter as AdapterOptionsMultiple).getSelectedItems()

                    if (!item.isSkipAble && ans.isNullOrEmpty()) {
                        return false
                    }
                } else {
                    val ans = (adapter as AdapterOptionsOnce).getSelectedItems()

                    if (!item.isSkipAble && ans.isNullOrEmpty()) {
                        return false
                    }
                }
            }
        }

        return true
    }

    class QuestionHolder constructor(vi: View) : RecyclerView.ViewHolder(vi) {
        val tvName: TextView = vi.findViewById(R.id.tvName)
        val tvTitle: TextView = vi.findViewById(R.id.tvTitle)
        val rvInnerList: RecyclerView = vi.findViewById(R.id.rvInnerList)

        init {
            rvInnerList.layoutManager = LinearLayoutManager(vi.context, RecyclerView.VERTICAL, false)
        }
    }
}