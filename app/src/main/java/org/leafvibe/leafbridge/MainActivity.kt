package org.leafvibe.leafbridge

import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var status: TextView
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 36)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(this).apply {
            text = "LeafBridge"
            textSize = 30f
        }
        status = TextView(this).apply {
            textSize = 17f
            setPadding(0, 28, 0, 28)
        }

        val permissionButton = Button(this).apply {
            text = "Autoriser les notifications Shazam"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        val testButton = Button(this).apply {
            text = "Tester la connexion à Termux"
            setOnClickListener { sendTest() }
        }

        val info = TextView(this).apply {
            text = "LeafBridge transmet uniquement les notifications Shazam reçues par Android. " +
                "Termux doit être démarré et son récepteur local actif sur le port 8765."
            textSize = 14f
            setPadding(0, 24, 0, 0)
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(permissionButton)
        layout.addView(testButton)
        layout.addView(info)
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val manager = getSystemService(NotificationManager::class.java)
        val component = ComponentName(this, ShazamNotificationListener::class.java)
        val enabled = manager.isNotificationListenerAccessGranted(component)
        status.text = if (enabled) {
            "Accès aux notifications : ACTIVÉ"
        } else {
            "Accès aux notifications : NON AUTORISÉ"
        }
    }

    private fun sendTest() {
        executor.execute {
            try {
                val connection = (URL("http://127.0.0.1:8765/shazam").openConnection()
                    as HttpURLConnection)
                connection.requestMethod = "POST"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                val body = """{"source":"LeafBridge","test":true,"title":"Test LeafBridge","text":"Connexion Android vers Termux"}"""
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                connection.disconnect()
                runOnUiThread {
                    Toast.makeText(
                        this,
                        if (code in 200..299) "Termux a répondu : $code" else "Réponse Termux : $code",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Connexion impossible : ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
