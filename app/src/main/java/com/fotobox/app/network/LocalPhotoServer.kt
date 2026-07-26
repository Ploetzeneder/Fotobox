package com.fotobox.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import kotlin.concurrent.thread

/**
 * Winziger HTTP-Server (keine externe Library).
 * Startet auf Port 8888 und serviert Fotos aus dem internen Speicher.
 * Gäste im gleichen WLAN können Fotos direkt herunterladen.
 *
 * URL-Schema: http://<geräte-ip>:8888/photo/<dateiname>
 */
class LocalPhotoServer(
    private val context: Context,
    private val port: Int = 8888
) {
    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var running = false
    private val photosDir get() = File(context.filesDir, "photos")

    fun start() {
        if (running) return
        running = true
        thread(isDaemon = true, name = "fotobox-server") {
            try {
                serverSocket = ServerSocket(port).also { srv ->
                    while (running) {
                        try {
                            val client = srv.accept()
                            thread(isDaemon = true) { handleClient(client) }
                        } catch (_: Exception) { /* server closed or client error */ }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        running = false
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }

    fun getDeviceIp(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return fallbackIp()
            val caps = cm.getNetworkCapabilities(network) ?: return fallbackIp()
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return fallbackIp()
            return cm.getLinkProperties(network)
                ?.linkAddresses
                ?.map { it.address }
                ?.filterIsInstance<Inet4Address>()
                ?.firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress ?: fallbackIp()
        }
        return fallbackIp()
    }

    private fun fallbackIp(): String? =
        NetworkInterface.getNetworkInterfaces()?.asSequence()
            ?.flatMap { it.inetAddresses.asSequence() }
            ?.filterIsInstance<Inet4Address>()
            ?.firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress

    fun photoUrl(fileName: String): String? {
        val ip = getDeviceIp() ?: return null
        return "http://$ip:$port/photo/$fileName"
    }

    fun stripUrl(sessionId: Long): String? {
        val ip = getDeviceIp() ?: return null
        return "http://$ip:$port/session/$sessionId"
    }

    private fun handleClient(socket: java.net.Socket) {
        socket.use { s ->
            try {
                val line = s.getInputStream().bufferedReader().readLine() ?: return
                // "GET /photo/strip_123.jpg HTTP/1.1"
                val parts = line.split(" ")
                if (parts.size < 2) return
                val path = parts[1]

                val file: File? = when {
                    path.startsWith("/photo/") -> {
                        val name = path.removePrefix("/photo/").sanitize()
                        File(photosDir, name).takeIf { it.isChildOf(photosDir) }
                    }
                    path.startsWith("/session/") -> {
                        val sessionId = path.removePrefix("/session/").toLongOrNull() ?: return
                        findStripForSession(sessionId)
                    }
                    else -> null
                }

                if (file != null && file.exists()) {
                    val bytes = file.readBytes()
                    val header = buildString {
                        append("HTTP/1.1 200 OK\r\n")
                        append("Content-Type: image/jpeg\r\n")
                        append("Content-Length: ${bytes.size}\r\n")
                        append("Content-Disposition: attachment; filename=\"${file.name}\"\r\n")
                        append("Access-Control-Allow-Origin: *\r\n")
                        append("\r\n")
                    }
                    s.getOutputStream().apply {
                        write(header.toByteArray())
                        write(bytes)
                        flush()
                    }
                } else {
                    val body = "<h1>Nicht gefunden</h1>"
                    val response = "HTTP/1.1 404 Not Found\r\nContent-Length: ${body.length}\r\n\r\n$body"
                    s.getOutputStream().write(response.toByteArray())
                }
            } catch (_: Exception) {}
        }
    }

    private fun findStripForSession(sessionId: Long): File? {
        // Strip files are named "strip_<sessionId>_*.jpg"
        return photosDir.listFiles()
            ?.filter { it.name.startsWith("strip_${sessionId}_") }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun String.sanitize(): String = this.replace("..", "").replace("/", "")

    private fun File.isChildOf(parent: File): Boolean =
        this.canonicalPath.startsWith(parent.canonicalPath + File.separator)
}
