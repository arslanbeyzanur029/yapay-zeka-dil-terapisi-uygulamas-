# veri_yukle.py
from sqlalchemy.orm import Session
from veritabani import SessionLocal, engine
import modeller

modeller.Base.metadata.create_all(bind=engine)


def yukle():
    db = SessionLocal()

    veriler = [
        # TEKERLEMELER (Seviyeli)
        {"icerik": "Oya oya koyun boya.", "seviye": "Kolay", "kategori": "Tekerleme"},
        {"icerik": "Ali topu at.", "seviye": "Kolay", "kategori": "Tekerleme"},
        {"icerik": "Şu köşe yaz köşesi.", "seviye": "Orta", "kategori": "Tekerleme"},
        {"icerik": "Kartal kalkar dal sarkar.", "seviye": "Orta", "kategori": "Tekerleme"},
        {"icerik": "Bir berber bir berbere gel beraber.", "seviye": "Zor", "kategori": "Tekerleme"},
        {"icerik": "Sarımsaklasak da mı saklasak?", "seviye": "Zor", "kategori": "Tekerleme"},

        # GÜNLÜK KONUŞMA
        {"icerik": "Merhaba nasılsın?", "seviye": "Kolay", "kategori": "Günlük"},
        {"icerik": "Bugün hava çok güzel.", "seviye": "Orta", "kategori": "Günlük"},
        {"icerik": "Bilgisayar mühendisliği okuyorum.", "seviye": "Zor", "kategori": "Günlük"},
    ]

    print("Veriler yükleniyor...")
    for v in veriler:
        var_mi = db.query(modeller.Metin).filter(modeller.Metin.icerik == v["icerik"]).first()
        if not var_mi:
            yeni = modeller.Metin(icerik=v["icerik"], seviye=v["seviye"], kategori=v["kategori"])
            db.add(yeni)
            print(f"+ Eklendi: {v['icerik']} ({v['seviye']})")

    db.commit()
    db.close()
    print("İşlem tamam!")


if __name__ == "__main__":
    yukle()