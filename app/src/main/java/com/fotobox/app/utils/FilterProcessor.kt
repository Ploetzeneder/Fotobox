package com.fotobox.app.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.fotobox.app.data.models.PhotoFilter

object FilterProcessor {

    fun applyFilter(source: Bitmap, filter: PhotoFilter): Bitmap {
        if (filter == PhotoFilter.NONE) return source
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val cm = buildColorMatrix(filter)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    fun buildColorMatrix(filter: PhotoFilter): ColorMatrix {
        val cm = ColorMatrix()
        when (filter) {
            PhotoFilter.NONE -> Unit
            PhotoFilter.BLACK_WHITE -> cm.set(
                floatArrayOf(
                    0.33f, 0.59f, 0.11f, 0f, 0f,
                    0.33f, 0.59f, 0.11f, 0f, 0f,
                    0.33f, 0.59f, 0.11f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            PhotoFilter.SEPIA -> cm.set(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            PhotoFilter.VIVID -> cm.setSaturation(2.2f)
            PhotoFilter.COOL -> cm.set(
                floatArrayOf(
                    0.85f, 0f, 0f, 0f, 0f,
                    0f, 0.95f, 0f, 0f, 0f,
                    0f, 0f, 1.25f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            PhotoFilter.WARM -> cm.set(
                floatArrayOf(
                    1.25f, 0f, 0f, 0f, 0f,
                    0f, 1.0f, 0f, 0f, 0f,
                    0f, 0f, 0.75f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            PhotoFilter.VINTAGE -> {
                val sat = ColorMatrix()
                sat.setSaturation(0.6f)
                val faded = ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, 25f,
                        0f, 1f, 0f, 0f, 10f,
                        0f, 0f, 1f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                faded.preConcat(sat)
                cm.set(faded)
            }
            PhotoFilter.HIGH_CONTRAST -> cm.set(
                floatArrayOf(
                    2.0f, 0f, 0f, 0f, -128f,
                    0f, 2.0f, 0f, 0f, -128f,
                    0f, 0f, 2.0f, 0f, -128f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            PhotoFilter.FADED -> {
                val sat = ColorMatrix()
                sat.setSaturation(0.75f)
                val lift = ColorMatrix(
                    floatArrayOf(
                        0.85f, 0f, 0f, 0f, 30f,
                        0f, 0.85f, 0f, 0f, 25f,
                        0f, 0f, 0.85f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                lift.preConcat(sat)
                cm.set(lift)
            }
        }
        return cm
    }
}
