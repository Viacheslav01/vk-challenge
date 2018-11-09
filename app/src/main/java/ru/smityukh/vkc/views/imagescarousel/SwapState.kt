package ru.smityukh.vkc.views.imagescarousel

import android.view.View

abstract class SwapState : ImagesCarouselView.State() {

    override fun init(delegate: ImagesCarouselView.StateDelegate) {
        super.init(delegate)

        val newIndex = getNextImageIndex()
        if (newIndex < 0 || newIndex == delegate.currentImageIndex) {
            setState(CommonState())
            return;
        }

        val imageInfo = delegate.getImageInfo(newIndex)
        delegate.loadImageToBack(imageInfo)

        val animator = delegate.animator
        animator.cancel()
        animator.currentPlayTime = 0
        animator.start()

        delegate.currentImageIndex = newIndex
        delegate.frontImageView
        delegate.backImageView
    }

    override fun onAnimatedValueChanged(value: Float) {
        val offset = getOffset(value)

        delegate.frontImageView.translationX = offset

        if (offset <= 0) {
            val targetOffset = delegate.frontImageView.width
            delegate.backImageView.translationX = targetOffset + offset
        } else {
            val targetOffset = delegate.backImageView.width
            delegate.backImageView.translationX = -targetOffset + offset
        }
    }

    override fun onAnimationFinished(canceled: Boolean) {
        if (canceled) {
            return
        }

        delegate.swapImageRoles()
        delegate.backImageView.visibility = View.INVISIBLE

        setState(CommonState())
    }

    abstract fun getOffset(progress: Float): Float
    abstract fun getNextImageIndex(): Int;
}