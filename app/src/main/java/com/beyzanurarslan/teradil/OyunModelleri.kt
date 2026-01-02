package com.beyzanurarslan.teradil

import com.google.gson.annotations.SerializedName

// 1. OYUNU BAŞLATMA
data class OyunBaslatRequest(
    val level: Int,
    val zorluk: String
)

data class OyunBaslatResponse(
    val level: Int,
    val zorluk: String,
    val sorular: List<SoruModel>
)

// Soruların Yapısı
data class SoruModel(
    val id: Int,
    val cumle: String,
    val puan: Int = 10 // Varsayılan puan 10 olsun
)

// 2. SKOR KAYDETME VE BÖLÜM BİTİRME
data class SkorRequest(
    val level: Int,
    val zorluk: String,

    // Backend "dogruluk_orani" bekliyorsa bunu kullanır,
    // ama biz kod içinde "dogruluk" diyerek rahatça kullanırız.
    @SerializedName("dogruluk_orani")
    val dogruluk: Float
)

data class SkorResponse(
    val puan: Int,
    val mesaj: String,

    @SerializedName("kilit_acildi")
    val kilitAcildi: Boolean, // Kodda kilitAcildi diyeceğiz

    @SerializedName("sonraki_asama")
    val sonrakiAsama: String? // Null gelebilir
)