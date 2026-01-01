package com.beyzanurarslan.teradil

// 1. SATIRDAKİ PACKAGE KODU ZATEN SENDE VAR, ONA DOKUNMA.
// BURADAN AŞAĞISINI YAPIŞTIR:

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Tasarımdaki (XML) butonları bulup isim takıyoruz
        val btnSerbest = findViewById<Button>(R.id.btnSerbestMod)
        val btnOyun = findViewById<Button>(R.id.btnOyunModu)

        // 1. SERBEST ÇALIŞMA BUTONUNA TIKLANINCA
        btnSerbest.setOnClickListener {
            Toast.makeText(this, "Serbest Mod Açılıyor... 📖", Toast.LENGTH_SHORT).show()

            // Diğer sayfaya geçiş yapıyoruz
            val intent = Intent(this, SerbestCalismaActivity::class.java)
            startActivity(intent)
        }

        // 2. OYUN MODU BUTONUNA TIKLANINCA
        btnOyun.setOnClickListener {
            Toast.makeText(this, "Oyun Modu Yakında! 🎮", Toast.LENGTH_SHORT).show()
        }
    }
}