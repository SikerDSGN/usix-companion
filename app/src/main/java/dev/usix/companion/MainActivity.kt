package dev.usix.companion

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Česká obrazovka stavu, oprávnění a párovacího tokenu. */
class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var tokenView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BridgeAuth.init(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        val title = TextView(this).apply {
            text = "USIX Companion CZ"
            textSize = 22f
        }
        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, 32, 0, 32)
        }
        val settingsBtn = Button(this).apply {
            text = "Nastavit přístup k oznámením"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        val accessBtn = Button(this).apply {
            text = "Nastavit ovládání obrazovky (Accessibility)"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        val mailHelp = TextView(this).apply {
            text = "Accessibility dovolí USIXu číst prvky na obrazovce, klepat, psát a posouvat obsah. Telefon musí být odemčený. Přístup k oznámením dovolí číst a obsluhovat podporovaná oznámení."
            textSize = 13f
            setPadding(0, 16, 0, 16)
        }
        val tokenLabel = TextView(this).apply {
            text = "Párovací token bridge. Zkopíruj ho a potom v Termuxu spusť: usix-termux pair"
            textSize = 13f
            setPadding(0, 40, 0, 8)
        }
        tokenView = TextView(this).apply {
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        val copyBtn = Button(this).apply {
            text = "Kopírovat párovací token"
            setOnClickListener { copyToken() }
        }
        val regenBtn = Button(this).apply {
            text = "Vygenerovat nový token (vyžaduje nové párování)"
            setOnClickListener {
                BridgeAuth.regenerate(this@MainActivity)
                refresh()
                Toast.makeText(this@MainActivity, "Nový token vytvořen. Zkopíruj ho a znovu proveď párování v Termuxu.", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(title)
        root.addView(status)
        root.addView(settingsBtn)
        root.addView(accessBtn)
        root.addView(mailHelp)
        root.addView(tokenLabel)
        root.addView(tokenView)
        root.addView(copyBtn)
        root.addView(regenBtn)
        setContentView(ScrollView(this).apply { addView(root) })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        BridgeForegroundService.start(this)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val enabled = isListenerEnabled()
        status.text = buildString {
            append(if (enabled) "✅ Přístup k oznámením povolen\n" else "❌ Přístup k oznámením vypnutý. Zapni ho tlačítkem níže.\n")
            append("Bridge: 127.0.0.1:${BridgeServer.PORT} (vyžaduje Bearer token)\n")
            append(if (NotifStore.listenerConnected) "✅ Listener oznámení připojen\n" else "• Listener čeká na připojení\n")
            append(if (UiController.connected()) "✅ Ovládání obrazovky připojeno" else "• Accessibility je vypnuté. Pro ovládání obrazovky ho zapni níže.")
        }
        tokenView.text = BridgeAuth.current() ?: "(token není k dispozici)"
    }

    private fun copyToken() {
        val token = BridgeAuth.current() ?: return
        getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("USIX Companion token", token))
        Toast.makeText(this, "Token zkopírován. Teď v Termuxu spusť: usix-termux pair", Toast.LENGTH_SHORT).show()
    }

    private fun isListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(":").any { ComponentName.unflattenFromString(it)?.packageName == packageName }
    }
}
