package com.fotobox.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class FotobiechenApi(
    private val baseUrl: String,
    private val token: String = ""
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class LoginResult(val token: String, val boxId: String, val customerName: String)

    data class CloudSettings(
        val eventName: String,
        val stripLayout: String,
        val defaultFilter: String,
        val countdownSeconds: Int,
        val logoUrl: String,
        val autoPrint: Boolean,
        val printCopies: Int,
    )

    data class UploadResult(val id: String, val url: String, val qrUrl: String?)

    suspend fun login(email: String, password: String): LoginResult? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject()
                .put("email", email)
                .put("password", password)
                .toString()
                .toRequestBody("application/json".toMediaType())
            val response = client.newCall(
                Request.Builder().url("$baseUrl/auth/login").post(body).build()
            ).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body!!.string())
                LoginResult(
                    token = json.getString("token"),
                    boxId = json.getString("box_id"),
                    customerName = json.getString("customer_name")
                )
            } else null
        } catch (_: Exception) { null }
    }

    suspend fun getSettings(boxId: String): CloudSettings? = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(
                Request.Builder()
                    .url("$baseUrl/box/$boxId/settings")
                    .addHeader("Authorization", "Bearer $token")
                    .get().build()
            ).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body!!.string())
                CloudSettings(
                    eventName = json.optString("event_name", ""),
                    stripLayout = json.optString("strip_layout", "STRIP_4"),
                    defaultFilter = json.optString("default_filter", "NONE"),
                    countdownSeconds = json.optInt("countdown_seconds", 5),
                    logoUrl = json.optString("logo_url", ""),
                    autoPrint = json.optBoolean("auto_print", false),
                    printCopies = json.optInt("print_copies", 1),
                )
            } else null
        } catch (_: Exception) { null }
    }

    suspend fun uploadPhoto(boxId: String, sessionId: Long, file: File): UploadResult? =
        withContext(Dispatchers.IO) {
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("session_id", sessionId.toString())
                    .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaType()))
                    .build()
                val response = client.newCall(
                    Request.Builder()
                        .url("$baseUrl/box/$boxId/photos/upload")
                        .addHeader("Authorization", "Bearer $token")
                        .post(body).build()
                ).execute()
                if (response.isSuccessful) {
                    val json = JSONObject(response.body!!.string())
                    UploadResult(json.getString("photo_id"), json.getString("url"), null)
                } else null
            } catch (_: Exception) { null }
        }

    suspend fun uploadStrip(boxId: String, sessionId: Long, file: File): UploadResult? =
        withContext(Dispatchers.IO) {
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("session_id", sessionId.toString())
                    .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaType()))
                    .build()
                val response = client.newCall(
                    Request.Builder()
                        .url("$baseUrl/box/$boxId/strips/upload")
                        .addHeader("Authorization", "Bearer $token")
                        .post(body).build()
                ).execute()
                if (response.isSuccessful) {
                    val json = JSONObject(response.body!!.string())
                    UploadResult(
                        id = json.getString("strip_id"),
                        url = json.getString("url"),
                        qrUrl = json.optString("qr_url").ifEmpty { null }
                    )
                } else null
            } catch (_: Exception) { null }
        }

    suspend fun uploadAudio(boxId: String, file: File, durationMs: Long): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("duration_ms", durationMs.toString())
                    .addFormDataPart("file", file.name, file.asRequestBody("audio/mp4".toMediaType()))
                    .build()
                client.newCall(
                    Request.Builder()
                        .url("$baseUrl/box/$boxId/audio/upload")
                        .addHeader("Authorization", "Bearer $token")
                        .post(body).build()
                ).execute().isSuccessful
            } catch (_: Exception) { false }
        }
}
