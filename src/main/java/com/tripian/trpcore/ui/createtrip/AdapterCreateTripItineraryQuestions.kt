package com.tripian.trpcore.ui.createtrip

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.trip.model.Question
import com.tripian.trpcore.R
import com.tripian.trpcore.ui.companion.AdapterOptionsOnce
import com.tripian.trpcore.util.widget.TextView

/**
 * Created by semihozkoroglu on 19.08.2020.
 */
abstract class AdapterCreateTripItineraryQuestions(val context: Context, val items: List<Question>, var answers: List<Int>?) :
    RecyclerView.Adapter<AdapterCreateTripItineraryQuestions.QuestionHolder>() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val adapters = HashMap<Int, RecyclerView.Adapter<*>>()

    abstract fun notified()
    abstract fun onClickedSpinnerItem(question: Question)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionHolder {
        return QuestionHolder(inflater.inflate(R.layout.item_select_question, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: QuestionHolder, position: Int) {
        val item = items[position]

        with(holder) {
            rvInnerList.isVisible = true
            clSpinner.isVisible = false
            tvName.isVisible = true
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
//            tvSpinnerValue.text = context.getString(R.string.please_select)

            if (item.theme == "spinner") {
                rvInnerList.isVisible = false
                clSpinner.isVisible = true
                tvName.isVisible = false
                tvSpinnerTitle.text = item.name
                clSpinner.setOnClickListener { onClickedSpinnerItem(item)}

                if (answers != null && item.answerList != null) {
                    item.answerList!!.forEach { answer ->
                        if (answers!!.contains(answer.id)) {
                            tvSpinnerValue.text = answer.name
                        }
                    }
                }
            } else {
                val adapter = if (item.isSelectMultiple) {
                    object : AdapterCreateTripAnswerMultiple(context, item.answerList ?: arrayListOf(), answers) {
                        override fun notified() {
                            this@AdapterCreateTripItineraryQuestions.notified()
                        }
                    }
                } else if (item.theme == "box") {
                    object : AdapterCreateTripBoxAnswer(context, item.answerList ?: arrayListOf(), answers) {
                        override fun notified() {
                            this@AdapterCreateTripItineraryQuestions.notified()
                        }
                    }
                } else {
                    object : AdapterOptionsOnce(context, item.answerList ?: arrayListOf(), answers) {
                        override fun notified() {
                            this@AdapterCreateTripItineraryQuestions.notified()
                        }
                    }
                }

                adapters[position] = adapter

                rvInnerList.adapter = adapter
            }
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
                        (adapter as AdapterCreateTripAnswerMultiple).getSelectedItems()
                    } else if (item.theme == "box") {
                        (adapter as AdapterCreateTripBoxAnswer).getSelectedItems()
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
                    val ans = (adapter as AdapterCreateTripAnswerMultiple).getSelectedItems()

                    if (!item.isSkipAble && ans.isEmpty()) {
                        return false
                    }
                } else if (item.theme == "box") {
                    val ans = (adapter as AdapterCreateTripBoxAnswer).getSelectedItems()

                    if (!item.isSkipAble && ans.isEmpty()) {
                        return false
                    }
                } else {
                    val ans = (adapter as AdapterOptionsOnce).getSelectedItems()

                    if (!item.isSkipAble && ans.isEmpty()) {
                        return false
                    }
                }
            }
        }

        return true
    }

    class QuestionHolder(vi: View) : RecyclerView.ViewHolder(vi) {
        val tvName: TextView = vi.findViewById(R.id.tvName)
        val tvTitle: TextView = vi.findViewById(R.id.tvTitle)
        val rvInnerList: RecyclerView = vi.findViewById(R.id.rvInnerList)
        val clSpinner: ConstraintLayout = vi.findViewById(R.id.clSpinner)
        val tvSpinnerTitle: TextView = vi.findViewById(R.id.tvSpinnerTitle)
        val tvSpinnerValue: TextView = vi.findViewById(R.id.tvSpinnerValue)

        init {
            rvInnerList.layoutManager = LinearLayoutManager(vi.context, RecyclerView.VERTICAL, false)
        }
    }
}