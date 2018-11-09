package ru.smityukh.vkc.transformations

import android.graphics.Bitmap
import com.squareup.picasso.Transformation
import kotlin.math.min

class SquareTransformation : Transformation {
    companion object {
        val Instance: Transformation = SquareTransformation()
    }

    override fun key(): String = "SquareTransformation"

    override fun transform(source: Bitmap): Bitmap {
        val sideSize = min(source.width, source.height)

        val xOffset = (source.width - sideSize) / 2

        val bitmap = Bitmap.createBitmap(source, xOffset, 0, sideSize, sideSize)
        if (bitmap !== source) {
            source.recycle()
        }

        return bitmap
    }
}