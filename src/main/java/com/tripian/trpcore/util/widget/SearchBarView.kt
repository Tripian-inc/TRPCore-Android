@file:Suppress("OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS")

package com.tripian.trpcore.util.widget

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updateLayoutParams
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ViewSearchBarBinding
import com.tripian.trpcore.util.extensions.dp

/**
 * Shared search input. When text is entered the search icon hides and a clear (X)
 * icon shows on the right. The query is published through
 * [setOnQueryChangedListener]: debounced in [Mode.REMOTE], per keystroke in
 * [Mode.LOCAL]. An empty query, the clear icon and the keyboard's search key all
 * publish immediately.
 *
 * Appearance (background, icon, icon size, horizontal padding, text/hint color)
 * is configurable through the `trpSearch*` XML attributes.
 */
class SearchBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * [REMOTE] for queries that hit a service, [LOCAL] for in-memory filtering.
     */
    enum class Mode { REMOTE, LOCAL }

    private val binding = ViewSearchBarBinding.inflate(LayoutInflater.from(context), this, true)

    private val lightFont: Typeface? = ResourcesCompat.getFont(context, R.font.light)
    private val mediumFont: Typeface? = ResourcesCompat.getFont(context, R.font.medium)

    private val debounceHandler = Handler(Looper.getMainLooper())
    private var pendingQuery: Runnable? = null

    private var mode: Mode = Mode.REMOTE
    private var debounceMs: Long = DEFAULT_DEBOUNCE_MS
    private var lastPublishedQuery: String = ""

    private var onQueryChangedListener: ((String) -> Unit)? = null
    private var onTextChangedListener: ((String) -> Unit)? = null
    private var onSearchActionListener: ((String) -> Unit)? = null

    private var iconSizePx: Int = DEFAULT_ICON_SIZE_DP.dp
    private var horizontalPaddingPx: Int = DEFAULT_HORIZONTAL_PADDING_DP.dp

    val editText get() = binding.etInput

    init {
        applyAppearance(attrs)
        setupTextWatcher()
        setupClearButton()
        setupSearchAction()
        updateUIState(hasText = false)
    }

    private fun applyAppearance(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SearchBarView)
        val backgroundRes = typedArray.getResourceId(
            R.styleable.SearchBarView_trpSearchBackground,
            R.drawable.trp_bg_addplan_search_bar
        )
        val iconRes = typedArray.getResourceId(
            R.styleable.SearchBarView_trpSearchIcon,
            R.drawable.trp_ic_search
        )
        val iconTint = typedArray.getColor(R.styleable.SearchBarView_trpSearchIconTint, NO_TINT)
        iconSizePx = typedArray.getDimensionPixelSize(
            R.styleable.SearchBarView_trpSearchIconSize,
            DEFAULT_ICON_SIZE_DP.dp
        )
        horizontalPaddingPx = typedArray.getDimensionPixelSize(
            R.styleable.SearchBarView_trpSearchHorizontalPadding,
            DEFAULT_HORIZONTAL_PADDING_DP.dp
        )
        val defaultTextColor = ContextCompat.getColor(context, R.color.trp_text_primary)
        val textColor = typedArray.getColor(
            R.styleable.SearchBarView_trpSearchTextColor,
            defaultTextColor
        )
        val hintColor = typedArray.getColor(
            R.styleable.SearchBarView_trpSearchHintColor,
            defaultTextColor
        )
        typedArray.recycle()

        binding.root.setBackgroundResource(backgroundRes)
        binding.ivSearch.setImageResource(iconRes)
        if (iconTint != NO_TINT) binding.ivSearch.setColorFilter(iconTint)
        binding.etInput.setTextColor(textColor)
        binding.etInput.setHintTextColor(hintColor)

        binding.ivSearch.updateLayoutParams<MarginLayoutParams> {
            width = iconSizePx
            height = iconSizePx
            marginStart = horizontalPaddingPx
        }
        binding.ivClear.updateLayoutParams<MarginLayoutParams> {
            width = iconSizePx
            height = iconSizePx
            marginEnd = horizontalPaddingPx
        }
        binding.etInput.updateLayoutParams<MarginLayoutParams> {
            marginEnd = horizontalPaddingPx + iconSizePx + ICON_GAP_DP.dp
        }
    }

    private fun setupTextWatcher() {
        binding.etInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                handleTextChanged(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun handleTextChanged(text: String) {
        updateUIState(text.isNotEmpty())
        onTextChangedListener?.invoke(text)
        cancelPendingQuery()

        val query = queryOf(text)
        if (mode == Mode.LOCAL || query.isEmpty()) {
            publishQuery(query)
            return
        }

        val runnable = Runnable { publishQuery(query) }
        pendingQuery = runnable
        debounceHandler.postDelayed(runnable, debounceMs)
    }

    private fun queryOf(text: String): String = if (text.isBlank()) "" else text

    private fun publishQuery(query: String) {
        if (query == lastPublishedQuery) return
        lastPublishedQuery = query
        onQueryChangedListener?.invoke(query)
    }

    private fun cancelPendingQuery() {
        pendingQuery?.let { debounceHandler.removeCallbacks(it) }
        pendingQuery = null
    }

    private fun setupClearButton() {
        binding.ivClear.setOnClickListener {
            binding.etInput.text?.clear()
            binding.etInput.requestFocus()
        }
    }

    private fun setupSearchAction() {
        binding.etInput.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH, EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_GO -> {
                    submitPendingQuery()
                    hideKeyboard()
                    onSearchActionListener?.invoke(getText())
                    true
                }
                else -> false
            }
        }
    }

    private fun updateUIState(hasText: Boolean) {
        binding.ivSearch.visibility = if (hasText) View.GONE else View.VISIBLE
        binding.ivClear.visibility = if (hasText) View.VISIBLE else View.GONE
        binding.etInput.typeface = if (hasText) mediumFont else lightFont
        binding.etInput.updateLayoutParams<MarginLayoutParams> {
            marginStart = if (hasText) {
                horizontalPaddingPx
            } else {
                horizontalPaddingPx + iconSizePx + ICON_GAP_DP.dp
            }
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
        binding.etInput.clearFocus()
    }

    fun setHint(hint: String) {
        binding.etInput.hint = hint
    }

    fun setHint(resId: Int) {
        binding.etInput.setHint(resId)
    }

    fun getText(): String = binding.etInput.text?.toString() ?: ""

    fun setText(text: String) {
        binding.etInput.setText(text)
    }

    fun clear() {
        binding.etInput.text?.clear()
    }

    fun hasText(): Boolean = binding.etInput.text?.isNotEmpty() == true

    /**
     * Publishes the query typed so far, cancelling any debounce still waiting.
     */
    fun submitPendingQuery() {
        cancelPendingQuery()
        publishQuery(queryOf(getText()))
    }

    /**
     * @param mode [Mode.REMOTE] waits [debounceMs] after the last keystroke before
     *   publishing; [Mode.LOCAL] publishes every keystroke.
     */
    fun setOnQueryChangedListener(
        mode: Mode = Mode.REMOTE,
        debounceMs: Long = DEFAULT_DEBOUNCE_MS,
        listener: (String) -> Unit
    ) {
        this.mode = mode
        this.debounceMs = debounceMs
        onQueryChangedListener = listener
    }

    /**
     * Fires on every keystroke regardless of [Mode], ahead of the debounce.
     * Meant for view-state updates only — the query itself belongs to
     * [setOnQueryChangedListener].
     */
    fun setOnTextChangedListener(listener: (String) -> Unit) {
        onTextChangedListener = listener
    }

    /**
     * Called after the keyboard's search key already published the query and
     * dismissed the keyboard.
     */
    fun setOnSearchActionListener(listener: (String) -> Unit) {
        onSearchActionListener = listener
    }

    override fun onDetachedFromWindow() {
        cancelPendingQuery()
        super.onDetachedFromWindow()
    }

    private companion object {
        const val DEFAULT_DEBOUNCE_MS = 600L
        const val DEFAULT_ICON_SIZE_DP = 24
        const val DEFAULT_HORIZONTAL_PADDING_DP = 18
        const val ICON_GAP_DP = 8
        const val NO_TINT = 0
    }
}
