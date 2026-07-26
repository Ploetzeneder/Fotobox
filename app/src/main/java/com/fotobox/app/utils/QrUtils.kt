package com.fotobox.app.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrUtils {

    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        fgColor: Int = Color.BLACK,
        bgColor: Int = Color.WHITE
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val writer = QRCodeWriter()
        val matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)

        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            for (x in 0 until sizePx) {
                pixels[y * sizePx + x] = if (matrix[x, y]) fgColor else bgColor
            }
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        return bitmap
    }

    /**
     * QR-Code mit schönem Rahmen und Beschriftung für die Anzeige nach dem Foto.
     */
    fun generateStyledQrBitmap(content: String, label: String = "Foto herunterladen"): Bitmap {
        val qrSize = 400
        val padding = 40
        val labelHeight = 60
        val totalSize = qrSize + padding * 2 + labelHeight

        val qr = generateQrBitmap(content, qrSize, Color.WHITE, Color.TRANSPARENT)

        val result = Bitmap.createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Hintergrund (dunkel, halbtransparent)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 20, 20, 20)
        }
        canvas.drawRoundRect(
            RectF(0f, 0f, totalSize.toFloat(), totalSize.toFloat()),
            24f, 24f, bgPaint
        )

        // QR Code
        val qrPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(
            qr,
            null,
            RectF(
                padding.toFloat(),
                padding.toFloat(),
                (padding + qrSize).toFloat(),
                (padding + qrSize).toFloat()
            ),
            qrPaint
        )
        qr.recycle()

        // Label
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            label,
            totalSize / 2f,
            (padding + qrSize + labelHeight / 2 + 10).toFloat(),
            textPaint
        )

        return result
    }
}
