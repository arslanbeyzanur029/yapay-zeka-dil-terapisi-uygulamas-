package com.beyzanurarslan.teradil

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class OyunActivity : AppCompatActivity() {

    // Arayüz
    private lateinit var txtLevelBilgi: TextView
    private lateinit var txtPuan: TextView
    private lateinit var txtSoruCumlesi: TextView
    private lateinit var btnMikrofon: ImageButton
    private lateinit var layoutSonuc: LinearLayout
    private lateinit var txtSonucMesaji: TextView
    private lateinit var btnSiradaki: Button
    private lateinit var txtDurum: TextView

    // Veriler
    private var soruListesi: List<SoruModel> = ArrayList()
    private var suankiSoruIndex = 0
    private var toplamPuan = 0
    private var currentLevel = 1
    private var currentZorluk = "kolay"

    // --- SES KAYIT AYARLARI (WAV İÇİN) ---
    private val SAMPLE_RATE = 16000
    private val RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO
    private val RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false

    private var hamSesDosyasi: String = ""
    private var wavDosyasi: String = ""

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_oyun)

        if (!izinKontrol()) izinIste()

        // Dosya yollarını hazırla
        hamSesDosyasi = "${externalCacheDir?.absolutePath}/oyun_temp.pcm"
        wavDosyasi = "${externalCacheDir?.absolutePath}/oyun_kayit.wav"

        // Intent verilerini al
        currentLevel = intent.getIntExtra("SECILEN_LEVEL", 1)
        currentZorluk = "kolay"

        // Tanımlamalar
        txtLevelBilgi = findViewById(R.id.txtLevelBilgi)
        txtPuan = findViewById(R.id.txtPuan)
        txtSoruCumlesi = findViewById(R.id.txtSoruCumlesi)
        btnMikrofon = findViewById(R.id.btnMikrofon)
        layoutSonuc = findViewById(R.id.layoutSonuc)
        txtSonucMesaji = findViewById(R.id.txtSonucMesaji)
        btnSiradaki = findViewById(R.id.btnSiradaki)
        txtDurum = findViewById(R.id.txtDurum)

        oyunuBaslat(currentLevel, currentZorluk)

        // --- BAS - KONUŞ (SERBEST MOD MANTIĞI) ---
        btnMikrofon.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (izinKontrol()) {
                        startRecording()
                        btnMikrofon.alpha = 0.6f
                        txtDurum.text = "🔴 Kaydediliyor..."
                        txtDurum.setTextColor(android.graphics.Color.RED)
                    } else {
                        izinIste()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    stopRecording()
                    btnMikrofon.alpha = 1.0f
                    txtDurum.text = "Analiz Ediliyor... ⏳"
                    txtDurum.setTextColor(android.graphics.Color.parseColor("#7B1FA2"))
                    true
                }
                else -> false
            }
        }

        btnSiradaki.setOnClickListener { sonrakiSoruyaGec() }
    }

    // --- WAV KAYIT FONKSİYONLARI ---
    private fun startRecording() {
        if (isRecording) return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING)
        recorder = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize)

        recorder?.startRecording()
        isRecording = true

        recordingThread = Thread { writeAudioDataToFile(bufferSize) }
        recordingThread?.start()
    }

    private fun writeAudioDataToFile(bufferSize: Int) {
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

    private fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        recorder?.stop()
        recorder?.release()
        recorder = null
        recordingThread = null

        // PCM -> WAV Çevir ve Gönder
        hamDosyayiWavaCevir(hamSesDosyasi, wavDosyasi)
        sesiAnalizeGonder()
    }

    // --- PCM to WAV ÇEVİRİCİ ---
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
        while (count != -1) {
            fos.write(data, 0, count)
            count = fis.read(data)
        }
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

    // --- BACKEND İLETİŞİMİ ---
    private fun sesiAnalizeGonder() {
        val file = File(wavDosyasi) // Artık WAV gönderiyoruz
        if (!file.exists()) return
        if (soruListesi.isEmpty()) return

        val requestFile = file.asRequestBody("audio/wav".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val simdikiCumle = soruListesi[suankiSoruIndex].cumle.replace("Level $currentLevel - ", "")
        val hedefCumleBody = simdikiCumle.toRequestBody("text/plain".toMediaTypeOrNull())

        RetrofitClient.instance.sesAnalizEt(hedefCumleBody, body).enqueue(object : Callback<SesAnalizResponse> {
            override fun onResponse(call: Call<SesAnalizResponse>, response: Response<SesAnalizResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val sonuc = response.body()!!
                    toplamPuan += sonuc.puan
                    txtPuan.text = "Puan: $toplamPuan"

                    layoutSonuc.visibility = View.VISIBLE
                    txtSonucMesaji.text = "Algılanan: ${sonuc.okunan}\nPuan: ${sonuc.puan}"

                    txtDurum.text = "Tamamlandı ✅"
                    btnMikrofon.isEnabled = false
                    btnMikrofon.alpha = 0.5f
                } else {
                    txtDurum.text = "Sunucu Hatası!"
                }
            }
            override fun onFailure(call: Call<SesAnalizResponse>, t: Throwable) {
                txtDurum.text = "Hata: ${t.localizedMessage}"
            }
        })
    }

    // --- DİĞER OYUN FONKSİYONLARI ---
    private fun oyunuBaslat(level: Int, zorluk: String) {
        txtLevelBilgi.text = "Level $level - ${zorluk.uppercase()}"
        txtSoruCumlesi.text = "Yükleniyor..."

        val istek = OyunBaslatRequest(level, zorluk)
        RetrofitClient.instance.bolumBaslat(istek).enqueue(object : Callback<OyunBaslatResponse> {
            override fun onResponse(call: Call<OyunBaslatResponse>, response: Response<OyunBaslatResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val veri = response.body()!!
                    soruListesi = veri.sorular
                    suankiSoruIndex = 0
                    soruyuEkranaYaz()
                } else {
                    txtSoruCumlesi.text = "Hata: Sorular alınamadı."
                }
            }
            override fun onFailure(call: Call<OyunBaslatResponse>, t: Throwable) {
                txtSoruCumlesi.text = "Bağlantı Hatası!"
            }
        })
    }

    private fun soruyuEkranaYaz() {
        if (suankiSoruIndex < soruListesi.size) {
            val soru = soruListesi[suankiSoruIndex]
            txtSoruCumlesi.text = soru.cumle
            layoutSonuc.visibility = View.INVISIBLE
            btnMikrofon.isEnabled = true
            btnMikrofon.alpha = 1.0f
            txtDurum.text = "Bas ve Konuş"
            txtDurum.setTextColor(android.graphics.Color.parseColor("#7B1FA2"))
        } else {
            bolumSonuIslemleri()
        }
    }

    private fun sonrakiSoruyaGec() {
        suankiSoruIndex++
        soruyuEkranaYaz()
    }

    private fun bolumSonuIslemleri() {
        txtSoruCumlesi.text = "Bölüm Sonu..."
        layoutSonuc.visibility = View.INVISIBLE
        txtDurum.text = ""

        val istek = SkorRequest(currentLevel, currentZorluk, 1.0f)
        RetrofitClient.instance.bolumBitir(istek).enqueue(object : Callback<SkorResponse> {
            override fun onResponse(call: Call<SkorResponse>, response: Response<SkorResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val sonuc = response.body()!!
                    val sonraki = sonuc.sonrakiAsama ?: ""

                    if (sonraki == "orta") {
                        currentZorluk = "orta"
                        oyunuBaslat(currentLevel, "orta")
                        Toast.makeText(applicationContext, "ORTA Seviyeye Geçiliyor...", Toast.LENGTH_SHORT).show()
                    } else if (sonraki == "zor") {
                        currentZorluk = "zor"
                        oyunuBaslat(currentLevel, "zor")
                        Toast.makeText(applicationContext, "ZOR Seviyeye Geçiliyor...", Toast.LENGTH_SHORT).show()
                    } else if (sonraki.startsWith("Level")) {
                        kilitAc(currentLevel + 1)
                        Toast.makeText(applicationContext, "Tebrikler! $sonraki Açıldı! 🔓", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        finish()
                    }
                }
            }
            override fun onFailure(call: Call<SkorResponse>, t: Throwable) { }
        })
    }

    private fun kilitAc(yeniLevel: Int) {
        val sharedPref = getSharedPreferences("OyunDurumu", Context.MODE_PRIVATE)
        val mevcutMax = sharedPref.getInt("MAX_LEVEL", 1)
        if (yeniLevel > mevcutMax && yeniLevel <= 10) {
            val editor = sharedPref.edit()
            editor.putInt("MAX_LEVEL", yeniLevel)
            editor.apply()
        }
    }

    private fun izinKontrol(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    private fun izinIste() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 123)
    }
}