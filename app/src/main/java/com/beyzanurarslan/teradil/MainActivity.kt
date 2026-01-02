package com.beyzanurarslan.teradil

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // XML'deki bileşenleri buluyoruz.
        // NOT: <View> kullandım ki XML tarafında Button, CardView veya ImageButton kullanman fark etmesin, hata vermez.
        val btnSerbest = findViewById<View>(R.id.btnSerbestMod)
        val btnOyun = findViewById<View>(R.id.btnOyunModu)

        // 1. SERBEST ÇALIŞMA BUTONUNA TIKLANINCA
        btnSerbest.setOnClickListener {
            // Serbest çalışma sayfasına geçiş
            val intent = Intent(this, SerbestCalismaActivity::class.java)
            startActivity(intent)
        }

        // 2. OYUN MODU BUTONUNA TIKLANINCA (GÜNCELLENDİ 🚀)
        btnOyun.setOnClickListener {
            // Artık "Çok Yakında" mesajı yok! Direkt Level Seçimine gidiyoruz.
            val intent = Intent(this, LevelSecimActivity::class.java)
            startActivity(intent)
        }
    }
}