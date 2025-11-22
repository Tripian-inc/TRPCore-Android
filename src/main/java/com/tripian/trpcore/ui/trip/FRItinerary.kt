package com.tripian.trpcore.ui.trip

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.net.toUri
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.FrItineraryBinding
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.domain.model.UberModel
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.StartDragListener
import com.tripian.trpcore.util.extensions.encodeUrl
import com.tripian.trpcore.util.extensions.observe
import java.io.Serializable
import java.util.Collections

/**
 * Created by semihozkoroglu on 23.09.2020.
 */
class FRItinerary :
    BaseBottomDialogFragment<FrItineraryBinding, FRItineraryVM>(FrItineraryBinding::inflate),
    StartDragListener {

    private var itemTouchHelper: ItemTouchHelper? = null

    companion object {
        fun newInstance(steps: List<MapStep>): FRItinerary {
            val fragment = FRItinerary()

            val data = Bundle()
            data.putSerializable("steps", steps as Serializable)

            fragment.arguments = data

            return fragment
        }
    }

    override fun isDragEnable(): Boolean {
        return false
    }

    override fun getTheme(): Int {
        return super.getTheme()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvPlans.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)

        val itemTouchHelperCallback = object : ItemTouchHelper.Callback() {
            var dragFrom = -1
            var dragTo = -1
            var targetPos = -1

            override fun isLongPressDragEnabled(): Boolean {
                return false
            }

            override fun isItemViewSwipeEnabled(): Boolean {
                return false
            }

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                if (viewHolder.itemViewType == 0) {
                    return makeMovementFlags(0, 0)
                }

                val dragflags = ItemTouchHelper.UP or ItemTouchHelper.DOWN

                return makeMovementFlags(dragflags, 0)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {

                if (viewHolder.itemViewType != target.itemViewType) {
                    return false
                }

                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                dragFrom = fromPosition
                dragTo = toPosition

                if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                    Log.e("DRAGDROP", "reallyMoved: $dragFrom - $dragTo")
                    reallyMoved(dragFrom, dragTo)
                } else {
                    Log.e("DRAGDROP", "reallyMoved else case: $dragFrom - $dragTo")
                }

                binding.rvPlans.adapter?.notifyItemMoved(
                    viewHolder.adapterPosition,
                    target.adapterPosition
                )
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }

            private fun reallyMoved(dragFrom: Int, dragTo: Int) {
                if (dragTo == binding.rvPlans.adapter?.itemCount) {
                    return
                }

                targetPos = dragTo

                Log.e("DRAGDROP", "reallyMoved targetPos: $targetPos")
                val items = (binding.rvPlans.adapter as AdapterStep).items
                Collections.swap(items, dragFrom, dragTo)
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    val items = (binding.rvPlans.adapter as AdapterStep).items
                    viewModel.onDragEnded(items, targetPos)
                }
            }
        }

        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper!!.attachToRecyclerView(binding.rvPlans)
    }

    override fun setListeners() {
        super.setListeners()

        binding.tvTitle.text = getLanguageForKey(LanguageConst.ITINERARY)
        binding.imNavigation.setOnClickListener { dismiss() }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onDismissListener) {
            dismiss()
        }

        observe(viewModel.onNotifyChangedListener) { pos ->
            binding.rvPlans.adapter?.let {
                (it as AdapterStep).notifyItemChanged(pos!!)
            }
        }

        observe(viewModel.onNotifyRemovedListener) { pos ->
            binding.rvPlans.adapter?.let {
                (it as AdapterStep).notifyItemRemoved(pos!!)
            }
        }

        observe(viewModel.onSetStepListener) {
            if (it.isNullOrEmpty()) {
                binding.tvError.visibility = View.VISIBLE
                binding.rvPlans.visibility = View.GONE
            } else {
                binding.tvError.visibility = View.GONE
                binding.rvPlans.visibility = View.VISIBLE

                if (binding.rvPlans.adapter == null) {
                    binding.rvPlans.adapter = object : AdapterStep(
                        this,
                        requireContext(),
                        it,
                        miscRepository = viewModel.miscRepository
                    ) {
                        override fun onClickedItem(step: MapStep) {
                            viewModel.onClickedItem(step)
                        }

                        override fun onClickedAlternatives(step: MapStep) {
                            viewModel.onClickedAlternatives(step)
                        }

                        override fun onClickedDelete(pos: Int, step: MapStep) {
                            viewModel.onClickedDelete(pos, step)
                        }

                        override fun onClickedThumbsUp(pos: Int, step: MapStep) {
                            viewModel.onClickedThumbsUp(pos, step)
                        }

                        override fun onClickedThumbsDown(pos: Int, step: MapStep) {
                            viewModel.onClickedThumbsDown(pos, step)
                        }

                        override fun onClickedThumbsUndo(pos: Int, step: MapStep) {
                            viewModel.onClickedThumbsUndo(pos, step)
                        }

                        override fun onClickedChangeTime(step: MapStep) {
                            viewModel.onClickedChangeTime(step)
                        }

                        override fun onClickedUber(uberModel: UberModel) {

                            val clientId = "StrtxTcD7VgyYiUZyXF3-ViEhhkLzhZp"
                            val url =
                                "https://m.uber.com/ul/?client_id=${clientId}&action=setPickup&pickup[latitude]=${uberModel.pickupLocation.lat}&pickup[longitude]=${uberModel.pickupLocation.lng}&pickup[nickname]=${uberModel.pickupName.encodeUrl() ?: "Pickup"}&dropoff[latitude]=${uberModel.dropoffLocation.lat}&dropoff[longitude]=${uberModel.dropoffLocation.lng}&dropoff[nickname]=${uberModel.dropOffName.encodeUrl() ?: "DropOff"}&product_id=a1111c8c-c720-46c3-8534-2fcdd730040d"

                            startActivity(Intent(Intent.ACTION_VIEW).apply {
                                data = url.toUri()
                            })
                        }
                    }
                } else {
                    (binding.rvPlans.adapter as AdapterStep).items = it

                    binding.rvPlans.adapter?.notifyDataSetChanged()
                }
            }
        }

        observe(viewModel.openUrlListener) {
            if (it.isNullOrEmpty().not()) {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(it)
                )
                startActivity(browserIntent)
            }
//            openCustomTabExt(it!!)
        }
    }

    override fun requestDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper?.startDrag(viewHolder)
    }
}