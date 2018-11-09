package ru.smityukh.vkc.utils

import android.animation.Animator

open class FinishAnimatorListener : SimpleAnimatorListener() {
    private var _canceled = false

    override fun onAnimationEnd(animation: Animator) {
        onAnimationFinished(_canceled)
    }

    override fun onAnimationCancel(animation: Animator) {
        _canceled = true
    }

    open fun onAnimationFinished(canceled: Boolean) {
    }
}