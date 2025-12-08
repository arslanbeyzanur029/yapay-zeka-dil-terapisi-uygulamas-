# download_model.py
# Bu dosya, ASR modelini internetten lokal klasöre hatasız indirir.

from huggingface_hub import snapshot_download

# Proje için kullanacağımız model (HARF HATASI DÜZELTİLDİ: 'EMRE' büyük olmalı)
MODEL_ID = "EMRE/wav2vec2-large-xlsr-53-turkish"
LOCAL_PATH = "./turkish_asr_model"

print("--- MODEL İNDİRME BAŞLATILIYOR ---")
print(f"Hedeflenen model: {MODEL_ID}")
print(f"İndirilen klasör: {LOCAL_PATH}")
print("Bu işlem, modelin boyutu (yaklaşık 1.2 GB) nedeniyle zaman alacaktır.\n")

try:
    # Modelin tüm dosyalarını belirtilen yerel klasöre indirir.
    snapshot_download(
        repo_id=MODEL_ID,
        local_dir=LOCAL_PATH,
        allow_patterns=["*.json", "*.bin", "*.txt", "*.vocab", "*.model"] # Gerekli tüm dosyaları indiriyoruz
    )
    print("\nMODEL BAŞARIYLA İNDİRİLDİ. Artık main.py çalışabilir.")
except Exception as e:
    print(f"\nFATAL HATA: İndirme başarısız oldu. Kontrol et: {e}")