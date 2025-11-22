package com.tripian.trpcore.ui.companion

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.UserCompanionListener
import com.tripian.trpcore.domain.UserCompanions
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.goBack
import com.tripian.one.api.companion.model.Companion
import javax.inject.Inject

class FRCompanionSelectVM @Inject constructor(val userCompanions: UserCompanions, val userCompanionListener: UserCompanionListener) : BaseViewModel(userCompanions, userCompanionListener) {

    var onSetCompanionsListener = MutableLiveData<Pair<List<Companion>, List<Companion>?>>()
    var onShowLoadingListener = MutableLiveData<Unit>()
    var onHideLoadingListener = MutableLiveData<Unit>()

    private var companions: List<Companion>? = null

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        companions = if (arguments != null && arguments!!.containsKey("companions")) {
            arguments!!.getSerializable("companions") as List<Companion>
        } else {
            null
        }

        onShowLoadingListener.postValue(Unit)

        userCompanions.on(success = {
            onSetCompanionsListener.postValue(Pair(it.data!!, companions))

            onHideLoadingListener.postValue(Unit)

            registerCompanion()
        }, error = {
            onHideLoadingListener.postValue(Unit)

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
            onSetCompanionsListener.postValue(Pair(it.data!!, companions))
        })
    }

    fun onClickedOk(companions: List<Companion>) {
        eventBus.post(EventMessage(EventConstants.CompanionPicker, companions))

        goBack()
    }

    fun onClickedManage() {
        startActivity(ACManageCompanion::class, Bundle().apply {
            putSerializable("mode", CompanionMode.PROFILE)
        })
    }
}
