package com.tripian.trpcore.domain

import com.tripian.one.api.trip.model.QuestionsResponse
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.QuestionRepository
import com.tripian.trpcore.util.QuestionType
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class Questions @Inject constructor(val repository: QuestionRepository) : BaseUseCase<QuestionsResponse, Questions.Params>() {

    class Params(val category: QuestionType)

    override fun on(params: Params?) {
        addObservable {
            repository.getQuestions(params!!.category)
        }
    }
}