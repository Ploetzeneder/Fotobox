package com.fotobox.app.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
        return when (layout) {
            StripLayout.SINGLE -> {
                val p = normalizeWidth(photos[0])
                withFooter(p, eventName, addTimestamp)
            }
            StripLayout.STRIP_2 ->
                composeColumn(photos.take(2).map { normalizeWidth(it) }, eventName, addTimestamp)
            StripLayout.STRIP_3 ->
                composeColumn(photos.take(3).map { normalizeWidth(it) }, eventName, addTimestamp)
            StripLayout.STRIP_4 ->
                composeColumn(photos.take(4).map { normalizeWidth(it) }, eventName, addTimestamp)
            StripLayout.GRID_4 ->
                composeGrid(photos.take(4).map { normalizeWidth(it) }, 2, 2, eventName, addTimestamp)
            StripLayout.GRID_6 ->
                composeGrid(photos.take(6).map { normalizeWidth(it) }, 2, 3, eventName, addTimestamp)
            StripLayout.GRID_9 ->
                composeGrid(photos.take(9).map { normalizeSmall(it) }, 3, 3, eventName, addTimestamp)
            StripLayout.ROW_2 ->
                composeRow(photos.take(2).map { normalizeHeight(it) }, eventName, addTimestamp)
            StripLayout.ROW_3 ->
                composeRow(photos.take(3).map { normalizeHeight(it) }, eventName, addTimestamp)
            StripLayout.ROW_4 ->
                composeRow(photos.take(4).map { normalizeHeight(it) }, eventName, addTimestamp)
            StripLayout.HERO_PLUS_2 ->
                composeHero(photos.take(3), 2, eventName, addTimestamp)
            StripLayout.HERO_PLUS_3 ->
                composeHero(photos.take(4), 3, eventName, addTimestamp)
        }
    }

    private fun normalizeWidth(bitmap: Bitmap, targetW: Int = 800): Bitmap {
        val targetH = (targetW.toFloat() / bitmap.width * bitmap.height).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun normalizeHeight(bitmap: Bitmap, targetH: Int = 600): Bitmap {
        val targetW = (targetH.toFloat() / bitmap.height * bitmap.width).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun normalizeSmall(bitmap: Bitmap): Bitmap = normalizeWidth(bitmap, 520)

    private fun composeColumn(photos: List<Bitmap>, eventName: String, addTimestamp: Boolean): Bitmap {
        val cellW = photos[0].width
        val cellH = photos[0].height
        val footerH = footerHeight(eventName, addTimestamp)
        val totalW = BORDER * 2 + cellW
        val totalH = BORDER * 2 + photos.size * cellH + (photos.size - 1) * GAP + footerH
        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)
        photos.forEachIndexed { i, bmp ->
            canvas.drawBitmap(bmp, BORDER.toFloat(), (BORDER + i * (cellH + GAP)).toFloat(), null)
        }
        if (footerH > 0) drawFooter(canvas, totalW, totalH, footerH, eventName, addTimestamp)
        return result
    }

    private fun composeRow(photos: List<Bitmap>, eventName: String, addTimestamp: Boolean): Bitmap {
        val cellH = photos[0].height
        val footerH = footerHeight(eventName, addTimestamp)
        val totalW = BORDER * 2 + photos.sumOf { it.width } + (photos.size - 1) * GAP
        val totalH = BORDER * 2 + cellH + footerH
        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)
        var x = BORDER.toFloat()
        for (bmp in photos) {
            canvas.drawBitmap(bmp, x, BORDER.toFloat(), null)
            x += bmp.width + GAP
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
        val footerH = footerHeight(eventName, addTimestamp)
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

    private fun composeHero(
        photos: List<Bitmap>,
        sideCount: Int,
        eventName: String,
        addTimestamp: Boolean
    ): Bitmap {
        val hero = normalizeWidth(photos[0], 900)
        val heroW = hero.width
        val heroH = hero.height

        val sideTargetW = heroW / 2
        val sides = photos.drop(1).take(sideCount).map { bmp ->
            val h = (sideTargetW.toFloat() / bmp.width * bmp.height).toInt()
            Bitmap.createScaledBitmap(bmp, sideTargetW, h, true)
        }

        val footerH = footerHeight(eventName, addTimestamp)
        val sidesH = sides.sumOf { it.height } + (sides.size - 1) * GAP
        val contentH = maxOf(heroH, sidesH)
        val totalW = BORDER * 2 + heroW + GAP + sideTargetW
        val totalH = BORDER * 2 + contentH + footerH

        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        canvas.drawBitmap(hero, BORDER.toFloat(), BORDER.toFloat(), null)

        var sideY = BORDER.toFloat()
        val sideX = (BORDER + heroW + GAP).toFloat()
        for (bmp in sides) {
            canvas.drawBitmap(bmp, sideX, sideY, null)
            sideY += bmp.height + GAP
        }

        if (footerH > 0) drawFooter(canvas, totalW, totalH, footerH, eventName, addTimestamp)
        return result
    }

    private fun withFooter(photo: Bitmap, eventName: String, addTimestamp: Boolean): Bitmap {
        val footerH = footerHeight(eventName, addTimestamp)
        val totalW = BORDER * 2 + photo.width
        val totalH = BORDER * 2 + photo.height + footerH
        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(photo, BORDER.toFloat(), BORDER.toFloat(), null)
        if (footerH > 0) drawFooter(canvas, totalW, totalH, footerH, eventName, addTimestamp)
        return result
    }

    private fun footerHeight(eventName: String, addTimestamp: Boolean) =
        if (eventName.isNotEmpty() || addTimestamp) FOOTER_HEIGHT else 0

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
