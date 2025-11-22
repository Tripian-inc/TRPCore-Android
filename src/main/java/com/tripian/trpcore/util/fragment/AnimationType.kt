package com.tripian.trpcore.util.fragment

import com.tripian.trpcore.R

/**
 * Created by Semih Özköroğlu on 29.09.2019
 */
enum class AnimationType {
    ENTER_FROM_LEFT,
    ENTER_FROM_RIGHT,
    ENTER_FROM_BOTTOM,
    ENTER_WITH_ALPHA,
    ENTER_FROM_RIGHT_STACK,
    ENTER_FROM_RIGHT_NO_ENTRANCE,
    NO_ANIM,
    ENTER_FROM_BOTTOM_SLOWER;

    companion object {
        fun getAnimation(type: AnimationType): List<Int> {
            // TODO: animasyonlarin simdilik hepsi eklenmedi kullanildica eklenebilir
            when (type) {
                ENTER_FROM_LEFT -> return listOf(R.anim.anim_horizontal_fragment_in_from_pop, R.anim.anim_horizontal_fragment_out_from_pop, R.anim.anim_horizontal_fragment_in, R.anim.anim_horizontal_fragment_out)
                ENTER_FROM_RIGHT -> return listOf(R.anim.anim_horizontal_fragment_in, R.anim.anim_horizontal_fragment_out, R.anim.anim_horizontal_fragment_in_from_pop, R.anim.anim_horizontal_fragment_out_from_pop)
                ENTER_WITH_ALPHA -> return listOf(R.anim.anim_alphain, R.anim.anim_alphaout, R.anim.anim_alphain, R.anim.anim_alphaout)
//                ENTER_FROM_BOTTOM -> RuntimeException("Fragment animasyon tanimlanmali!!!")
//                ENTER_FROM_RIGHT_STACK -> RuntimeException("Fragment animasyon tanimlanmali!!!")
//                ENTER_FROM_RIGHT_NO_ENTRANCE -> RuntimeException("Fragment animasyon tanimlanmali!!!")
//                ENTER_FROM_BOTTOM_SLOWER -> RuntimeException("Fragment animasyon tanimlanmali!!!")
                else -> return listOf()
            }

            return listOf()
        }
    }
}