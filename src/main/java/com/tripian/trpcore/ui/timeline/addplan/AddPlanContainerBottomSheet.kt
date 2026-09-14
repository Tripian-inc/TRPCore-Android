package com.tripian.trpcore.ui.timeline.addplan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.trip.model.Accommodation
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.BottomSheetAddPlanContainerBinding
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.domain.model.timeline.AddPlanStep
import com.tripian.trpcore.domain.model.timeline.ManualCategory
import com.tripian.trpcore.ui.timeline.activity.ACActivityListing
import com.tripian.trpcore.ui.timeline.poilisting.ACPOIListing
import com.tripian.trpcore.ui.timeline.poilisting.POIListingType
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.toSerializableIdsByDay
import com.tripian.trpcore.util.widget.BottomToast
import java.util.Date

/**
 * AddPlanContainerBottomSheet
 * Container for AddPlan flow with child fragment navigation
 * iOS Reference: AddPlanContainerVC.swift
 */
class AddPlanContainerBottomSheet : BaseBottomDialogFragment<BottomSheetAddPlanContainerBinding, AddPlanContainerVM>(
    BottomSheetAddPlanContainerBinding::inflate
) {

    private var onAddPlanCompleteListener: ((AddPlanData) -> Unit)? = null
    private var onSegmentCreatedListener: ((Int) -> Unit)? = null
    private var isResetting = false

    private val sharedVM: AddPlanContainerVM by lazy {
        ViewModelProvider(this, viewModelFactory)[AddPlanContainerVM::class.java]
    }

    private val manualListingLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedDayIndex = result.data?.getIntExtra(
                    ACActivityListing.RESULT_SELECTED_DAY_INDEX,
                    sharedVM.planData.selectedDayIndex
                ) ?: 0
                onSegmentCreatedListener?.invoke(selectedDayIndex)
                dismiss()
            }
        }

    override fun isFullscreen() = false
    override fun isCancelable() = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activityInsets = requireActivity().window.decorView.rootWindowInsets
        if (activityInsets != null) {
            val sysBars = WindowInsetsCompat.toWindowInsetsCompat(activityInsets)
                .getInsets(WindowInsetsCompat.Type.systemBars())
            adjustFragmentContainerMaxHeight(topInset = sysBars.top, bottomInset = sysBars.bottom)
        } else {
            adjustFragmentContainerMaxHeight(topInset = 0, bottomInset = 0)
            view.post {
                val rootInsets = view.rootWindowInsets ?: return@post
                val sysBars = WindowInsetsCompat.toWindowInsetsCompat(rootInsets)
                    .getInsets(WindowInsetsCompat.Type.systemBars())
                adjustFragmentContainerMaxHeight(topInset = sysBars.top, bottomInset = sysBars.bottom)
            }
        }

        arguments?.let { args ->
            sharedVM.initializeFromArgs(args)
        }

        showFragment(FRSelectDay())
    }

    /**
     * Calculates the fragmentContainer max height accounting for actual system bar insets.
     * Without subtracting the navigation bar inset, the LinearLayout root (which already gets
     * navbar-height bottom padding) overflows the bottom sheet and the Continue button is
     * clipped behind the device's bottom navigation buttons.
     */
    private fun adjustFragmentContainerMaxHeight(topInset: Int, bottomInset: Int) {
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density

        val headerHeight = (72 * density).toInt()
        val footerHeight = (80 * density).toInt()
        val handleHeight = (12 * density).toInt()
        val padding = (16 * density).toInt()

        val maxHeight = screenHeight - headerHeight - footerHeight - handleHeight - topInset - bottomInset - padding
        binding.fragmentContainer.maxHeight = maxHeight.coerceAtLeast((120 * density).toInt())
    }

    override fun setListeners() {
        super.setListeners()

        binding.ivClose.setOnClickListener {
            dismiss()
        }

        binding.ivBack.visibility = View.VISIBLE
        binding.ivBack.setOnClickListener {
            if (sharedVM.currentStep.value == AddPlanStep.SELECT_DAY_AND_CITY) {
                dismiss()
            } else {
                sharedVM.goToPreviousStep()
            }
        }

        binding.btnContinue.setOnClickListener {
            sharedVM.goToNextStep()
        }

        binding.tvClearSelection.setOnClickListener {
            sharedVM.clearSelection()
        }

        binding.tvClearSelection.text = sharedVM.getLanguageForKey(LanguageConst.ADD_PLAN_CLEAR_SELECTION)
    }

    /**
     * [AddPlanContainerVM.showBackButton] is intentionally not observed: the back
     * button stays visible on every step, acting as a close affordance on the first.
     */
    override fun setReceivers() {
        sharedVM.titleKey.observe(viewLifecycleOwner) { key ->
            binding.tvTitle.text = getLanguageForKey(key)
        }

        sharedVM.continueButtonEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.btnContinue.isEnabled = enabled
        }

        sharedVM.continueButtonTextKey.observe(viewLifecycleOwner) { key ->
            binding.btnContinue.text = getLanguageForKey(key)
        }

        sharedVM.showClearSelection.observe(viewLifecycleOwner) { show ->
            binding.tvClearSelection.visibility = if (show) View.VISIBLE else View.GONE
        }

        sharedVM.currentStep.observe(viewLifecycleOwner) { step ->
            if (!isResetting && !sharedVM.isNavigatingBack) {
                navigateToStep(step)
            }
        }

        sharedVM.navigateBack.observe(viewLifecycleOwner) { shouldGoBack ->
            if (shouldGoBack) {
                childFragmentManager.popBackStack()
                sharedVM.clearNavigateBack()
            }
        }

        sharedVM.resetToFirstStep.observe(viewLifecycleOwner) { shouldReset ->
            if (shouldReset) {
                isResetting = true
                childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                showFragment(FRSelectDay(), addToBackStack = false)
                sharedVM.clearResetToFirstStep()
                isResetting = false
            }
        }

        sharedVM.openManualListing.observe(viewLifecycleOwner) { category ->
            category?.let {
                openManualListingActivity(it)
                sharedVM.clearOpenManualListing()
            }
        }

        sharedVM.dismissSheet.observe(viewLifecycleOwner) { shouldDismiss ->
            if (shouldDismiss) {
                dismiss()
            }
        }

        sharedVM.onComplete.observe(viewLifecycleOwner) { data ->
            data?.let {
                onAddPlanCompleteListener?.invoke(it)
            }
        }

        sharedVM.expandBottomSheet.observe(viewLifecycleOwner) { expand ->
            expandBottomSheet(expand)
        }
    }

    private fun navigateToStep(step: AddPlanStep) {
        val fragment = when (step) {
            AddPlanStep.SELECT_DAY_AND_CITY -> FRSelectDay()
            AddPlanStep.TIME_AND_TRAVELERS -> FRTimeAndTravelers()
            AddPlanStep.CATEGORY_SELECTION -> FRCategorySelection()
        }

        showFragment(fragment, addToBackStack = step != AddPlanStep.SELECT_DAY_AND_CITY)
    }

    private fun showFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        val transaction = childFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.anim_horizontal_fragment_in,
                R.anim.anim_horizontal_fragment_out,
                R.anim.anim_horizontal_fragment_in_from_pop,
                R.anim.anim_horizontal_fragment_out_from_pop
            )
            .replace(R.id.fragmentContainer, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }

    /**
     * No-op: MaxHeightFrameLayout caps content height, footer is fixed,
     * content scrolls internally.
     */
    private fun expandBottomSheet(expand: Boolean) {
    }

    private fun openManualListingActivity(category: ManualCategory) {
        val planData = sharedVM.getValidPlanData() ?: return
        val tripHash = sharedVM.getTripHash() ?: return

        when (category) {
            ManualCategory.ACTIVITIES -> {
                val intent = ACActivityListing.launch(
                    context = requireContext(),
                    planData = planData,
                    tripHash = tripHash,
                    plannedActivityIdsByDay = sharedVM.getPlannedActivityIdsByDay(),
                    tripWideExcludedActivityIds = sharedVM.getTripWideExcludedActivityIds()
                )
                manualListingLauncher.launch(intent)
            }
            ManualCategory.PLACES_OF_INTEREST -> {
                val intent = ACPOIListing.launch(
                    context = requireContext(),
                    planData = planData,
                    tripHash = tripHash,
                    listingType = POIListingType.PLACES_OF_INTEREST,
                    tripStartDate = sharedVM.tripStartDate(),
                    tripEndDate = sharedVM.tripEndDate()
                )
                manualListingLauncher.launch(intent)
            }
            ManualCategory.EAT_AND_DRINK -> {
                val intent = ACPOIListing.launch(
                    context = requireContext(),
                    planData = planData,
                    tripHash = tripHash,
                    listingType = POIListingType.EAT_AND_DRINK,
                    tripStartDate = sharedVM.tripStartDate(),
                    tripEndDate = sharedVM.tripEndDate()
                )
                manualListingLauncher.launch(intent)
            }
        }
    }

    fun setOnAddPlanCompleteListener(listener: (AddPlanData) -> Unit) {
        onAddPlanCompleteListener = listener
    }

    fun setOnSegmentCreatedListener(listener: (Int) -> Unit) {
        onSegmentCreatedListener = listener
    }

    /**
     * Surfaces a create-segment error from the AddPlan flow over this sheet.
     * Anchors [BottomToast] to the sheet's own dialog window so the toast
     * appears on top of the sheet rather than behind it on the activity's
     * content view.
     */
    fun showCreateError(message: String) {
        if (!isAdded) return
        val parent = dialog?.window?.decorView as? ViewGroup
        BottomToast.show(
            activity = requireActivity(),
            message = message,
            alertType = AlertType.ERROR,
            parent = parent
        )
    }

    companion object {
        const val TAG = "AddPlanContainerBottomSheet"

        /**
         * @param plannedActivityIdsByDay "yyyy-MM-dd" → activity ids that day already
         *   holds; forwarded to the activity listing so the time selection sheet can
         *   block those days and exclude their ids from the created segment.
         */
        fun newInstance(
            availableDays: List<Date>,
            cities: List<City>,
            selectedDayIndex: Int,
            selectedCity: City?,
            tripHash: String?,
            accommodation: Accommodation? = null,
            bookedActivities: List<TimelineSegment> = emptyList(),
            plannedActivityIdsByDay: Map<String, List<String>> = emptyMap(),
            tripWideExcludedActivityIds: List<String> = emptyList()
        ): AddPlanContainerBottomSheet {
            return AddPlanContainerBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(AddPlanContainerVM.ARG_AVAILABLE_DAYS, ArrayList(availableDays))
                    putSerializable(AddPlanContainerVM.ARG_CITIES, ArrayList(cities))
                    putInt(AddPlanContainerVM.ARG_SELECTED_DAY_INDEX, selectedDayIndex)
                    selectedCity?.let { putSerializable(AddPlanContainerVM.ARG_SELECTED_CITY, it) }
                    tripHash?.let { putString(AddPlanContainerVM.ARG_TRIP_HASH, it) }
                    accommodation?.let { putSerializable(AddPlanContainerVM.ARG_ACCOMMODATION, it) }
                    putSerializable(AddPlanContainerVM.ARG_BOOKED_ACTIVITIES, ArrayList(bookedActivities))
                    putSerializable(
                        AddPlanContainerVM.ARG_PLANNED_ACTIVITY_IDS,
                        plannedActivityIdsByDay.toSerializableIdsByDay()
                    )
                    putStringArrayList(
                        AddPlanContainerVM.ARG_TRIP_WIDE_EXCLUDED_IDS,
                        ArrayList(tripWideExcludedActivityIds)
                    )
                }
            }
        }
    }
}
