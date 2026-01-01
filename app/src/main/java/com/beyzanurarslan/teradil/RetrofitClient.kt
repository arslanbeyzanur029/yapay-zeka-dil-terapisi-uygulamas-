package com.beyzanurarslan.teradil

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Ngrok linkin burada kalsın (Aynısı olsun)
    private const val BASE_URL = "https://utterly-overtimid-bethany.ngrok-free.dev/"

    // --- SABIR AYARI ---
    // Uygulama artık cevabı 5 DAKİKA boyunca bekleyecek.
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(300, TimeUnit.SECONDS) // Bağlanırken 5 dk bekle
        .readTimeout(300, TimeUnit.SECONDS)    // Cevap okurken 5 dk bekle
        .writeTimeout(300, TimeUnit.SECONDS)   // Dosya gönderirken 5 dk bekle
        .build()

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Yukarıdaki sabırlı istemciyi kullan
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}