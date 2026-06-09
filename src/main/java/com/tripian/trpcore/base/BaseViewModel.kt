package com.tripian.trpcore.base

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import androidx.annotation.CallSuper
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.ui.common.loader.LottieLoadingPresentation
import com.tripian.trpcore.ui.common.loader.LottieLoadingText
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.OnBackPressListener
import com.tripian.trpcore.util.Strings
import com.tripian.trpcore.util.ViewListener
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.event.SingleLiveEvent
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.setUseCasesListener
import com.tripian.trpcore.util.fragment.FragmentFactory
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.Array as Array1


/**
 * Created by Semih Özköroğlu on 29.09.2019
 */
abstract class BaseViewModel(vararg cases: BaseUseCase<*, *>) : ViewModel(), OnBackPressListener {

    @Inject
    lateinit var strings: Strings

    @Inject
    lateinit var miscRepository: MiscRepository

    var arguments: Bundle? = null

    /**
     * Holds the list of use cases to dispose requests in use cases
     */
    var useCases = arrayListOf(*cases)

    var fragmentManager: FragmentManager? = null
    var viewListener: ViewListener? = null

    open fun onCreate(savedInstanceState: Bundle?) {
    }

    @CallSuper
    open fun onViewCreated(savedInstanceState: Bundle?) {
        setUseCasesListener()

        viewListener?.hideLoading()
    }

    @CallSuper
    open fun onStart() {
    }

    @CallSuper
    open fun onDestroy() {
    }

    @CallSuper
    open fun onResume() {
    }

    @CallSuper
    open fun onPause() {
    }

    @CallSuper
    open fun onSaveInstanceState(outState: Bundle?) {
    }

    /**
     * Called when the ViewModel lifecycle ends.
     * Clears RxJava Disposables.
     */
    override fun onCleared() {
        super.onCleared()

        for (useCase in useCases) {
            useCase.clear()
        }
    }

    open fun onDestroyView() {
        for (useCase in useCases) {
            useCase.clear()
        }
    }

    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    }

    open fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array1<String>,
        grantResults: IntArray
    ) {
    }

    override fun isBackEnable(): Boolean {
        return false
    }

    override fun onBackPressed(): Boolean {
        return true
    }

    fun startActivity(
        kClass: KClass<out FragmentActivity>,
        bundle: Bundle? = null,
        flags: Int? = null
    ) {
        viewListener?.startActivity(kClass, bundle, flags)
    }

    fun finishActivity() {
        viewListener?.finishActivity()
    }

    @kotlin.jvm.JvmOverloads
    fun showDialog(
        title: String? = getLanguageForKey(LanguageConst.WARNING),
        contentText: String? = "",
        positiveBtn: String? = getLanguageForKey(LanguageConst.CONTINUE),
        negativeBtn: String? = null,
        positive: DGActionListener? = null,
        negative: DGActionListener? = null,
        isCloseEnable: Boolean = true
    ) {
        val fragment =
            FRWarning.newInstance(title, contentText, positiveBtn, negativeBtn, isCloseEnable)
        fragment.positiveListener = object : DGActionListener {
            override fun onClicked(o: Any?) {
                positive?.onClicked(o)
                fragment.dismiss()
            }
        }
        fragment.negativeListener = object : DGActionListener {
            override fun onClicked(o: Any?) {
                negative?.onClicked(o)
                fragment.dismiss()
            }
        }

        navigateToFragment(fragment)
    }

    fun showFragment(factory: FragmentFactory) {
        viewListener?.showFragment(factory)
    }

    fun showAlert(type: AlertType, message: String?) {
        if (!TextUtils.isEmpty(message)) {
            viewListener?.showAlert(type, message!!)
        }
    }

    fun getLanguageForKey(key: String): String {
        return miscRepository.getLanguageValueForKey(key)
    }

    // ---------------------------------------------------------------------
    // Lottie loading (long-running operations)
    // ---------------------------------------------------------------------
    data class LottieLoadingEvent(
        val show: Boolean,
        val presentation: LottieLoadingPresentation = LottieLoadingPresentation.FULL_SCREEN,
        val text: LottieLoadingText = LottieLoadingText.None
    )

    private val _lottieLoadingEvent = SingleLiveEvent<LottieLoadingEvent>()
    val lottieLoadingEvent: LiveData<LottieLoadingEvent> = _lottieLoadingEvent

    fun showLottieLoading(
        presentation: LottieLoadingPresentation = LottieLoadingPresentation.FULL_SCREEN,
        text: LottieLoadingText = LottieLoadingText.Rotating.default()
    ) {
        dispatchLottieEvent(LottieLoadingEvent(true, presentation, text))
    }

    fun hideLottieLoading() {
        dispatchLottieEvent(LottieLoadingEvent(false))
    }

    private fun dispatchLottieEvent(event: LottieLoadingEvent) {
        // setValue on main thread fires the observer synchronously, so the
        // loader dialog can be committed in the same onCreate pass and shown
        // alongside the first frame instead of one tick after it.
        if (Looper.myLooper() == Looper.getMainLooper()) {
            _lottieLoadingEvent.value = event
        } else {
            _lottieLoadingEvent.postValue(event)
        }
    }

    /** Convenience for bottom-sheet "refreshing" loader. */
    fun showRefreshLoader() {
        val refreshing = miscRepository
            .getLanguageValueForKey(LanguageConst.LOADING_TEXT_LOADING_TIME_SLOTS)
            .ifBlank { "Loading available times" }
        showLottieLoading(
            LottieLoadingPresentation.BOTTOM_SHEET,
            LottieLoadingText.Single(refreshing)
        )
    }

    /** Bottom-sheet loader with a single resolved language key as text. */
    fun showBottomSheetLoader(languageKey: String, fallback: String) {
        val text = miscRepository.getLanguageValueForKey(languageKey).ifBlank { fallback }
        showLottieLoading(
            LottieLoadingPresentation.BOTTOM_SHEET,
            LottieLoadingText.Single(text)
        )
    }

    /** Full-screen loader with a single resolved language key as text. */
    fun showFullScreenLoader(languageKey: String, fallback: String) {
        val text = miscRepository.getLanguageValueForKey(languageKey).ifBlank { fallback }
        showLottieLoading(
            LottieLoadingPresentation.FULL_SCREEN,
            LottieLoadingText.Single(text)
        )
    }

    /** Full-screen loader showing only the animation (no text). */
    fun showFullScreenLoaderNoText() {
        showLottieLoading(
            LottieLoadingPresentation.FULL_SCREEN,
            LottieLoadingText.None
        )
    }

    /** Bottom-sheet loader showing only the animation (no text). */
    fun showBottomSheetLoaderNoText() {
        showLottieLoading(
            LottieLoadingPresentation.BOTTOM_SHEET,
            LottieLoadingText.None
        )
    }

    /**
     * In-sheet loader: renders inline inside the hosting bottom sheet's own
     * view tree (no separate window). Resolved by [BaseBottomDialogFragment]'s
     * loader observer. Calling this from a non-bottom-sheet host is a no-op.
     */
    fun showInSheetLoader(languageKey: String, fallback: String) {
        val text = miscRepository.getLanguageValueForKey(languageKey).ifBlank { fallback }
        showLottieLoading(
            LottieLoadingPresentation.INLINE_SHEET,
            LottieLoadingText.Single(text)
        )
    }

    /** In-sheet loader showing only the animation (no text). */
    fun showInSheetLoaderNoText() {
        showLottieLoading(
            LottieLoadingPresentation.INLINE_SHEET,
            LottieLoadingText.None
        )
    }
}