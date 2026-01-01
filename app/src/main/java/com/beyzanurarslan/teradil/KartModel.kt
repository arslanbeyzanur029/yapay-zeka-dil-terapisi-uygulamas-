package com.beyzanurarslan.teradil

import com.google.gson.annotations.SerializedName

data class KartModel(
    @SerializedName("id") val id: Int,
    @SerializedName("icerik") val icerik: String,
    @SerializedName("seviye") val seviye: String,
    @SerializedName("kategori") val kategori: String
)