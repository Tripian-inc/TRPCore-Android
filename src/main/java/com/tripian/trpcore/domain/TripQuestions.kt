package com.tripian.trpcore.domain

import com.tripian.one.api.trip.model.Answer
import com.tripian.one.api.trip.model.Question
import com.tripian.one.api.trip.model.QuestionsResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.QuestionRepository
import com.tripian.trpcore.util.CreateTripSteps
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.QuestionType
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class TripQuestions @Inject constructor(val repository: QuestionRepository) : BaseUseCase<QuestionsResponse, TripQuestions.Params>() {

    private var isTitleUsed = false

    class Params(val step: CreateTripSteps)

    override fun on(params: Params?) {
        addObservable {
            repository.getQuestions(QuestionType.TRIP).map { questionsResponse ->
                val questions = ArrayList<Question>()
                questionsResponse.data?.let { list ->
                    val sortedList = list.sortedBy { question -> question.order }
                    if (params!!.step == CreateTripSteps.ITINERARY_PROFILE) {
                        questions.addAll(sortedList.filter { question -> question.id != 6 })
                    } else {
                        questions.addAll(sortedList.filter { question -> question.id == 6 })
                    }
                }
                if (params!!.step == CreateTripSteps.ITINERARY_PROFILE) {
                    questions.filter { (it.id == 1111) || (it.id == 11) }.forEach { question ->
                        var questionName = ""
                        if (question.id == 1111) {
                            // Dinner
                            questionName = miscRepository.getLanguageValueForKey(LanguageConst.DINNER)
                        } else if (question.id == 11) {
                            // Lunch/Brunch
                            questionName = miscRepository.getLanguageValueForKey(LanguageConst.LUNCH_BRUNCH)
                        }
                        question.name = questionName
                        question.theme = "spinner"
                        if (!isTitleUsed) {
                            isTitleUsed = true

                            question.tmpTitle = miscRepository.getLanguageValueForKey(LanguageConst.WHAT_TYPE_RESTAURANT)
                        }
                        addDefaultAnswer(question)

                    }
                }

                QuestionsResponse().apply {
                    data = questions
                    status = 200
                }
//                it
            }
        }
    }

    private fun defaultAnswer() = Answer().apply {
        name = miscRepository.getLanguageValueForKey(LanguageConst.PLEASE_SELECT)
        id = -1
    }

    private fun addDefaultAnswer(question: Question) {
        val answerList = ArrayList<Answer>()
        question.answerList?.let { answerList.addAll(it) }
        if (answerList.indexOfFirst { it.id == defaultAnswer().id } < 0) {
            answerList.add(0, defaultAnswer())
        }
        question.answerList = answerList
    }
}