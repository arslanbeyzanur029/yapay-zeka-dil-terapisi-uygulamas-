from fastapi import FastAPI, UploadFile, File, Form
import shutil
import os


import asr_baglantisi
import telaffuz_analiz_baglantisi

app = FastAPI()


os.makedirs("gecici_sesler", exist_ok=True)

@app.post("/analiz-et")
async def analiz_yap(
    hedef_metin: str = Form(...),
    file: UploadFile = File(...)
):


    dosya_yolu = f"gecici_sesler/{file.filename}"
    with open(dosya_yolu, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    bulunan_metin, logit_verisi = asr_baglantisi.sesi_metne_cevir(dosya_yolu)

    hesaplanan_puan = telaffuz_analiz_baglantisi.telaffuzu_puanla(
        ses_yolu=dosya_yolu,
        asr_metni=bulunan_metin,
        logits=logit_verisi,
        referans_metin=hedef_metin
    )


    return {
        "durum": "basarili",
        "dosya_adi": file.filename,
        "beklenen_metin": hedef_metin,
        "algilanan_metin": bulunan_metin,
        "telaffuz_skoru": hesaplanan_puan
    }