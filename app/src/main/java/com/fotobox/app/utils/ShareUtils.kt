package com.fotobox.app.utils

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager

object ShareUtils {

    fun sharePhoto(context: Context, filePath: String) {
        val uri = BitmapUtils.getUri(context, filePath)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Foto teilen"))
    }

    fun shareMultiplePhotos(context: Context, filePaths: List<String>) {
        val uris = filePaths.map { BitmapUtils.getUri(context, it) }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/jpeg"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Fotos teilen"))
    }
}
