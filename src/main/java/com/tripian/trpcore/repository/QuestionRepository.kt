package com.tripian.trpcore.repository

import com.tripian.one.api.trip.model.Question
import com.tripian.one.api.trip.model.QuestionsResponse
import com.tripian.trpcore.util.QuestionType
import com.tripian.trpcore.util.extensions.getLanguage
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class QuestionRepository @Inject constructor(val service: Service) {

    private var tripQuestions = ArrayList<Question>()
    private var companionQuestions = ArrayList<Question>()
    private var profileQuestions = ArrayList<Question>()

    fun getQuestions(
        category: QuestionType,
        language: String? = getLanguage()
    ): Observable<QuestionsResponse> {
        if (category == QuestionType.TRIP && tripQuestions.isNotEmpty()) {
            return Observable.just(QuestionsResponse().apply {
                data = tripQuestions
                status = 200
            })
        } else if (category == QuestionType.COMPANION && companionQuestions.isNotEmpty()) {
            return Observable.just(QuestionsResponse().apply {
                data = companionQuestions
                status = 200
            })
        } else if (category == QuestionType.PROFILE && profileQuestions.isNotEmpty()) {
            return Observable.just(QuestionsResponse().apply {
                data = profileQuestions
                status = 200
            })
        }
        return service.getQuestions(
            cityId = null,
            category = category.type,
            languageCode = language
        ).map { questionResponse ->
            questionResponse.data?.let { questions ->
                when(category) {
                    QuestionType.TRIP -> tripQuestions.addAll(questions)
                    QuestionType.PROFILE -> profileQuestions.addAll(questions)
                    QuestionType.COMPANION -> companionQuestions.addAll(questions)
                }
            }

            QuestionsResponse().apply {
                data = questionResponse.data
                status = 200
            }
        }
    }

    fun clearItems() {
        tripQuestions.clear()
        companionQuestions.clear()
        profileQuestions.clear()
    }

}