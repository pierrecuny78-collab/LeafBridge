package org.leafvibe.leafbridge

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ShazamNotificationListener : NotificationListenerService() {
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val shazamPackage = "com.shazam.android"
    private val queueLock = Any()
    private val preferences by lazy { getSharedPreferences("leafbridge_notifications", MODE_PRIVATE) }
    private val appPreferences by lazy { getSharedPreferences("leafbridge", MODE_PRIVATE) }
    private val queueKey = "pending_shazam"

    override fun onListenerConnected() {
        super.onListenerConnected()
        executor.execute { drainQueue() }
        executor.scheduleWithFixedDelay({ drainQueue() }, 20, 20, TimeUnit.SECONDS)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != shazamPackage) return
        // Silent background automation: never launch Shazam or open its notification.
        if (!appPreferences.getBoolean("auto_shazam_spotify", true)) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val payload = JSONObject().apply {
            put("package", sbn.packageName)
            put("title", title)
            put("text", text)
            put("post_time", sbn.postTime)
            put("notification_id", sbn.id)
            put("notification_key", sbn.key)
        }
        synchronized(queueLock) {
            val queue = readQueue()
            queue.put(payload)
            while (queue.length() > 500) queue.remove(0)
            preferences.edit().putString(queueKey, queue.toString()).apply()
        }
        executor.execute { drainQueue() }
    }

    private fun readQueue(): JSONArray {
        return try { JSONArray(preferences.getString(queueKey, "[]") ?: "[]") }
        catch (_: Exception) { JSONArray() }
    }

    private fun drainQueue() {
        if (!appPreferences.getBoolean("auto_shazam_spotify", true)) return
        while (true) {
            val payload = synchronized(queueLock) {
                val queue = readQueue()
                if (queue.length() == 0) return
                queue.optJSONObject(0)?.toString()
            } ?: return
            if (!postToTermux(payload)) return
            synchronized(queueLock) {
                val queue = readQueue()
                if (queue.length() > 0) {
                    queue.remove(0)
                    preferences.edit().putString(queueKey, queue.toString()).apply()
                }
            }
        }
    }

    private fun postToTermux(payload: String): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL("http://127.0.0.1:8765/shazam").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 15000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            connection.responseCode in 200..299
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}