package ru.smityukh.vkc.views.cardview

import ru.smityukh.vkc.views.imagescarousel.ImagesCarouselData

interface CardViewData {
    val avatarUri: String
    val userName: String
    val timestamp: String
    val carouselData: ImagesCarouselData
    val description: String

    fun like()
    fun skip()
}