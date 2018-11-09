package ru.smityukh.vkc.views.cardview

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.card_view.view.*
import ru.smityukh.vkc.R
import ru.smityukh.vkc.transformations.SquareTransformation
import ru.smityukh.vkc.views.imagescarousel.ImagesCarouselData

class CardView : LinearLayout {

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs, R.attr.cardViewStyle) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context)
    }

    private fun init(context: Context) {
        val rootView: ViewGroup = inflate(context, R.layout.card_view, this) as ViewGroup

        rootView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val cornerRadius = context.resources.getDimensionPixelSize(R.dimen.vk_card_view_corner_radius)
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius.toFloat())
                outline.alpha = 0.5f
            }
        }

        userAvatarView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.width.toFloat() / 2)
            }
        }
        userAvatarView.clipToOutline = true

        onDataSourceChanged()
    }

    var dataSource: CardViewData? = null
        set(value) {
            if (field === value) {
                return
            }

            field = value
            onDataSourceChanged()
        }

    private fun onDataSourceChanged() {
        val data = dataSource
        if (data == null) {
            cleanUpData()
            return
        }

        bindData(data)
    }

    private fun cleanUpData() {
        bindAvatar(null)
        bindUserName("")
        bindTimestamp("")
        bindCarousel(null)
        bindDescription("")
    }

    private fun bindData(data: CardViewData) {
        bindAvatar(data.avatarUri)
        bindUserName(data.userName)
        bindTimestamp(data.timestamp)
        bindCarousel(data.carouselData)
        bindDescription(data.description)
    }

    private fun bindDescription(description: String) {
        descriptionTextView.text = description
    }

    private fun bindCarousel(carouselData: ImagesCarouselData?) {
        imagesCarouselView.dataSource = carouselData
    }

    private fun bindTimestamp(timestamp: String) {
        postTimestampView.text = timestamp
    }

    private fun bindUserName(userName: String) {
        userNameView.text = userName
    }

    private fun bindAvatar(avatarUri: String?) {
        Picasso
            .get()
            .load(avatarUri)
            .placeholder(R.drawable.avatar_placeholder)
            .error(R.drawable.avatar_placeholder)
            .transform(SquareTransformation.Instance)
            .tag(this)
            .into(userAvatarView)
    }
}