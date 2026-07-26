package com.fotobox.app.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.TimeUnit

class DalleClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun generateBackground(eventName: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(eventName)
            val json = JSONObject()
                .put("model", "dall-e-3")
                .put("prompt", prompt)
                .put("n", 1)
                .put("size", "1792x1024")
                .put("quality", "standard")
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.openai.com/v1/images/generations")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val responseBody = response.body?.string() ?: return@withContext null
            val imageUrl = JSONObject(responseBody)
                .getJSONArray("data")
                .getJSONObject(0)
                .getString("url")
            val bytes = URL(imageUrl).readBytes()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) { null }
    }

    private fun buildPrompt(eventName: String): String {
        val eventPart = if (eventName.isNotEmpty()) "for the event '$eventName'" else ""
        return "Professional photobooth background $eventPart. " +
            "Luxurious dark atmosphere with deep purple and magenta bokeh lights, " +
            "elegant and festive, wide landscape format 16:9, " +
            "no text, no people, cinematic lighting quality, ultra realistic."
    }

    companion object {
        fun cacheFile(context: Context): File =
            File(context.filesDir, "ai_background.jpg")

        suspend fun saveBitmap(context: Context, bitmap: Bitmap): File =
            withContext(Dispatchers.IO) {
                val file = cacheFile(context)
                FileOutputStream(file).use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it)
                }
                file
            }
    }
}
