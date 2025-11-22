package com.tripian.trpcore.util.fragment

import android.text.TextUtils
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * Created by Semih Özköroğlu on 29.09.2019
 */
class FragmentFactory {
    private var mFragment: Fragment? = null
    private var mDialogFragment: DialogFragment? = null

    var mManager: FragmentManager? = null
    var mTag: String? = null
    lateinit var mAnimationType: AnimationType
    lateinit var mTransitionType: TransitionType
    var clearBackStack: Boolean = false
    var mViewId: Int = -1
    var mView: View? = null
    var addToBackStack: Boolean = false

    private constructor(builder: Builder) {
        if (builder.mDialogFragment != null) {
            mDialogFragment = builder.mDialogFragment
        } else {
            mFragment = builder.mFragment
        }
        mAnimationType = builder.mAnimationType
        mTag = builder.mTag
        mViewId = builder.mViewId
        mView = builder.mView
        addToBackStack = builder.addToBackStack
        mTransitionType = builder.mTransitionType
        clearBackStack = builder.clearBackStack
        mManager = builder.mManager
    }

    fun getFragment(): Fragment? {
        return mFragment
    }

    fun getAnimationType(): AnimationType {
        return mAnimationType
    }

    fun getTag(): String? {
        return mTag
    }

    fun getViewId(): Int {
        return mViewId
    }

    fun getView(): View? {
        return mView
    }

    fun addToBackStack(): Boolean {
        return addToBackStack
    }

    fun getTransitionType(): TransitionType? {
        return mTransitionType
    }

    fun getDialogFragment(): DialogFragment? {
        return mDialogFragment
    }

    fun isDialog(): Boolean {
        return mDialogFragment != null
    }

    fun isClearBackStack(): Boolean {
        return clearBackStack
    }

    fun getManager(): FragmentManager? {
        return mManager
    }

    fun setManager(mManager: FragmentManager) {
        this.mManager = mManager
    }

    class Builder
    /**
     * Creates a new [Builder]
     *
     * @param fragment [Fragment] that will be added / replaced
     */(fragment: Fragment) {
        // Default
        var mManager: FragmentManager? = null
        var mTag: String? = null

        var mFragment: Fragment? = null
        var mDialogFragment: DialogFragment? = null

        var mAnimationType: AnimationType = AnimationType.NO_ANIM
        var mView: View? = null
        var mTransitionType: TransitionType = TransitionType.REPLACE
        var addToBackStack: Boolean = false
        var mViewId: Int = -1
        var clearBackStack: Boolean = false

        init {
            if (fragment is DialogFragment) {
                mDialogFragment = fragment
            } else {
                mFragment = fragment
                mTag = fragment.javaClass.simpleName
            }
        }

        fun setViewId(viewId: Int): Builder {
            mViewId = viewId
            return this
        }

        fun setView(view: View): Builder {
            mView = view
            return this
        }

        fun setTag(tag: String): Builder {
            if (TextUtils.isEmpty(tag)) {
                throw NullPointerException("Tag can't be null")
            }

            mTag = tag
            return this
        }

        fun addToBackStack(addToBackStack: Boolean): Builder {
            this.addToBackStack = addToBackStack
            return this
        }

        /**
         * Set an [AnimationType] that [android.support.v4.app.FragmentTransaction] will use as enter / exit animations
         * Default is [AnimationType.NO_ANIM]
         *
         * @param animationType [AnimationType] type of animation
         * @return this class for chaining
         */
        fun setAnimation(animationType: AnimationType?): Builder {
            if (animationType == null) {
                throw NullPointerException("AnimationType can't be null")
            }

            mAnimationType = animationType
            return this
        }

        fun setTransitionType(type: TransitionType?): Builder {
            if (type == null) {
                throw NullPointerException("TransitionType can't be null")
            }

            this.mTransitionType = type
            return this
        }

        fun setClearBackStack(clearBackStack: Boolean): Builder {
            this.clearBackStack = clearBackStack
            return this
        }

        fun setFragmentManager(mManager: FragmentManager?): Builder {
            this.mManager = mManager
            return this
        }

        /**
         * Builds a new [FragmentFactory] with set properties
         *
         * @return new [FragmentFactory]
         */
        fun build(): FragmentFactory {
            return FragmentFactory(this)
        }
    }
}