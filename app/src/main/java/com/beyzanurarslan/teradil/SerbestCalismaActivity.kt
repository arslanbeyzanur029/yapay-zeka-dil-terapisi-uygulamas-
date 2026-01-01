package com.beyzanurarslan.teradil

import org.json.JSONObject
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import okhttp3.RequestBody.Companion.asRequestBody
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*

class SerbestCalismaActivity : AppCompatActivity() {

    lateinit var tvCumle: TextView
    lateinit var tvPuan: TextView
    lateinit var btnYeniKart: Button
    lateinit var btnMikrofon: FloatingActionButton

    // --- ÖNEMLİ: Hangi kartı okuduğumuzu burada tutacağız ---
    private var mevcutMetinId: Int = -1

    // Ses Kayıt Ayarları
    private val SAMPLE_RATE = 16000
    private val RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO
    private val RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false

    // Dosya Yolları
    private var hamSesDosyasi: String = ""
    private var wavDosyasi: String = ""

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_serbest_calisma)

        tvCumle = findViewById(R.id.tvCumle)
        tvPuan = findViewById(R.id.tvPuan)
        btnYeniKart = findViewById(R.id.btnYeniKart)
        btnMikrofon = findViewById(R.id.btnMikrofon)

        hamSesDosyasi = "${externalCacheDir?.absolutePath}/temp_audio.pcm"
        wavDosyasi = "${externalCacheDir?.absolutePath}/kayit.wav"

        // Uygulama açılır açılmaz ilk kartı sunucudan çek!
        yeniKartGetir()

        // İzinleri Kontrol Et
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 111)
        }

        btnYeniKart.setOnClickListener {
            yeniKartGetir()
        }

        btnMikrofon.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startRecording()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    stopRecording()
                    true
                }
                else -> false
            }
        }
    }

    // --- YENİ FONKSİYON: SUNUCUDAN KART ÇEKME ---
    fun yeniKartGetir() {
        tvPuan.text = "⏳ Sunucudan çekiliyor..."
        btnYeniKart.isEnabled = false // Üst üste basılmasın

        RetrofitClient.instance.rastgeleKartGetir().enqueue(object : Callback<KartModel> {
            override fun onResponse(call: Call<KartModel>, response: Response<KartModel>) {
                if (response.isSuccessful && response.body() != null) {
                    val kart = response.body()!!

                    // Gelen cümleyi ekrana yaz
                    tvCumle.text = kart.icerik

                    // ID'yi hafızaya al (Puanlarken lazım olacak)
                    mevcutMetinId = kart.id

                    tvPuan.text = "Basılı tut ve oku (${kart.seviye})"
                } else {
                    tvPuan.text = "Kart bulunamadı!"
                    tvCumle.text = "???"
                }
                btnYeniKart.isEnabled = true
            }

            override fun onFailure(call: Call<KartModel>, t: Throwable) {
                tvPuan.text = "Bağlantı Hatası!"
                tvCumle.text = "İnternet Yok mu?"
                btnYeniKart.isEnabled = true
            }
        })
    }

    // ... (startRecording ve writeAudioDataToFile aynı, o yüzden kısaltıyorum, silme sakın!) ...
    private fun startRecording() {
        if (isRecording) return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING)
        recorder = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize)
        recorder?.startRecording()
        isRecording = true
        tvPuan.text = "🔴 Kaydediliyor..."
        recordingThread = Thread { writeAudioDataToFile() }
        recordingThread?.start()
    }

    private fun writeAudioDataToFile() {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING)
        val data = ByteArray(bufferSize)
        try {
            val os = FileOutputStream(hamSesDosyasi)
            while (isRecording) {
                val read = recorder?.read(data, 0, bufferSize) ?: 0
                if (read > 0) os.write(data, 0, read)
            }
            os.close()
        } catch (e: IOException) { e.printStackTrace() }
    }
    // ...

    private fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        recorder?.stop()
        recorder?.release()
        recorder = null
        recordingThread = null

        hamDosyayiWavaCevir(hamSesDosyasi, wavDosyasi)
        dosyayiSunucuyaGonder(wavDosyasi)
    }

    // --- GÜNCELLENMİŞ GÖNDERME FONKSİYONU ---
    private fun dosyayiSunucuyaGonder(dosyaYolu: String) {
        // Eğer kart çekilemediyse gönderme yapma
        if (mevcutMetinId == -1) {
            Toast.makeText(this, "Önce kart çekmelisiniz!", Toast.LENGTH_SHORT).show()
            return
        }

        tvPuan.text = "⏳ Puanlanıyor..."

        val file = File(dosyaYolu)
        val requestFile = file.asRequestBody("audio/wav".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        // ARTIK GERÇEK ID'Yİ GÖNDERİYORUZ!
        val metinIdPart = RequestBody.create("text/plain".toMediaTypeOrNull(), mevcutMetinId.toString())
        val kullaniciIdPart = RequestBody.create("text/plain".toMediaTypeOrNull(), "beyza_user")

        RetrofitClient.instance.analizEt(body, metinIdPart, kullaniciIdPart).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                tvPuan.text = "Sonuç Geldi!" // Arkada görünen yazı

                if (response.isSuccessful) {
                    try {
                        val hamVeri = response.body()?.string()
                        if (hamVeri != null) {
                            // JSON verisini parçala
                            val jsonObject = JSONObject(hamVeri)
                            val puan = jsonObject.getInt("puan")
                            val okunanMetin = jsonObject.getString("okunan_metin")

                            // HAVALI KUTUYU GÖSTER!
                            sonucKutusunuGoster(puan, okunanMetin)
                        }
                    } catch (e: Exception) {
                        tvPuan.text = "Hata: ${e.localizedMessage}"
                    }
                } else {
                    tvPuan.text = "Sunucu Hatası!"
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                tvPuan.text = "❌ Hata: ${t.localizedMessage}"
            }
        })
    }

    // --- WAV DÖNÜŞTÜRME KODLARI (AYNI) ---
    private fun hamDosyayiWavaCevir(inFilename: String, outFilename: String) {
        val fis = FileInputStream(inFilename)
        val fos = FileOutputStream(outFilename)
        val totalAudioLen = fis.channel.size()
        val totalDataLen = totalAudioLen + 36
        val longSampleRate = SAMPLE_RATE.toLong()
        val channels = 1
        val byteRate = (16 * SAMPLE_RATE * channels / 8).toLong()
        writeWavHeader(fos, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate)
        val data = ByteArray(1024)
        var count = fis.read(data)
        while (count != -1) { fos.write(data, 0, count); count = fis.read(data) }
        fis.close(); fos.close()
    }

    private fun writeWavHeader(out: FileOutputStream, totalAudioLen: Long, totalDataLen: Long, longSampleRate: Long, channels: Int, byteRate: Long) {
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte(); header[5] = (totalDataLen shr 8 and 0xff).toByte(); header[6] = (totalDataLen shr 16 and 0xff).toByte(); header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte(); header[25] = (longSampleRate shr 8 and 0xff).toByte(); header[26] = (longSampleRate shr 16 and 0xff).toByte(); header[27] = (longSampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte(); header[29] = (byteRate shr 8 and 0xff).toByte(); header[30] = (byteRate shr 16 and 0xff).toByte(); header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = 2; header[33] = 0
        header[34] = 16; header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte(); header[41] = (totalAudioLen shr 8 and 0xff).toByte(); header[42] = (totalAudioLen shr 16 and 0xff).toByte(); header[43] = (totalAudioLen shr 24 and 0xff).toByte()
        out.write(header, 0, 44)
    }

    private fun sonucKutusunuGoster(puan: Int, okunan: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_sonuc)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Tasarımdaki elemanları bul
        val tvDialogBaslik = dialog.findViewById<TextView>(R.id.tvDialogBaslik) // Artık başlığı değiştirebiliriz!
        val tvDialogPuan = dialog.findViewById<TextView>(R.id.tvDialogPuan)
        val tvDialogOkunan = dialog.findViewById<TextView>(R.id.tvDialogOkunan)
        val btnKapat = dialog.findViewById<Button>(R.id.btnDialogKapat)

        tvDialogPuan.text = puan.toString()
        tvDialogOkunan.text = okunan

        // --- 3 AŞAMALI PUANLAMA MANTIĞI ---

        if (puan < 60) {
            // DURUM 1: ZAYIF (Kırmızı)
            tvDialogBaslik.text = "Pes Etme!"
            tvDialogBaslik.setTextColor(Color.RED)

            tvDialogPuan.setTextColor(Color.RED)
            btnKapat.text = "DAHA FAZLA DENE"
            // Buton rengi kırmızı
            btnKapat.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.RED)

        } else if (puan in 60..75) {
            // DURUM 2: ORTA (Turuncu/Sarı)
            tvDialogBaslik.text = "İyi Gidiyorsun!"
            // Turuncu renk kodu: #FF9800
            tvDialogBaslik.setTextColor(Color.parseColor("#FF9800"))

            tvDialogPuan.setTextColor(Color.parseColor("#FF9800"))
            btnKapat.text = "HA GAYRET!"
            // Buton rengi turuncu
            btnKapat.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800"))

        } else {
            // DURUM 3: HARİKA (Yeşil)
            tvDialogBaslik.text = "Tebrikler!"
            tvDialogBaslik.setTextColor(Color.parseColor("#4CAF50")) // Yeşil

            tvDialogPuan.setTextColor(Color.parseColor("#4CAF50"))
            btnKapat.text = "BAŞARDIN!"
            // Buton rengi yeşil (Varsayılan mor kalsın istersen bu satırı sil)
            btnKapat.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))
        }

        btnKapat.setOnClickListener {
            dialog.dismiss()
            // Eğer otomatik geçiş istersen buradaki // işaretini kaldır:
            // yeniKartGetir()
        }
        dialog.show()
    }
}