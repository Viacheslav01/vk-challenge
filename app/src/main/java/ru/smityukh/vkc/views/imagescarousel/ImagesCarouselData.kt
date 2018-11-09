package ru.smityukh.vkc.views.imagescarousel

interface ImagesCarouselData {
    val count: Int
    fun getImageInfo(index: Int): ImageInfo

    interface ImageInfo {
        val url: String
    }
}