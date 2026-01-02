package com.beyzanurarslan.teradil

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class LevelSecimActivity : AppCompatActivity() {

    // LevelSecimActivity.kt içindeki onCreate fonksiyonunu bununla değiştir:

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_secim)

        val gridLayout = findViewById<GridLayout>(R.id.gridLayoutLevels)

        // 1. Telefon Hafızasından En Son Açılan Level'ı Öğren
        // Eğer hiç oynamadıysa varsayılan olarak 1 döner.
        val sharedPref = getSharedPreferences("OyunDurumu", MODE_PRIVATE)
        val acikOlanMaxLevel = sharedPref.getInt("MAX_LEVEL", 1)

        // 10 Level Butonu Oluştur
        for (i in 1..10) {
            val btn = Button(this)

            // Boyut ve Margin Ayarları (Senin kodundaki gibi)
            val ekranGenisligi = resources.displayMetrics.widthPixels
            val butonBoyutu = (ekranGenisligi / 4)
            val params = GridLayout.LayoutParams()
            params.width = butonBoyutu
            params.height = butonBoyutu
            val margin = (resources.displayMetrics.density * 8).toInt()
            params.setMargins(margin, margin, margin, margin)
            btn.layoutParams = params

            // --- KİLİT MANTIĞI BURADA ---
            if (i <= acikOlanMaxLevel) {
                // Level AÇIK
                btn.text = "$i"
                btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#7B1FA2")) // Mor
                btn.isEnabled = true
                btn.setOnClickListener { zorlukSecimPenceresiAc(i) }
            } else {
                // Level KİLİTLİ
                btn.text = "🔒"
                btn.backgroundTintList = ColorStateList.valueOf(Color.LTGRAY) // Gri
                btn.isEnabled = false
            }

            gridLayout.addView(btn)
        }
    }

    private fun zorlukSecimPenceresiAc(level: Int) {
        // Dialog İçin Tasarım
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val padding = (resources.displayMetrics.density * 20).toInt()
        layout.setPadding(padding, padding, padding, padding)
        layout.gravity = Gravity.CENTER

        // 1. KOLAY BUTONU
        val btnKolay = butonOlustur("Kolay (1 Yıldız)", "#4CAF50")
        btnKolay.setOnClickListener { oyunuBaslat(level, "kolay") }
        layout.addView(btnKolay)

        // 2. ORTA BUTONU
        val btnOrta = butonOlustur("Orta (2 Yıldız)", "#FF9800")
        if (level == 1) { // Şimdilik sadece 1. Levelda açık olsun
            btnOrta.setOnClickListener { oyunuBaslat(level, "orta") }
        } else {
            kilitliYap(btnOrta)
        }
        layout.addView(btnOrta)

        // 3. ZOR BUTONU
        val btnZor = butonOlustur("Zor (3 Yıldız)", "#F44336")
        if (level == 1) {
            btnZor.setOnClickListener { oyunuBaslat(level, "zor") }
        } else {
            kilitliYap(btnZor)
        }
        layout.addView(btnZor)

        // Dialogu Göster
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Level $level - Zorluk Seç")
        builder.setView(layout)
        builder.setNegativeButton("İptal", null)
        builder.show()
    }

    private fun butonOlustur(metin: String, renkKodu: String): Button {
        val btn = Button(this)
        btn.text = metin
        btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor(renkKodu))
        btn.setTextColor(Color.WHITE)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val margin = (resources.displayMetrics.density * 5).toInt()
        params.setMargins(0, margin, 0, margin)
        btn.layoutParams = params
        return btn
    }

    private fun kilitliYap(btn: Button) {
        btn.text = "${btn.text} 🔒"
        btn.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
        btn.isEnabled = false
    }

    private fun oyunuBaslat(level: Int, zorluk: String) {
        val intent = Intent(this, OyunActivity::class.java)
        intent.putExtra("SECILEN_LEVEL", level)
        intent.putExtra("SECILEN_ZORLUK", zorluk)
        startActivity(intent)
    }
}