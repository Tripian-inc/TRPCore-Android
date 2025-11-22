package com.tripian.trpcore.domain

import android.text.TextUtils
import com.tripian.one.api.companion.model.CompanionResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.CompanionRepository
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class CreateCompanion @Inject constructor(
    val repository: CompanionRepository
) : BaseUseCase<CompanionResponse, CreateCompanion.Params>() {

    class Params(
        val name: String,
        val title: String,
        val age: String,
        val answers: Array<Int>? = null
    )

    override fun on(params: Params?) {
        when {

            TextUtils.isEmpty(params!!.name) -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.NAME_REQUIRED),
                        type = AlertType.ERROR
                    )
                )
            }

            TextUtils.isEmpty(params.age) -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.TYPE_AGE),
                        type = AlertType.ERROR
                    )
                )
            }

            TextUtils.isEmpty(params.title) -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.COMPANION_TITLE_REQUIRED),
                        type = AlertType.ERROR
                    )
                )
            }

            else -> {
                addObservable {
                    repository.addCompanion(
                        params.name,
                        params.title,
                        params.age.toInt(),
                        params.answers
                    )
                }
            }
        }
    }
}