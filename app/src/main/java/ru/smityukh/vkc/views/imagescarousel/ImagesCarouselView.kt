package ru.smityukh.vkc.views.imagescarousel

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.support.annotation.CallSuper
import android.support.annotation.UiThread
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.images_carousel_view.view.*
import ru.smityukh.vkc.R
import ru.smityukh.vkc.transformations.SquareTransformation
import ru.smityukh.vkc.utils.FinishAnimatorListener

class ImagesCarouselView : FrameLayout {

    private lateinit var frontImageView: ImageView
    private lateinit var backImageView: ImageView

    private var currentImageIndex: Int = 0
        set(value) {
            field = value
            timelineView.currentItem = value
        }

    private val stateDelegate: StateDelegate
    private lateinit var state: State

    init {
        stateDelegate = StateDelegate()
        stateDelegate.setState(CommonState())
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, R.attr.imagesCarouselViewStyle) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context)
    }

    private fun init(context: Context) {
        inflate(context, R.layout.images_carousel_view, this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // TODO: No time to search a nice solution, I need a square right now
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        frontImageView = firstImage
        backImageView = secondImage

        leftClickArea.setOnClickListener { onSwapLeft() }
        rightClickArea.setOnClickListener { onSwapRight() }
    }

    private val _animator: ValueAnimator by lazy {
        val valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f)
        valueAnimator.duration = resources.getInteger(R.integer.image_carousel_swap_duration).toLong()
        valueAnimator.interpolator = DecelerateInterpolator()

        valueAnimator.addListener(object : FinishAnimatorListener() {
            override fun onAnimationFinished(canceled: Boolean) {
                state.onAnimationFinished(canceled)
            }

            override fun onAnimationStart(animation: Animator) {
                state.onAnimationStarted()
            }
        })

        valueAnimator.addUpdateListener { state.onAnimatedValueChanged(it.animatedValue as Float) }

        valueAnimator
    }

    private fun swapImageRoles() {
        val tmp = frontImageView
        frontImageView = backImageView
        backImageView = tmp
    }

    private fun onSwapRight() {
        state.swapRight()
    }

    private fun onSwapLeft() {
        state.swapLeft()
    }

    private fun getNextRightIndex(): Int {
        val data = dataSource
        if (data == null || data.count == 0) {
            return -1
        }

        if (data.count == 1) {
            return 0
        }

        val newValue = currentImageIndex + 1
        if (newValue >= data.count) {
            return 0
        }

        return newValue
    }

    private fun getNextLeftIndex(): Int {
        val data = dataSource
        if (data == null || data.count == 0) {
            return -1
        }

        if (data.count == 1) {
            return 0
        }

        if (currentImageIndex <= 0) {
            return data.count - 1
        }

        return currentImageIndex - 1
    }

    var dataSource: ImagesCarouselData? = null
        set(value) {
            if (field === value) {
                return
            }
            field = value

            onDataSourceChanged()
        }

    private fun onDataSourceChanged() {
        Picasso.get().cancelTag(this)

        val data = dataSource
        if (data == null || data.count == 0) {
            timelineView.itemsCount = 0

            frontImageView.setImageResource(R.drawable.progress)
            backImageView.setImageResource(R.drawable.carousel_empty_drawable)
            backImageView.visibility = View.INVISIBLE
            return
        }

        timelineView.itemsCount = data.count
        currentImageIndex = 0
        backImageView.setImageResource(R.drawable.carousel_empty_drawable)
        backImageView.visibility = View.INVISIBLE

        loadImageTo(frontImageView, data.getImageInfo(currentImageIndex).url)
    }

    private fun loadImageTo(imageView: ImageView, url: String) {
        Picasso
            .get()
            .load(url)
            .placeholder(R.drawable.progress)
            .error(R.drawable.error)
            .transform(SquareTransformation.Instance)
            .tag(this)
            .into(imageView)
    }

    inner class StateDelegate {
        @UiThread
        fun setState(state: State) {
            this@ImagesCarouselView.state = state
            this@ImagesCarouselView.state.init(stateDelegate)
        }

        fun loadImageToFront(imageInfo: ImagesCarouselData.ImageInfo) {
            loadImageTo(this@ImagesCarouselView.frontImageView, imageInfo.url)
        }

        fun loadImageToBack(imageInfo: ImagesCarouselData.ImageInfo) {
            loadImageTo(this@ImagesCarouselView.backImageView, imageInfo.url)
        }

        fun getImageInfo(index: Int): ImagesCarouselData.ImageInfo =
            dataSource?.getImageInfo(index) ?: throw IllegalStateException()

        fun getNextRightIndex(): Int = this@ImagesCarouselView.getNextRightIndex()

        fun getNextLeftIndex(): Int = this@ImagesCarouselView.getNextLeftIndex()

        fun swapImageRoles() {
            this@ImagesCarouselView.swapImageRoles()
        }

        val animator: ValueAnimator
            get() = _animator

        val frontImageView: ImageView
            get() = this@ImagesCarouselView.frontImageView

        val backImageView: ImageView
            get() = this@ImagesCarouselView.backImageView

        var currentImageIndex: Int
            get() = this@ImagesCarouselView.currentImageIndex
            set(value) {
                this@ImagesCarouselView.currentImageIndex = value
            }
    }

    abstract class State {
        protected lateinit var delegate: StateDelegate

        protected fun setState(state: State) {
            checkNotNull(delegate)
            delegate.setState(state)
        }

        @CallSuper
        open fun init(delegate: StateDelegate) {
            this.delegate = delegate
        }

        open fun onAnimationStarted() {
        }

        open fun onAnimationFinished(canceled: Boolean) {
        }

        open fun onAnimatedValueChanged(value: Float) {
        }

        open fun swapRight() {
        }

        open fun swapLeft() {
        }
    }
}