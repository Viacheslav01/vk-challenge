package ru.smityukh.vkc.views.imagescarousel

import android.view.View

class SwapRightState : SwapState() {
    override fun onAnimationStarted() {
        val frontImageView = delegate.frontImageView
        val backImageView = delegate.backImageView

        frontImageView.translationX = 0f
        backImageView.translationX = frontImageView.width.toFloat()
        backImageView.visibility = View.VISIBLE
    }

    override fun getOffset(progress: Float): Float {
        val targetOffset = delegate.frontImageView.width
        return -targetOffset.toFloat() * progress
    }

    override fun getNextImageIndex(): Int = delegate.getNextRightIndex()
}