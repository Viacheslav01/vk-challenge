package ru.smityukh.vkc.views.cardview

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.card_juggler_view.view.*
import ru.smityukh.vkc.R
import ru.smityukh.vkc.data.DataRequestCallback
import ru.smityukh.vkc.utils.FinishAnimatorListener
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.sign

class CardsJugglerView : FrameLayout {

    private lateinit var frontCardView: CardView
    private lateinit var backCardView: CardView

    private val touchSlop: Int

    private var isScrolling: Boolean = false
    private var downEvent: MotionEvent? = null
    private var moveEvent: MotionEvent? = null

    companion object {
        const val MAX_ANGLE: Float = 30f
        const val ACTION_NONE: Int = 0
        const val ACTION_LIKE: Int = 1
        const val ACTION_SKIP: Int = 2

        fun getActionByAngle(angle: Float): Int {
            if (angle > 0.5f) {
                return ACTION_LIKE
            }

            if (angle < -0.5f) {
                return ACTION_SKIP
            }

            return ACTION_NONE
        }
    }

    constructor(context: Context) : super(context) {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        init(context)
    }

    //TODO: Ad default style in case of enough time
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        init(context)
    }

    private fun init(context: Context) {
        inflate(context, R.layout.card_juggler_view, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        frontCardView = firstCard
        backCardView = secondCard

        backCardView.visibility = View.INVISIBLE
        actionLabelLike.visibility = View.INVISIBLE
        actionLabelSkip.visibility = View.INVISIBLE

        onDataSourceChanged()
    }

    var dataSource: CardJugglerDataSource? = null
        set(value) {
            if (field == value) {
                return
            }

            field = value
            onDataSourceChanged()
        }

    private val requestCallback: DataRequestCallback<VkPostData> = object : DataRequestCallback<VkPostData> {
        override fun start() {
            post { state.onStartLoadData() }
        }

        override fun error() {
            post { state.onLoadDataError() }
        }

        override fun success(data: VkPostData) {
            post { state.bindData(data) }
        }
    }

    private fun onDataSourceChanged() {
        cleanUpData()

        val data = dataSource
        if (data == null) {
            state = NoDataState()
            return
        }

        state = LoadFirstState()
        state.loadData()
    }

    private fun cleanUpData() {
        frontCardView.dataSource = null
        backCardView.dataSource = null
    }

    private fun recycleEvents() {
        downEvent?.recycle()
        moveEvent?.recycle()
        downEvent = null
        moveEvent = null
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downEvent?.recycle()
                downEvent = MotionEvent.obtain(event)
                return false
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                recycleEvents()
                if (isScrolling) {
                    isScrolling = false
                    onScrollEnd()
                }

                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (isScrolling) {
                    return true
                }

                val lastDown = downEvent
                if (lastDown == null) {
                    return false
                }

                val deltaX = abs(lastDown.x - event.x)
                val deltaY = abs(lastDown.y - event.y)

                if (deltaX >= touchSlop || deltaY >= touchSlop) {
                    isScrolling = true
                    return true
                }

                return false;
            }
            else -> {
                return false
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                return true
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                recycleEvents()

                if (isScrolling) {
                    isScrolling = false
                    onScrollEnd()
                }

                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val lastDown = downEvent
                if (lastDown == null) {
                    return super.onTouchEvent(event)
                }

                val totalX = lastDown.x - event.x
                val totalY = lastDown.y - event.y

                isScrolling = isScrolling || (abs(totalX) >= touchSlop || abs(totalY) >= touchSlop)
                if (!isScrolling) {
                    return super.onTouchEvent(event);
                }

                val lastMove = moveEvent
                val deltaX = lastMove?.let { it.x - event.x } ?: totalX
                val deltaY = lastMove?.let { it.y - event.y } ?: totalY

                moveEvent?.recycle()
                moveEvent = MotionEvent.obtain(event)

                if (abs(deltaX) < 1 && abs(deltaY) < 1) {
                    return super.onTouchEvent(event);
                }

                onScrollGesture(event.x, event.y, totalX, totalY, deltaX, deltaY)
                return true;
            }
        }

        return super.onTouchEvent(event)
    }

    private fun onScrollGesture(x: Float, y: Float, totalX: Float, totalY: Float, deltaX: Float, deltaY: Float) {
        state.onScrollGesture(x, y, totalX, totalY, deltaX, deltaY)
    }

    private fun onScrollEnd() {
        state.onScrollEnd()
    }

    private val universeX: Float
        get() = (width / 2).toFloat()

    private val universeY: Float
        get() = (height * 2).toFloat()

    private fun rotateCard(angle: Float) {
        var resultAngle = angle
        if (abs(resultAngle) > MAX_ANGLE) {
            resultAngle = MAX_ANGLE * sign(resultAngle)
        }

        frontCardView.pivotX = (frontCardView.width / 2).toFloat()
        frontCardView.pivotY = universeY - frontCardView.top
        frontCardView.rotation = resultAngle

        showBackCard(abs(resultAngle) / MAX_ANGLE)
        showActionLabel(resultAngle)
    }

    private fun showBackCard(progress: Float) {
        if (progress <= 0.001f) {
            backCardView.visibility = View.INVISIBLE
            return;
        }

        backCardView.visibility = View.VISIBLE

        if (progress >= 0.999f) {
            backCardView.scaleX = 1.0f
            backCardView.scaleY = 1.0f
        }

        var scale = 0.9f + 0.1f * progress;
        backCardView.scaleX = scale
        backCardView.scaleY = scale
    }

    private fun showActionLabel(angle: Float) {
        val action = getActionByAngle(angle)

        if (action == ACTION_NONE) {
            actionLabelLike.visibility = View.INVISIBLE
            actionLabelSkip.visibility = View.INVISIBLE
            return;
        }

        val absAngle = abs(angle)
        var resultAngle = (absAngle - 4) * sign(angle)
        var alpha = if (absAngle > 4.0f) 1.0f else absAngle / 4.0f

        var actionLabel: View
        if (action == ACTION_LIKE) {
            actionLabel = actionLabelLike
            actionLabelSkip.visibility = View.INVISIBLE
        } else {
            actionLabel = actionLabelSkip
            actionLabelLike.visibility = View.INVISIBLE
        }

        actionLabel.visibility = View.VISIBLE
        actionLabel.alpha = alpha

        actionLabel.pivotX = (actionLabel.width / 2).toFloat()
        actionLabel.pivotY = universeY - actionLabel.top
        actionLabel.rotation = resultAngle
    }

    private fun swapCards() {
        val tmpCard = frontCardView;
        frontCardView = backCardView;
        backCardView = tmpCard
    }

    fun likeCurrentCard() {
        state.likeCurrentCard()
    }

    fun skipCurrentCard() {
        state.skipCurrentCard()
    }

    private var state: State = NoDataState()
        set(value) {
            field = value
            field.init()
        }

    private abstract inner class State {
        open fun onScrollGesture(x: Float, y: Float, totalX: Float, totalY: Float, deltaX: Float, deltaY: Float) {}
        open fun onScrollEnd() {}
        open fun init() {}

        open fun onStartLoadData() {}
        open fun onLoadDataError() {}

        open fun bindData(data: VkPostData) {
            backCardView.dataSource = data.cardViewData
        }

        open fun loadData() {
            val data = dataSource
            checkNotNull(data)

            data.getPost(requestCallback)
        }

        open fun skipCurrentCard() {
        }

        open fun likeCurrentCard() {
        }
    }

    private inner class NoDataState : State() {
        override fun loadData() {
        }
    }

    private inner class LoadFirstState : State() {
        override fun bindData(data: VkPostData) {
            frontCardView.dataSource = data.cardViewData

            state = CommonState()
            state.loadData()
        }
    }

    private inner class CommonState : State() {
        override fun onScrollGesture(x: Float, y: Float, totalX: Float, totalY: Float, deltaX: Float, deltaY: Float) {
            state = SwingState()
            state.onScrollGesture(x, y, totalX, totalY, deltaX, deltaY)
        }

        override fun bindData(data: VkPostData) {
            backCardView.dataSource = data.cardViewData
        }

        override fun onStartLoadData() {
        }

        override fun skipCurrentCard() {
            swapCards()

            backCardView.dataSource?.skip()

            frontCardView.visibility = View.VISIBLE
            frontCardView.bringToFront()
            frontCardView.scaleX = 1.0f
            frontCardView.scaleY = 1.0f
            frontCardView.rotation = 0f

            backCardView.dataSource = null
            backCardView.visibility = View.INVISIBLE
            backCardView.scaleX = 1.0f
            backCardView.scaleY = 1.0f
            backCardView.rotation = 0f
            backCardView.pivotX = (backCardView.width / 2).toFloat()
            backCardView.pivotY = (backCardView.height / 2).toFloat()

            if (frontCardView.dataSource == null) {
                state = LoadFirstState()
                return
            }

            state.loadData()
        }

        override fun likeCurrentCard() {
            swapCards()

            backCardView.dataSource?.like()

            frontCardView.visibility = View.VISIBLE
            frontCardView.bringToFront()
            frontCardView.scaleX = 1.0f
            frontCardView.scaleY = 1.0f
            frontCardView.rotation = 0f

            backCardView.dataSource = null
            backCardView.visibility = View.INVISIBLE
            backCardView.scaleX = 1.0f
            backCardView.scaleY = 1.0f
            backCardView.rotation = 0f
            backCardView.pivotX = (backCardView.width / 2).toFloat()
            backCardView.pivotY = (backCardView.height / 2).toFloat()

            if (frontCardView.dataSource == null) {
                state = LoadFirstState()
                return
            }

            state.loadData()
        }
    }

    private inner class ExpandedState : State() {
    }

    private inner class SwingState : State() {
        private var angle: Float = 0f

        override fun onScrollGesture(x: Float, y: Float, totalX: Float, totalY: Float, deltaX: Float, deltaY: Float) {
            val height = universeY - y;
            val width = totalX

            if (height == 0f) {
                return;
            }

            angle = -Math.toDegrees(atan(width / height).toDouble()).toFloat()
            rotateCard(angle)
        }

        override fun onScrollEnd() {
            if (abs(angle) > 10f) {
                state = AwayAnimationState(angle)
                return;
            }

            state = ReturnAnimationState(angle)
        }
    }

    private abstract inner class AnimationState(val fromAngle: Float) : State() {
        override fun init() {

            var duration = resources.getInteger(R.integer.card_rotate_duration).toDouble()
            duration *= abs(toAngle - fromAngle) / MAX_ANGLE

            val valueAnimator = ValueAnimator.ofFloat(fromAngle, toAngle)
            valueAnimator.duration = duration.toLong()
            valueAnimator.interpolator = DecelerateInterpolator()

            valueAnimator.addListener(object : FinishAnimatorListener() {
                override fun onAnimationFinished(canceled: Boolean) {
                    this@AnimationState.onAnimationFinished(canceled)
                }
            })

            valueAnimator.addUpdateListener {
                rotateCard(it.animatedValue as Float)
            }

            valueAnimator.start()
        }

        abstract val toAngle: Float

        open fun onAnimationFinished(canceled: Boolean) {
        }
    }

    private inner class ReturnAnimationState(fromAngle: Float) : AnimationState(fromAngle) {
        override val toAngle: Float = 0f

        override fun onAnimationFinished(canceled: Boolean) {
            if (canceled) {
                return
            }

            state = CommonState()
        }
    }

    private inner class AwayAnimationState(fromAngle: Float) : AnimationState(fromAngle) {
        override val toAngle: Float = MAX_ANGLE * sign(fromAngle)

        override fun onAnimationFinished(canceled: Boolean) {
            swapCards()

            when (getActionByAngle(fromAngle)) {
                ACTION_LIKE -> {
                    backCardView.dataSource?.like()
                }
                ACTION_SKIP -> {
                    backCardView.dataSource?.skip()
                }
            }

            frontCardView.bringToFront()
            frontCardView.scaleX = 1.0f
            frontCardView.scaleY = 1.0f
            frontCardView.rotation = 0f

            backCardView.dataSource = null
            backCardView.visibility = View.INVISIBLE
            backCardView.scaleX = 1.0f
            backCardView.scaleY = 1.0f
            backCardView.rotation = 0f
            backCardView.pivotX = (backCardView.width / 2).toFloat()
            backCardView.pivotY = (backCardView.height / 2).toFloat()

            if (frontCardView.dataSource == null) {
                state = LoadFirstState()
                return
            }

            state = CommonState()
            state.loadData()
        }
    }
}

interface VkPostData {
    val cardViewData: CardViewData
}

interface CardJugglerDataSource {
    fun getPost(callback: DataRequestCallback<VkPostData>)
}