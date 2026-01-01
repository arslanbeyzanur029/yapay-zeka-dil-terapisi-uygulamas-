# main.py
from fastapi import FastAPI, UploadFile, File, Form, Depends, HTTPException
from sqlalchemy.orm import Session
from rapidfuzz import fuzz
from sqlalchemy import func
import shutil
import os
import random

# Kendi modüllerimiz
import asr_baglantisi
# import telaffuz_analiz_baglantisi # Artık kendi fonksiyonumuzu kullanacağız
import modeller
from veritabani import SessionLocal, engine

app = FastAPI()

# Geçici ses klasörü
os.makedirs("gecici_sesler", exist_ok=True)

# Tabloları oluştur
modeller.Base.metadata.create_all(bind=engine)


# --- YENİ ADALETLİ PUANLAMA FONKSİYONU ---
def adaletli_puan_hesapla(hedef_metin, gelen_ses):
    """
    Bu fonksiyon, kullanıcının sadece ses benzerliğinden puan almasını engeller.
    Eğer ortak kelime yoksa puanı tavana (20) kilitler.
    """
    # 1. Temizlik (Küçük harfe çevir)
    hedef = hedef_metin.lower().strip()
    gelen = gelen_ses.lower().strip()

    # Boş gelirse 0 ver
    if not gelen or not hedef:
        return 0

    # 2. Kelimeleri Ayır (Küme oluştur)
    # Noktalama işaretlerini temizlemek iyi olur ama basit split iş görür
    hedef_kelimeler = set(hedef.replace(".", "").replace(",", "").split())
    gelen_kelimeler = set(gelen.replace(".", "").replace(",", "").split())

    # 3. Kesişim Kontrolü (Hiç ortak kelime var mı?)
    # "&" işareti iki kümenin ortak elemanlarını bulur.
    ortak_kelime_var_mi = bool(hedef_kelimeler & gelen_kelimeler)

    # Normal Puanı Hesapla (Ses benzerliği - RapidFuzz)
    ham_puan = fuzz.ratio(hedef, gelen)

    if not ortak_kelime_var_mi:
        # SENARYO 1: HİÇ ORTAK KELİME YOK (Konu dışı konuşmuş)
        # Puanı hesapla ama Maksimum 20 ver.
        # Böylece ses benziyorsa 15-20 alır, benzemiyorsa 5-10 alır.
        return min(ham_puan, 20)

    else:
        # SENARYO 2: ORTAK KELİME VAR (Doğru yolda)
        # Hakkı neyse onu ver.
        return ham_puan


# Veri tabanı bağlantısı
def veritabanini_getir():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


# --- 1. SERBEST ÇALIŞMA MODU (RASTGELE KART) ---
@app.get("/rastgele-metin")
def rastgele_metin_getir(
        kategori: str = None,  # İsteğe bağlı: Sadece 'Günlük' veya 'Tekerleme' isteyebilir
        db: Session = Depends(veritabanini_getir)
):
    """
    Seviye kontrolü yapmadan, istenen kategoriden rastgele bir kart getirir.
    Kullanıcı sadece pratik yapmak istediğinde kullanılır.
    """
    sorgu = db.query(modeller.Metin)

    # Eğer kategori belirtildiyse filtrele (Örn: ?kategori=Günlük)
    if kategori:
        sorgu = sorgu.filter(modeller.Metin.kategori == kategori)

    metinler = sorgu.all()

    if not metinler:
        return {"hata": "Bu kategoride hiç kart bulunamadı!"}

    # Rastgele birini seçip döndür
    secilen = random.choice(metinler)
    return secilen


# --- 2. OYUN MODU (LEVEL SİSTEMİ) ---
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

    # Kullanıcının geçmiş skorlarını çek
    gecmis = db.query(modeller.Skor).join(modeller.Metin).filter(
        modeller.Skor.kullanici_id == kullanici_id,
        modeller.Metin.kategori == kategori
    ).all()

    max_puan_kolay = 0
    max_puan_orta = 0

    for skor in gecmis:
        # Skoru alınan kartı bulalım
        kart = db.query(modeller.Metin).filter(modeller.Metin.id == skor.metin_id).first()
        if kart and kart.seviye == "Kolay":
            max_puan_kolay = max(max_puan_kolay, skor.alinan_puan)
        elif kart and kart.seviye == "Orta":
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


# --- 3. ANALİZ VE PUANLAMA (ID BAZLI) ---
@app.post("/analiz-et")
async def analiz_yap(
        metin_id: int = Form(...),  # ID İSTİYORUZ
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

    # 4. YENİ PUANLAMA (Adaletli Fonksiyon)
    hesaplanan_puan = adaletli_puan_hesapla(hedef_metin, bulunan_metin)

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


# --- 4. YARDIMCI: ADMIN İÇİN METİN EKLEME ---
@app.post("/admin/metin-ekle")
def metin_ekle(icerik: str, seviye: str, kategori: str, db: Session = Depends(veritabanini_getir)):
    yeni = modeller.Metin(icerik=icerik, seviye=seviye, kategori=kategori)
    db.add(yeni)
    db.commit()
    db.refresh(yeni)
    return yeni