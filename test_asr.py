import torch
import librosa
import numpy as np
import os
from transformers import Wav2Vec2ForCTC, Wav2Vec2Processor

# AYARLAR
MODEL_PATH = "./turkish_asr_model"
# Test etmek istediğin o net ses dosyasının tam adını buraya yaz:
SES_DOSYASI = "Kayit_ornegi2.wav"

print(f"--- 1. MODEL YÜKLENİYOR ({MODEL_PATH}) ---")
try:
    processor = Wav2Vec2Processor.from_pretrained(MODEL_PATH, local_files_only=True)
    model = Wav2Vec2ForCTC.from_pretrained(MODEL_PATH, local_files_only=True)
    print("✅ Model ve Processor başarıyla yüklendi.")
except Exception as e:
    print(f"❌ Model Yükleme Hatası: {e}")
    exit()

print(f"\n--- 2. SES DOSYASI OKUNUYOR ({SES_DOSYASI}) ---")
try:
    # Sesi ham haliyle oku
    speech_array, sr = librosa.load(SES_DOSYASI, sr=16000)
    print(f"✅ Ses Okundu. Örnekleme Hızı: {sr} Hz")
    print(f"📊 Ses Verisi İstatistikleri:")
    print(f"   - Uzunluk: {len(speech_array)} örnek")
    print(f"   - Min Değer: {np.min(speech_array)}")
    print(f"   - Max Değer: {np.max(speech_array)}")
    print(f"   - Ortalama: {np.mean(speech_array)}")

    if np.max(np.abs(speech_array)) == 0:
        print("⚠️ UYARI: Ses dosyası tamamen BOŞ (Sessiz)!")
except Exception as e:
    print(f"❌ Ses Okuma Hatası: {e}")
    exit()

print(f"\n--- 3. MODEL TAHMİNİ ---")
try:
    # Giriş verisini hazırla
    input_values = processor(speech_array, return_tensors="pt", sampling_rate=16000).input_values

    # Logitleri (Puanları) al
    with torch.no_grad():
        logits = model(input_values).logits

    # Tahmin edilen ID'leri al
    predicted_ids = torch.argmax(logits, dim=-1)

    print(f"📊 Model Çıktı Analizi:")
    print(f"   - Logit Boyutu: {logits.shape}")
    print(f"   - Tahmin Edilen ID'ler (İlk 20): {predicted_ids[0][:20].tolist()}")

    # Metne çevir
    transcription = processor.batch_decode(predicted_ids)[0]
    print(f"\n📢 HAM ÇIKTI: '{transcription}'")

except Exception as e:
    print(f"❌ Tahmin Hatası: {e}")