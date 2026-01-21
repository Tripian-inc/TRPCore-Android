package com.tripian.trpcore.ui.timeline.addplan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.tripian.trpcore.util.LanguageConst
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
    private var onSegmentCreatedListener: (() -> Unit)? = null
    private var isResetting = false

    // Shared ViewModel accessible by child fragments
    private val sharedVM: AddPlanContainerVM by lazy {
        ViewModelProvider(this, viewModelFactory)[AddPlanContainerVM::class.java]
    }

    // Activity result launcher for manual listing activities
    private val manualListingLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Segment was created successfully, notify and dismiss
                onSegmentCreatedListener?.invoke()
                dismiss()
            }
            // If cancelled, the bottom sheet remains open for user to try again
        }

    override fun isFullscreen() = false
    override fun isDragEnable() = false
    override fun isCancelable() = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize from arguments
        arguments?.let { args ->
            sharedVM.initializeFromArgs(args)
        }

        // Show first fragment
        showFragment(FRSelectDay())
    }

    override fun setListeners() {
        super.setListeners()

        // Close button
        binding.ivClose.setOnClickListener {
            dismiss()
        }

        // Back button
        binding.ivBack.setOnClickListener {
            sharedVM.goToPreviousStep()
        }

        // Continue button
        binding.btnContinue.setOnClickListener {
            sharedVM.goToNextStep()
        }

        // Clear selection
        binding.tvClearSelection.setOnClickListener {
            sharedVM.clearSelection()
        }

        // Set clear selection text
        binding.tvClearSelection.text = sharedVM.getLanguageForKey(LanguageConst.ADD_PLAN_CLEAR_SELECTION)
    }

    override fun setReceivers() {
        // Title
        sharedVM.titleKey.observe(viewLifecycleOwner) { key ->
            binding.tvTitle.text = getLanguageForKey(key)
        }

        // Back button visibility
        sharedVM.showBackButton.observe(viewLifecycleOwner) { show ->
            binding.ivBack.visibility = if (show) View.VISIBLE else View.GONE
        }

        // Continue button state
        sharedVM.continueButtonEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.btnContinue.isEnabled = enabled
        }

        // Continue button text
        sharedVM.continueButtonTextKey.observe(viewLifecycleOwner) { key ->
            binding.btnContinue.text = getLanguageForKey(key)
        }

        // Clear selection visibility
        sharedVM.showClearSelection.observe(viewLifecycleOwner) { show ->
            binding.tvClearSelection.visibility = if (show) View.VISIBLE else View.GONE
        }

        // Step navigation
        sharedVM.currentStep.observe(viewLifecycleOwner) { step ->
            // Skip navigation if we're resetting or navigating back
            if (!isResetting && !sharedVM.isNavigatingBack) {
                navigateToStep(step)
            }
        }

        // Navigate back (for fragment transitions)
        sharedVM.navigateBack.observe(viewLifecycleOwner) { shouldGoBack ->
            if (shouldGoBack) {
                childFragmentManager.popBackStack()
                sharedVM.clearNavigateBack()
            }
        }

        // Reset to first step (clear selection)
        sharedVM.resetToFirstStep.observe(viewLifecycleOwner) { shouldReset ->
            if (shouldReset) {
                isResetting = true
                // Clear entire back stack and show first fragment
                childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                showFragment(FRSelectDay(), addToBackStack = false)
                sharedVM.clearResetToFirstStep()
                isResetting = false
            }
        }

        // Open manual listing
        sharedVM.openManualListing.observe(viewLifecycleOwner) { category ->
            category?.let {
                openManualListingActivity(it)
                sharedVM.clearOpenManualListing()
            }
        }

        // Dismiss sheet
        sharedVM.dismissSheet.observe(viewLifecycleOwner) { shouldDismiss ->
            if (shouldDismiss) {
                dismiss()
            }
        }

        // Completion
        sharedVM.onComplete.observe(viewLifecycleOwner) { data ->
            data?.let {
                onAddPlanCompleteListener?.invoke(it)
            }
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

    private fun openManualListingActivity(category: ManualCategory) {
        val planData = sharedVM.getValidPlanData() ?: return
        val tripHash = sharedVM.getTripHash() ?: return

        when (category) {
            ManualCategory.ACTIVITIES -> {
                // Open ACActivityListing for tours/activities
                val intent = ACActivityListing.launch(
                    context = requireContext(),
                    planData = planData,
                    tripHash = tripHash
                )
                manualListingLauncher.launch(intent)
            }
            ManualCategory.PLACES_OF_INTEREST -> {
                // Open ACPOIListing for places of interest
                val intent = ACPOIListing.launch(
                    context = requireContext(),
                    planData = planData,
                    tripHash = tripHash,
                    listingType = POIListingType.PLACES_OF_INTEREST
                )
                manualListingLauncher.launch(intent)
            }
            ManualCategory.EAT_AND_DRINK -> {
                // Open ACPOIListing for eat & drink
                val intent = ACPOIListing.launch(
                    context = requireContext(),
                    planData = planData,
                    tripHash = tripHash,
                    listingType = POIListingType.EAT_AND_DRINK
                )
                manualListingLauncher.launch(intent)
            }
        }
        // Don't dismiss - wait for activity result
    }

    fun setOnAddPlanCompleteListener(listener: (AddPlanData) -> Unit) {
        onAddPlanCompleteListener = listener
    }

    fun setOnSegmentCreatedListener(listener: () -> Unit) {
        onSegmentCreatedListener = listener
    }

    companion object {
        const val TAG = "AddPlanContainerBottomSheet"

        fun newInstance(
            availableDays: List<Date>,
            cities: List<City>,
            selectedDayIndex: Int,
            selectedCity: City?,
            tripHash: String?,
            accommodation: Accommodation? = null,
            bookedActivities: List<TimelineSegment> = emptyList()
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
                }
            }
        }
    }
}
