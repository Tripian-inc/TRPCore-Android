package com.tripian.trpcore.ui.companion

import android.content.Context
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
abstract class AdapterCompanionQuestions constructor(val context: Context, val items: List<Question>, val answers: List<Int>?) :
    RecyclerView.Adapter<AdapterCompanionQuestions.QuestionHolder>() {

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
            tvName.text = if (!item.isSkipAble) {
                "${item.name} *"
            } else {
                item.name
            }

            val adapter = if (item.isSelectMultiple) {
                object : AdapterCompanionOptionsMultiple(context, item.answerList ?: arrayListOf(), answers) {
                    override fun notified() {
                        this@AdapterCompanionQuestions.notified()
                    }
                }
            } else {
                object : AdapterCompanionOptionsOnce(context, item.answerList ?: arrayListOf(), answers) {
                    override fun notified() {
                        this@AdapterCompanionQuestions.notified()
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
                        (adapter as AdapterCompanionOptionsMultiple).getSelectedItems()
                    } else {
                        (adapter as AdapterCompanionOptionsOnce).getSelectedItems()
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
                    val ans = (adapter as AdapterCompanionOptionsMultiple).getSelectedItems()

                    if (!item.isSkipAble && ans.isEmpty()) {
                        return false
                    }
                } else {
                    val ans = (adapter as AdapterCompanionOptionsOnce).getSelectedItems()

                    if (!item.isSkipAble && ans.isEmpty()) {
                        return false
                    }
                }
            }
        }

        return true
    }

    class QuestionHolder constructor(vi: View) : RecyclerView.ViewHolder(vi) {
        val tvName: TextView = vi.findViewById(R.id.tvName)
        val rvInnerList: RecyclerView = vi.findViewById(R.id.rvInnerList)

        init {
//            rvInnerList.layoutManager = GridLayoutManager(vi.context, 2, RecyclerView.VERTICAL, false)
            rvInnerList.layoutManager = LinearLayoutManager(vi.context, RecyclerView.VERTICAL, false)
        }
    }
}