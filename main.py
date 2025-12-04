from fastapi import FastAPI, UploadFile, File
import shutil
import os

import asr_baglantisi
import telaffuz_analiz_baglantisi

app = FastAPI()
os.makedirs("gecici_sesler", exist_ok=True)

@app.post("/analiz-et")
async def analiz_yap(file: UploadFile = File(...)):

    dosya_yolu=f"gecici_sesler/{file.filename}"
    with open(dosya_yolu,"wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    bulunan_metin=asr_baglantisi.sesi_metne_cevir(dosya_yolu)

    hesaplanan_puan=telaffuz_analiz_baglantisi.telaffuzu_puanla(dosya_yolu,bulunan_metin)

    return{
        "ses_dosyasi":file.filename,
        "algilanan_metin":bulunan_metin,
        "puan":hesaplanan_puan
    }
