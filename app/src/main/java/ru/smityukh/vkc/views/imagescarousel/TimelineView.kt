package ru.smityukh.vkc.views.imagescarousel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.annotation.ColorInt
import android.support.annotation.Px
import android.util.AttributeSet
import android.view.View
import ru.smityukh.vkc.R

class TimelineView : View {

    private lateinit var paint: Paint;

    @ColorInt
    var activeColor: Int = Color.WHITE
    @ColorInt
    var inactiveColor: Int = Color.GRAY
    @Px
    var separatorWidth: Int = 0
    @Px
    var cornerRadius: Int = 0

    var itemsCount: Int = 0
        set(value) {
            if (field == value) {
                return;
            }

            field = value
            if (field > 0) {
                currentItem = 0
            } else {
                currentItem = -1
            }

            invalidate()
        }

    var currentItem: Int = -1
        set(value) {
            if (field == value) {
                return;
            }

            field = value
            invalidate()
        }

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, R.attr.timelineViewStyle) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.TimelineView, defStyle, 0)

        activeColor = a.getColor(R.styleable.TimelineView_activeColor, activeColor)
        inactiveColor = a.getColor(R.styleable.TimelineView_inactiveColor, inactiveColor)
        separatorWidth = a.getDimensionPixelSize(R.styleable.TimelineView_separatorWidth, separatorWidth)
        cornerRadius = a.getDimensionPixelSize(R.styleable.TimelineView_cornerRadius, cornerRadius)

        a.recycle()

        paint = Paint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
        }

        invalidate()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (itemsCount <= 1) {
            return
        }

        val width = width - paddingLeft - paddingRight

        val itemWidth = (width - (separatorWidth * (itemsCount - 1))) / itemsCount
        val itemHeiht = height.toFloat()

        var x = paddingLeft.toFloat()

        for (index in 0 until itemsCount) {
            if (index == currentItem) {
                paint.color = activeColor
            } else {
                paint.color = inactiveColor
            }

            canvas.drawRoundRect(
                x,
                0f,
                x + itemWidth,
                itemHeiht,
                cornerRadius.toFloat(),
                cornerRadius.toFloat(),
                paint
            );

            x += itemWidth + separatorWidth
        }
    }
}
