package com.tripian.trpcore.ui.companion

import android.os.Build
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.companion.model.Companion
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.DeleteCompanion
import com.tripian.trpcore.domain.UserCompanionListener
import com.tripian.trpcore.domain.UserCompanions
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.goBack
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.showLoading
import com.tripian.trpcore.util.fragment.AnimationType
import javax.inject.Inject

class FRCompanionsVM @Inject constructor(
    private val userCompanions: UserCompanions,
    private val userCompanionListener: UserCompanionListener,
    val deleteCompanion: DeleteCompanion
) :
    BaseViewModel(userCompanions, userCompanionListener, deleteCompanion) {

    var onSetCompanionsListener = MutableLiveData<Pair<List<Companion>?, List<Companion>>>()

    private lateinit var selectedCompanion: List<Companion>
    lateinit var notifyData: Pair<List<Companion>?, List<Companion>>

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        selectedCompanion = if (arguments?.containsKey("companions") == true) {
            val companionsSerializable =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arguments?.getSerializable("companions", ArrayList::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    arguments?.getSerializable("companions")
                }
            if (companionsSerializable is List<*>) {
                companionsSerializable.filterIsInstance<Companion>()
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }

        showLoading()

        userCompanions.on(success = {
            notifyData = Pair(selectedCompanion, it.data!!)
            onSetCompanionsListener.postValue(notifyData)

            hideLoading()

            registerCompanion()
        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }

            registerCompanion()
        })
    }

    private fun registerCompanion() {
        userCompanionListener.on(success = {
            onSetCompanionsListener.postValue(Pair(selectedCompanion, it.data!!))
        })
    }

    fun onApplySelected(companions: List<Companion>) {
        eventBus.post(EventMessage(EventConstants.CompanionPicker, companions))

        goBack()
    }

    fun onClickedItem(companion: Companion) {
        navigateToFragment(
            fragment = FRNewCompanion.newInstance(companion),
            animation = AnimationType.ENTER_FROM_RIGHT
        )
    }

    fun remove(companion: Companion) {
        showDialog(
            contentText = getLanguageForKey(LanguageConst.DOU_YOU_CONFIRM),
            positiveBtn = getLanguageForKey(LanguageConst.DELETE),
            negativeBtn = getLanguageForKey(LanguageConst.CANCEL),
            positive = object : DGActionListener {
                override fun onClicked(o: Any?) {
                    showLoading()

                    deleteCompanion.on(DeleteCompanion.Params(companion.id!!), success = {
                        hideLoading()
                    }, error = {
                        hideLoading()

                        if (::notifyData.isInitialized) {
                            onSetCompanionsListener.postValue(notifyData)
                        }

                        if (it.type == AlertType.DIALOG) {
                            showDialog(contentText = it.errorDesc)
                        } else {
                            showAlert(AlertType.ERROR, it.errorDesc)
                        }
                    })
                }
            },
            negative = object : DGActionListener {
                override fun onClicked(o: Any?) {
                    if (::notifyData.isInitialized) {
                        onSetCompanionsListener.postValue(notifyData)
                    }
                }
            }
        )
    }

    fun onCreateCompanion() {
        navigateToFragment(fragment = FRNewCompanion.newInstance())
    }
}
