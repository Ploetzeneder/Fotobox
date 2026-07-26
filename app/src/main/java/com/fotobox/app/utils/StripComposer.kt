package com.fotobox.app.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.fotobox.app.data.models.FrameStyle
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
        addTimestamp: Boolean = true,
        backgroundColor: Int = Color.WHITE,
        logoBitmap: Bitmap? = null,
        frameStyle: FrameStyle = FrameStyle.NONE
    ): Bitmap {
        val result = composeLayout(photos, layout, eventName, addTimestamp, backgroundColor)
        if (frameStyle != FrameStyle.NONE) overlayFrame(result, frameStyle)
        if (logoBitmap != null) overlayLogo(result, logoBitmap)
        return result
    }

    private fun overlayFrame(bitmap: Bitmap, style: FrameStyle) {
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
        }
        val m = 14f
        val rect = RectF(m, m, bitmap.width - m, bitmap.height - m)
        when (style) {
            FrameStyle.NONE -> return
            FrameStyle.THIN_BLACK -> {
                paint.color = Color.argb(220, 0, 0, 0)
                paint.strokeWidth = 6f
                canvas.drawRect(rect, paint)
            }
            FrameStyle.THIN_WHITE -> {
                paint.color = Color.argb(220, 255, 255, 255)
                paint.strokeWidth = 6f
                canvas.drawRect(rect, paint)
            }
            FrameStyle.ROUNDED -> {
                paint.color = Color.argb(220, 255, 255, 255)
                paint.strokeWidth = 9f
                canvas.drawRoundRect(rect, 36f, 36f, paint)
            }
            FrameStyle.DOUBLE -> {
                paint.color = Color.argb(210, 20, 20, 20)
                paint.strokeWidth = 3f
                canvas.drawRect(rect, paint)
                val inner = RectF(m + 9, m + 9, bitmap.width - m - 9, bitmap.height - m - 9)
                canvas.drawRect(inner, paint)
            }
            FrameStyle.GOLD -> {
                paint.color = Color.argb(230, 212, 175, 55)
                paint.strokeWidth = 11f
                canvas.drawRect(rect, paint)
                paint.strokeWidth = 2f
                paint.color = Color.argb(180, 255, 230, 100)
                val inner = RectF(m + 14, m + 14, bitmap.width - m - 14, bitmap.height - m - 14)
                canvas.drawRect(inner, paint)
            }
        }
    }

    private fun composeLayout(
        photos: List<Bitmap>,
        layout: StripLayout,
        eventName: String,
        addTimestamp: Boolean,
        backgroundColor: Int
    ): Bitmap {
        return when (layout) {
            StripLayout.SINGLE -> {
                val p = normalizeWidth(photos[0])
                withFooter(p, eventName, addTimestamp, backgroundColor).also { p.recycle() }
            }
            StripLayout.STRIP_2 -> composeWithNormalized(
                photos.take(2), ::normalizeWidth, eventName, addTimestamp, backgroundColor
            ) { n -> composeColumn(n, eventName, addTimestamp, backgroundColor) }
            StripLayout.STRIP_3 -> composeWithNormalized(
                photos.take(3), ::normalizeWidth, eventName, addTimestamp, backgroundColor
            ) { n -> composeColumn(n, eventName, addTimestamp, backgroundColor) }
            StripLayout.STRIP_4 -> composeWithNormalized(
                photos.take(4), ::normalizeWidth, eventName, addTimestamp, backgroundColor
            ) { n -> composeColumn(n, eventName, addTimestamp, backgroundColor) }
            StripLayout.GRID_4 -> composeWithNormalized(
                photos.take(4), ::normalizeWidth, eventName, addTimestamp, backgroundColor
            ) { n -> composeGrid(n, 2, 2, eventName, addTimestamp, backgroundColor) }
            StripLayout.GRID_6 -> composeWithNormalized(
                photos.take(6), ::normalizeWidth, eventName, addTimestamp, backgroundColor
            ) { n -> composeGrid(n, 2, 3, eventName, addTimestamp, backgroundColor) }
            StripLayout.GRID_9 -> composeWithNormalized(
                photos.take(9), ::normalizeSmall, eventName, addTimestamp, backgroundColor
            ) { n -> composeGrid(n, 3, 3, eventName, addTimestamp, backgroundColor) }
            StripLayout.ROW_2 -> composeWithNormalized(
                photos.take(2), ::normalizeHeight, eventName, addTimestamp, backgroundColor
            ) { n -> composeRow(n, eventName, addTimestamp, backgroundColor) }
            StripLayout.ROW_3 -> composeWithNormalized(
                photos.take(3), ::normalizeHeight, eventName, addTimestamp, backgroundColor
            ) { n -> composeRow(n, eventName, addTimestamp, backgroundColor) }
            StripLayout.ROW_4 -> composeWithNormalized(
                photos.take(4), ::normalizeHeight, eventName, addTimestamp, backgroundColor
            ) { n -> composeRow(n, eventName, addTimestamp, backgroundColor) }
            StripLayout.HERO_PLUS_2 ->
                composeHero(photos.take(3), 2, eventName, addTimestamp, backgroundColor)
            StripLayout.HERO_PLUS_3 ->
                composeHero(photos.take(4), 3, eventName, addTimestamp, backgroundColor)
        }
    }

    private inline fun composeWithNormalized(
        photos: List<Bitmap>,
        normalize: (Bitmap) -> Bitmap,
        eventName: String,
        addTimestamp: Boolean,
        backgroundColor: Int,
        compose: (List<Bitmap>) -> Bitmap
    ): Bitmap {
        val normalized = photos.map { normalize(it) }
        return try {
            compose(normalized)
        } finally {
            // Only recycle bitmaps that were newly created (not the same object as the input)
            normalized.zip(photos).forEach { (n, orig) -> if (n !== orig) n.recycle() }
        }
    }

    private fun overlayLogo(strip: Bitmap, logo: Bitmap) {
        val maxLogoW = (strip.width * 0.22f).toInt().coerceAtLeast(60)
        val scale = maxLogoW.toFloat() / logo.width
        val logoW = maxLogoW
        val logoH = (logo.height * scale).toInt()
        val margin = BORDER / 2
        val left = strip.width - logoW - margin
        val top = margin
        val canvas = Canvas(strip)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { alpha = 210 }
        canvas.drawBitmap(
            logo, null,
            RectF(left.toFloat(), top.toFloat(), (left + logoW).toFloat(), (top + logoH).toFloat()),
            paint
        )
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

    private fun composeColumn(photos: List<Bitmap>, eventName: String, addTimestamp: Boolean, bg: Int): Bitmap {
        val cellW = photos[0].width
        val cellH = photos[0].height
        val footerH = footerHeight(eventName, addTimestamp)
        val totalW = BORDER * 2 + cellW
        val totalH = BORDER * 2 + photos.size * cellH + (photos.size - 1) * GAP + footerH
        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(bg)
        photos.forEachIndexed { i, bmp ->
            canvas.drawBitmap(bmp, BORDER.toFloat(), (BORDER + i * (cellH + GAP)).toFloat(), null)
        }
        if (footerH > 0) drawFooter(canvas, totalW, totalH, footerH, eventName, addTimestamp, bg)
        return result
    }

    private fun composeRow(photos: List<Bitmap>, eventName: String, addTimestamp: Boolean, bg: Int): Bitmap {
        val cellH = photos[0].height
        val footerH = footerHeight(eventName, addTimestamp)
        val totalW = BORDER * 2 + photos.sumOf { it.width } + (photos.size - 1) * GAP
        val totalH = BORDER * 2 + cellH + footerH
        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(bg)
        var x = BORDER.toFloat()
        for (bmp in photos) {
            canvas.drawBitmap(bmp, x, BORDER.toFloat(), null)
            x += bmp.width + GAP
        }
        if (footerH > 0) drawFooter(canvas, totalW, totalH, footerH, eventName, addTimestamp, bg)
        return result
    }

    private fun composeGrid(
        photos: List<Bitmap>,
        cols: Int,
        rows: Int,
        eventName: String,
        addTimestamp: Boolean,
        bg: Int
    ): Bitmap {
        val cellW = photos[0].width
        val cellH = photos[0].height
        val footerH = footerHeight(eventName, addTimestamp)
        val totalW = BORDER * 2 + cols * cellW + (cols - 1) * GAP
        val totalH = BORDER * 2 + rows * cellH + (rows - 1) * GAP + footerH
        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(bg)
        photos.forEachIndexed { i, bmp ->
            val col = i % cols
            val row = i / cols
            val x = (BORDER + col * (cellW + GAP)).toFloat()
            val y = (BORDER + row * (cellH + GAP)).toFloat()
            canvas.drawBitmap(bmp, x, y, null)
        }
        if (footerH > 0) drawFooter(canvas, totalW, totalH, footerH, eventName, addTimestamp, bg)
        return result
    }

    private fun composeHero(
        photos: List<Bitmap>,
        sideCount: Int,
        eventName: String,
        addTimestamp: Boolean,
        bg: Int
    ): Bitmap {
        val hero = normalizeWidth(photos[0], 900)
        val heroW = hero.width
        val heroH = hero.height

        val sideTargetW = heroW / 2
        val sides = photos.drop(1).take(sideCount).map { bmp ->
            val h = (sideTargetW.toFloat() / bmp.width * bmp.height).toInt()
            Bitmap.createScaledBitmap(bmp, sideTargetW, h, true)
        }

        try {
            val footerH = footerHeight(eventName, addTimestamp)
            val sidesH = sides.sumOf { it.height } + (sides.size - 1) * GAP
            val contentH = maxOf(heroH, sidesH)
            val totalW = BORDER * 2 + heroW + GAP + sideTargetW
            val totalH = BORDER * 2 + contentH + footerH

            val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            canvas.drawColor(bg)

            canvas.drawBitmap(hero, BORDER.toFloat(), BORDER.toFloat(), null)

            var sideY = BORDER.toFloat()
            val sideX = (BORDER + heroW + GAP).toFloat()
            for (bmp in sides) {
                canvas.drawBitmap(bmp, sideX, sideY, null)
                sideY += bmp.height + GAP
            }

            if (footerH > 0) drawFooter(canvas, totalW, totalH, footerH, eventName, addTimestamp, bg)
            return result
        } finally {
            if (hero !== photos[0]) hero.recycle()
            val origSides = photos.drop(1).take(sideCount)
            sides.zip(origSides).forEach { (s, orig) -> if (s !== orig) s.recycle() }
        }
    }

    private fun withFooter(photo: Bitmap, eventName: String, addTimestamp: Boolean, bg: Int): Bitmap {
        val footerH = footerHeight(eventName, addTimestamp)
        val totalW = BORDER * 2 + photo.width
        val totalH = BORDER * 2 + photo.height + footerH
        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(bg)
        canvas.drawBitmap(photo, BORDER.toFloat(), BORDER.toFloat(), null)
        if (footerH > 0) drawFooter(canvas, totalW, totalH, footerH, eventName, addTimestamp, bg)
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
        addTimestamp: Boolean,
        bg: Int = Color.WHITE
    ) {
        // Use dark text on light backgrounds, light text on dark backgrounds
        val luminance = (0.299 * Color.red(bg) + 0.587 * Color.green(bg) + 0.114 * Color.blue(bg)) / 255.0
        val textColor = if (luminance > 0.4) Color.DKGRAY else Color.argb(200, 220, 220, 220)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
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
