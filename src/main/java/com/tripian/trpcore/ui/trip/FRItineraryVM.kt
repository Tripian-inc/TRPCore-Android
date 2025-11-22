package com.tripian.trpcore.ui.trip

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.AddReaction
import com.tripian.trpcore.domain.DeleteReaction
import com.tripian.trpcore.domain.DeleteStep
import com.tripian.trpcore.domain.GetDirectionRoutes
import com.tripian.trpcore.domain.GetMapStep
import com.tripian.trpcore.domain.UpdateDailyPlanStepOrder
import com.tripian.trpcore.domain.UpdateStepOrder
import com.tripian.trpcore.domain.UpdateStepTime
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.ui.trip_detail.ACTripDetail
import com.tripian.trpcore.ui.trip_detail.TripDetail
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.ReactionType
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.getSerializableCompat
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.showLoading
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

class FRItineraryVM @Inject constructor(
    val getMapStep: GetMapStep, val getDirectionRoutes: GetDirectionRoutes,
    val addReaction: AddReaction, val deleteReaction: DeleteReaction,
    private val updateDailyPlanStepOrder: UpdateDailyPlanStepOrder,
    val updateStepOrder: UpdateStepOrder, val deleteStep: DeleteStep,
    private val updateStepTime: UpdateStepTime,
) :
    BaseViewModel(
        getMapStep,
        getDirectionRoutes,
        updateDailyPlanStepOrder,
        updateStepOrder,
        addReaction,
        deleteReaction,
        deleteStep,
        updateStepTime
    ) {

    var onSetStepListener = MutableLiveData<List<MapStep>>()
    var onDismissListener = MutableLiveData<Unit>()
    var onNotifyChangedListener = MutableLiveData<Int>()
    var onNotifyRemovedListener = MutableLiveData<Int>()
    var openUrlListener = MutableLiveData<String>()

    var mapSteps: ArrayList<MapStep>? = null
//    private lateinit var city: String

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)


        val mapStepList: ArrayList<MapStep> =
            arguments?.getSerializableCompat<ArrayList<MapStep>>("steps") ?: arrayListOf()
//        city = arguments!!.getString("city") ?: ""
        onSetStepListener.postValue(mapStepList)

        getMapStep.on(success = { steps ->
            if (mapSteps == null) {
                mapSteps = ArrayList()
            }

            mapSteps!!.clear()
            mapSteps!!.addAll(steps)

            onSetStepListener.postValue(mapSteps)

            getDirectionRoutes.on(GetDirectionRoutes.Params(steps = mapSteps), success = {
                onSetStepListener.postValue(mapSteps)
            }, error = {
                onSetStepListener.postValue(mapSteps)
            })
        })
    }

    fun onDragEnded(sortedItems: List<MapStep>, targetPos: Int) {
        val hasHomeBase = sortedItems[0].homeBase
        for (i in sortedItems.indices) {
            sortedItems[i].order = i
        }

        var reOrderSteps = sortedItems.map { it.stepId }
        if (sortedItems[0].homeBase) {
            reOrderSteps = reOrderSteps.drop(1)
        }

        onSetStepListener.postValue(sortedItems)

        if (targetPos != -1 && targetPos < sortedItems.size) {
            showLoading()

            updateDailyPlanStepOrder.on(UpdateDailyPlanStepOrder.Params(reOrderSteps), success = {
                hideLoading()
            }, error = {
                hideLoading()

                if (it.type == AlertType.DIALOG) {
                    showDialog(contentText = it.errorDesc)
                } else {
                    showAlert(AlertType.ERROR, it.errorDesc)
                }
            })

//            val step = sortedItems[targetPos]
//            updateStepOrder.on(UpdateStepOrder.Params(step.stepId, null, targetPos), success = {
//                hideLoading()
//            }, error = {
//                hideLoading()
//
//                if (it.type == AlertType.DIALOG) {
//                    showDialog(contentText = it.errorDesc)
//                } else {
//                    showAlert(AlertType.ERROR, it.errorDesc)
//                }
//            })
        }
    }

    fun onClickedAlternatives(step: MapStep) {
        navigateToFragment(FRStepAlternatives.newInstance(step))
    }

    fun onClickedItem(step: MapStep) {
        if (!step.homeBase) {
            startActivity(ACTripDetail::class, bundleOf(Pair("detail", TripDetail().apply {
                poiId = step.poiId
                stepId = step.stepId
            })))
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onLocationRouteEvent(item: EventMessage<MapStep>) {
        if (item.tag == EventConstants.LocationRedirect) {
            onDismissListener.postValue(Unit)
        }
    }

    private fun sendReaction(pos: Int, item: MapStep, type: ReactionType) {
        showLoading()

        addReaction.on(AddReaction.Params(item.poiId, item.stepId, type), success = {
            item.reaction = it.data

            onNotifyChangedListener.postValue(pos)

            hideLoading()
        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }
        })
    }

    fun onClickedDelete(pos: Int, step: MapStep) {
        showLoading()

        deleteStep.on(DeleteStep.Params(step.stepId), success = {
            onNotifyRemovedListener.postValue(pos)

            hideLoading()
        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }
        })
    }

    fun onClickedThumbsUp(pos: Int, step: MapStep) {
        sendReaction(pos, step, ReactionType.THUMBS_UP)
    }

    fun onClickedThumbsDown(pos: Int, step: MapStep) {
        sendReaction(pos, step, ReactionType.THUMBS_DOWN)
    }

    fun onClickedThumbsUndo(pos: Int, step: MapStep) {
        step.reaction?.let {
            showLoading()

            deleteReaction.on(DeleteReaction.Params(it.id!!), success = {
                step.reaction = null

                onNotifyChangedListener.postValue(pos)

                hideLoading()
            }, error = {
                hideLoading()

                if (it.type == AlertType.DIALOG) {
                    showDialog(contentText = it.errorDesc)
                } else {
                    showAlert(AlertType.ERROR, it.errorDesc)
                }
            })
        }
    }

    fun onClickedBuyTicket(step: MapStep) {
//        openUrlListener.postValue(step.getBuyTicketUrl())
    }

    fun onClickedChangeTime(step: MapStep) {
        navigateToFragment(
            FRChangeTimePicker.newInstance(
                step.stepId,
                step.times?.from,
                step.times?.to,
                estimatedDuration = step.poi?.duration ?: 30
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onChangeStepTimeEvent(item: EventMessage<Triple<Int, String, String>>) {
        if (item.tag == EventConstants.ChangeTimePicker) {

            item.value?.let { (stepId, startTime, endTime) ->

                showLoading()
                updateStepTime.on(UpdateStepTime.Params(stepId, startTime, endTime), success = {
                    hideLoading()
                }, error = {
                    hideLoading()

                    if (it.type == AlertType.DIALOG) {
                        showDialog(contentText = it.errorDesc)
                    } else {
                        showAlert(AlertType.ERROR, it.errorDesc)
                    }
                })
            }
        }
    }
}
