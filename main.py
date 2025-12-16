# main.py
from fastapi import FastAPI, UploadFile, File, Form, Depends, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy import func
import shutil
import os
import random

# Kendi modüllerimiz
import asr_baglantisi
import telaffuz_analiz_baglantisi
import modeller
from veritabani import SessionLocal, engine

app = FastAPI()

# Geçici ses klasörü
os.makedirs("gecici_sesler", exist_ok=True)

# Tabloları oluştur
modeller.Base.metadata.create_all(bind=engine)


# Veri tabanı bağlantısı
def veritabanini_getir():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


# --- YENİ: AKILLI KART SİSTEMİ (LEVEL SİSTEMİ) ---
@app.get("/seviyeli-kart-getir")
def seviyeli_kart_getir(
        kullanici_id: str,
        kategori: str,
        db: Session = Depends(veritabanini_getir)
):
    """
    Kullanıcının geçmişine bakar. 
    Eğer Kolay'da 90 üstü yaptıysa Orta'yı açar.
    Orta'da 90 üstü yaptıysa Zor'u açar.
    """

    # Kullanıcının bu kategorideki en yüksek başarısını bulalım
    # (Karmaşık SQL yerine basit Python mantığıyla yapıyoruz)

    # Kullanıcının tüm skorlarını çek
    gecmis = db.query(modeller.Skor).join(modeller.Metin).filter(
        modeller.Skor.kullanici_id == kullanici_id,
        modeller.Metin.kategori == kategori
    ).all()

    max_puan_kolay = 0
    max_puan_orta = 0

    for skor in gecmis:
        # Skoru alınan kartı bulalım
        kart = db.query(modeller.Metin).filter(modeller.Metin.id == skor.metin_id).first()
        if kart.seviye == "Kolay":
            max_puan_kolay = max(max_puan_kolay, skor.alinan_puan)
        elif kart.seviye == "Orta":
            max_puan_orta = max(max_puan_orta, skor.alinan_puan)

    # SEVİYE BELİRLEME MANTIĞI
    hak_edilen_seviye = "Kolay"  # Varsayılan

    if max_puan_kolay >= 90:
        hak_edilen_seviye = "Orta"
        # Eğer Orta seviyede de başarılıysa Zora geç
        if max_puan_orta >= 90:
            hak_edilen_seviye = "Zor"

    # O seviyeye uygun kartları getir
    kartlar = db.query(modeller.Metin).filter(
        modeller.Metin.kategori == kategori,
        modeller.Metin.seviye == hak_edilen_seviye
    ).all()

    if not kartlar:
        return {"mesaj": "Bu seviyede kart bulunamadı.", "seviye": hak_edilen_seviye}

    secilen_kart = random.choice(kartlar)

    return {
        "durum": "basarili",
        "seviye_durumu": f"Mevcut başarınıza göre {hak_edilen_seviye} seviyesi açıldı.",
        "kart_bilgisi": secilen_kart
    }


# --- ANALİZ VE PUANLAMA (ID BAZLI) ---
@app.post("/analiz-et")
async def analiz_yap(
        metin_id: int = Form(...),  # ARTIK ID İSTİYORUZ
        kullanici_id: str = Form(...),  # KİM BU PUANI ALDI?
        file: UploadFile = File(...),  # SES DOSYASI
        db: Session = Depends(veritabanini_getir)
):
    # 1. Kartı Veritabanından Bul
    hedef_kart = db.query(modeller.Metin).filter(modeller.Metin.id == metin_id).first()
    if not hedef_kart:
        raise HTTPException(status_code=404, detail="Kart bulunamadı!")

    hedef_metin = hedef_kart.icerik

    # 2. Dosyayı Kaydet
    dosya_yolu = f"gecici_sesler/{file.filename}"
    with open(dosya_yolu, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    # 3. ASR (Sesi Yazıya Dök)
    bulunan_metin, logit_verisi = asr_baglantisi.sesi_metne_cevir(dosya_yolu)

    # 4. Puanlama (Veritabanındaki metne göre)
    hesaplanan_puan = telaffuz_analiz_baglantisi.telaffuzu_puanla(
        ses_yolu=dosya_yolu,
        asr_metni=bulunan_metin,
        logits=logit_verisi,
        referans_metin=hedef_metin
    )

    # 5. SKORU KAYDET (GAMIFICATION)
    yeni_skor = modeller.Skor(
        metin_id=metin_id,
        kullanici_id=kullanici_id,
        alinan_puan=hesaplanan_puan
    )
    db.add(yeni_skor)
    db.commit()

    return {
        "durum": "basarili",
        "kullanici": kullanici_id,
        "kart_id": metin_id,
        "hedef_metin": hedef_metin,
        "okunan_metin": bulunan_metin,
        "puan": hesaplanan_puan
    }


# --- YARDIMCI: ADMIN İÇİN METİN EKLEME ---
@app.post("/admin/metin-ekle")
def metin_ekle(icerik: str, seviye: str, kategori: str, db: Session = Depends(veritabanini_getir)):
    yeni = modeller.Metin(icerik=icerik, seviye=seviye, kategori=kategori)
    db.add(yeni)
    db.commit()
    db.refresh(yeni)
    return yeni