package com.fotobox.app.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.fotobox.app.data.models.StripLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StripComposer {

    private const val BORDER = 60
    private const val GAP = 20
    private const val FOOTER_HEIGHT = 80

    fun compose(
        photos: List<Bitmap>,
        layout: StripLayout,
        eventName: String = "",
        addTimestamp: Boolean = true
    ): Bitmap {
        val processed = photos.map { normalizeSize(it) }
        return when (layout) {
            StripLayout.SINGLE -> withFooter(processed[0], eventName, addTimestamp)
            StripLayout.DOUBLE -> composeColumn(processed.take(2), eventName, addTimestamp)
            StripLayout.STRIP_3 -> composeColumn(processed.take(3), eventName, addTimestamp)
            StripLayout.STRIP_4 -> composeColumn(processed.take(4), eventName, addTimestamp)
            StripLayout.GRID_4 -> composeGrid(processed.take(4), 2, 2, eventName, addTimestamp)
        }
    }

    private fun normalizeSize(bitmap: Bitmap): Bitmap {
        val targetW = 800
        val targetH = (targetW.toFloat() / bitmap.width * bitmap.height).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun composeColumn(
        photos: List<Bitmap>,
        eventName: String,
        addTimestamp: Boolean
    ): Bitmap {
        val cellW = photos[0].width
        val cellH = photos[0].height
        val footerH = if (eventName.isNotEmpty() || addTimestamp) FOOTER_HEIGHT else 0
        val totalW = BORDER * 2 + cellW
        val totalH = BORDER * 2 + photos.size * cellH + (photos.size - 1) * GAP + footerH
        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        photos.forEachIndexed { i, bmp ->
            val x = BORDER.toFloat()
            val y = (BORDER + i * (cellH + GAP)).toFloat()
            canvas.drawBitmap(bmp, x, y, null)
        }
        if (footerH > 0) drawFooter(canvas, totalW, totalH, footerH, eventName, addTimestamp)
        return result
    }

    private fun composeGrid(
        photos: List<Bitmap>,
        cols: Int,
        rows: Int,
        eventName: String,
        addTimestamp: Boolean
    ): Bitmap {
        val cellW = photos[0].width
        val cellH = photos[0].height
        val footerH = if (eventName.isNotEmpty() || addTimestamp) FOOTER_HEIGHT else 0
        val totalW = BORDER * 2 + cols * cellW + (cols - 1) * GAP
        val totalH = BORDER * 2 + rows * cellH + (rows - 1) * GAP + footerH
        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        photos.forEachIndexed { i, bmp ->
            val col = i % cols
            val row = i / cols
            val x = (BORDER + col * (cellW + GAP)).toFloat()
            val y = (BORDER + row * (cellH + GAP)).toFloat()
            canvas.drawBitmap(bmp, x, y, null)
        }
        if (footerH > 0) drawFooter(canvas, totalW, totalH, footerH, eventName, addTimestamp)
        return result
    }

    private fun withFooter(photo: Bitmap, eventName: String, addTimestamp: Boolean): Bitmap {
        val footerH = if (eventName.isNotEmpty() || addTimestamp) FOOTER_HEIGHT else 0
        val totalW = BORDER * 2 + photo.width
        val totalH = BORDER * 2 + photo.height + footerH
        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(photo, BORDER.toFloat(), BORDER.toFloat(), null)
        if (footerH > 0) drawFooter(canvas, totalW, totalH, footerH, eventName, addTimestamp)
        return result
    }

    private fun drawFooter(
        canvas: Canvas,
        totalW: Int,
        totalH: Int,
        footerH: Int,
        eventName: String,
        addTimestamp: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val footerY = (totalH - footerH).toFloat()
        val centerX = (totalW / 2).toFloat()

        if (eventName.isNotEmpty()) {
            paint.textSize = 36f
            canvas.drawText(eventName, centerX, footerY + 45f, paint)
        }
        if (addTimestamp) {
            paint.textSize = 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val dateStr = SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText(dateStr, centerX, footerY + 70f, paint)
        }
    }
}
