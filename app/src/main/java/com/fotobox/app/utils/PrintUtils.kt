package com.fotobox.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.activity.ComponentActivity
import java.io.FileOutputStream

/**
 * Druckt den Fotostreifen über den Android PrintManager.
 * Der Canon Selphy 1500 erscheint automatisch wenn er im gleichen WLAN ist (Mopria/AirPrint).
 * Zum Testen: "Als PDF speichern" im Android-Druckdialog wählen.
 */
fun printStrip(activity: ComponentActivity, imagePath: String, copies: Int = 1) {
    val printManager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = "Fotobox Streifen"

    val attrs = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.JIS_B7)   // 88x125mm — Selphy Standardformat
        .setResolution(PrintAttributes.Resolution("default", "300dpi", 300, 300))
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build()

    printManager.print(jobName, PhotoPrintAdapter(activity, imagePath, copies.coerceAtLeast(1)), attrs)
}

private class PhotoPrintAdapter(
    private val context: Context,
    private val imagePath: String,
    private val copies: Int
) : PrintDocumentAdapter() {

    private var pageWidthPts = 0
    private var pageHeightPts = 0

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }

        // Druckattribute in Punkte (72pt = 1 inch) umrechnen
        pageWidthPts = newAttributes.mediaSize?.widthMils?.milsToPts() ?: 252   // 88mm
        pageHeightPts = newAttributes.mediaSize?.heightMils?.milsToPts() ?: 360  // 125mm

        val info = PrintDocumentInfo.Builder("fotostreifen.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
            .setPageCount(copies)
            .build()

        callback.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        val bitmap = BitmapUtils.loadBitmapSampled(imagePath, 2400, 3600)
        if (bitmap == null) {
            callback.onWriteFailed("Bild nicht gefunden")
            return
        }

        val document = PdfDocument()
        try {
            repeat(copies) { i ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPts, pageHeightPts, i + 1).create()
                val page = document.startPage(pageInfo)
                drawBitmapCentered(page.canvas, bitmap, pageWidthPts, pageHeightPts)
                document.finishPage(page)
            }
            document.writeTo(FileOutputStream(destination.fileDescriptor))
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message)
        } finally {
            document.close()
            bitmap.recycle()
        }
    }

    private fun drawBitmapCentered(canvas: Canvas, bitmap: Bitmap, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val scaleX = w.toFloat() / bitmap.width
        val scaleY = h.toFloat() / bitmap.height
        val scale = minOf(scaleX, scaleY)
        val scaledW = bitmap.width * scale
        val scaledH = bitmap.height * scale
        val left = (w - scaledW) / 2f
        val top = (h - scaledH) / 2f
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + scaledW, top + scaledH), paint)
    }

    // 1 mil = 1/1000 inch, 1 inch = 72 pt
    private fun Int.milsToPts(): Int = (this * 72 / 1000f).toInt()
}
