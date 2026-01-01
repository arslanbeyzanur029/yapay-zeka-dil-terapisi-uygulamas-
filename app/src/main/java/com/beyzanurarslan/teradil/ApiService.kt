package com.beyzanurarslan.teradil

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    // 1. GÖREV: Rastgele Kart Getir (Yeni Eklediğimiz)
    @GET("rastgele-metin")
    fun rastgeleKartGetir(): Call<KartModel>

    // 2. GÖREV: Sesi ve Kart ID'sini Gönder (Analiz Et)
    @Multipart
    @POST("analiz-et")
    fun analizEt(
        @Part file: MultipartBody.Part,
        @Part("metin_id") metinId: RequestBody,      // Hangi kartı okuduk?
        @Part("kullanici_id") kullaniciId: RequestBody // Kim okudu?
    ): Call<ResponseBody>
}