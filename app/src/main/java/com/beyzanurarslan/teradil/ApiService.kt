package com.beyzanurarslan.teradil

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // --- 1. SERBEST ÇALIŞMA MODU (MEVCUT KODLARIN) ---

    // Rastgele kelime/kart getir
    @GET("rastgele-metin")
    fun rastgeleKartGetir(): Call<KartModel> // KartModel sınıfın varsa hata vermez

    // Sesi gönder ve analiz et (Multipart)
    @Multipart
    @POST("analiz-et")
    fun analizEt(
        @Part file: MultipartBody.Part,
        @Part("metin_id") metinId: RequestBody,
        @Part("kullanici_id") kullaniciId: RequestBody
    ): Call<ResponseBody> // veya kendi Response modelin


    // --- 2. OYUN MODU (YENİ EKLEDİKLERİMİZ) ---

    // Bölümü Başlat (Level ve Zorluk gönder, Soruları al)
    // NOT: Python tarafındaki adresin "/oyun/baslat" ise burası da öyle olmalı.
    @POST("/oyun/baslat")
    fun bolumBaslat(@Body request: OyunBaslatRequest): Call<OyunBaslatResponse>

    // Bölümü Bitir (Skoru gönder, Sonucu al)
    @POST("/oyun/bitir")
    fun bolumBitir(@Body request: SkorRequest): Call<SkorResponse>

    // MEVCUT KODLARININ ALTINA EKLE:

    @Multipart
    @POST("/oyun/ses-analiz")
    fun sesAnalizEt(
        @Part("hedef_cumle") hedefCumle: RequestBody,
        @Part sesDosyasi: MultipartBody.Part
    ): Call<SesAnalizResponse>
}
data class SesAnalizResponse(
    val puan: Int,
    val okunan: String,
    val mesaj: String
)