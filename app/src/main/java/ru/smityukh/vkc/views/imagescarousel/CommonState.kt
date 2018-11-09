package ru.smityukh.vkc.views.imagescarousel

class CommonState : ImagesCarouselView.State() {
    override fun swapRight() {
        setState(SwapRightState())
    }

    override fun swapLeft() {
        setState(SwapLeftState())
    }
}