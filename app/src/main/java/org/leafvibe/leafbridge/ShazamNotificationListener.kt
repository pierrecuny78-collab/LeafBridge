package org.leafvibe.leafbridge

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.File
import java.util.concurrent.Executors

class ShazamNotificationListener : NotificationListenerService() {
    private val executor = Executors.newSingleThreadExecutor()
    private val shazamPackage = "com.shazam.android"

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != shazamPackage) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
            )?.toString().orEmpty()

        val payload = JSONObject().apply {
            put("package", sbn.packageName)
            put("title", title)
            put("text", text)
            put("post_time", sbn.postTime)
            put("notification_id", sbn.id)
        }.toString()

        executor.execute { postToTermux(payload) }
    }

    private fun postToTermux(payload: String) {
        persistNotification(payload)
        try {
            val connection = (URL("http://127.0.0.1:8765/shazam").openConnection()
                as HttpURLConnection)
            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            connection.responseCode
            connection.disconnect()
        } catch (_: Exception) {
            // La copie locale est conservée même si Termux est momentanément indisponible.
        }
    }

    private fun persistNotification(payload: String) {
        runCatching {
            val folder = File(filesDir, "leafbridge").apply { mkdirs() }
            val log = File(folder, "shazam-notifications.jsonl")
            log.appendText(payload + "\n", Charsets.UTF_8)
            // Keep the local audit log bounded; retain the newest 2 MiB.
            if (log.length() > 2L * 1024L * 1024L) {
                val bytes = log.readBytes()
                val start = (bytes.size - 2 * 1024 * 1024).coerceAtLeast(0)
                val boundary = bytes.indexOf(10, start).let { if (it >= 0) it + 1 else start }
                log.writeBytes(bytes.copyOfRange(boundary, bytes.size))
            }
            LeafAutoBackupScheduler.requestSoon(this)
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
