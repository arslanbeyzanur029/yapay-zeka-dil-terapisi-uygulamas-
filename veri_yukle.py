from sqlalchemy.orm import Session
from veritabani import SessionLocal, engine
import modeller

# Tabloların veritabanında var olduğundan emin olalım
modeller.Base.metadata.create_all(bind=engine)


def yukle():
    db = SessionLocal()

    # --- ZENGİNLEŞTİRİLMİŞ CÜMLE LİSTESİ ---
    veriler = [
        # --- SEVİYE 1: KOLAY (Kısa ve Basit) ---
        {"icerik": "Ali topu at.", "seviye": "Kolay", "kategori": "Günlük"},
        {"icerik": "Elma yemeyi severim.", "seviye": "Kolay", "kategori": "Günlük"},
        {"icerik": "Oya okula koş.", "seviye": "Kolay", "kategori": "Günlük"},
        {"icerik": "Süt çok sıcak.", "seviye": "Kolay", "kategori": "Günlük"},
        {"icerik": "Kedi ağaca çıktı.", "seviye": "Kolay", "kategori": "Hayvanlar"},
        {"icerik": "Babam eve geldi.", "seviye": "Kolay", "kategori": "Aile"},
        {"icerik": "Mavi araba durdu.", "seviye": "Kolay", "kategori": "Renkler"},
        {"icerik": "Su içmek sağlıklıdır.", "seviye": "Kolay", "kategori": "Sağlık"},
        {"icerik": "Güneş doğdu.", "seviye": "Kolay", "kategori": "Doğa"},
        {"icerik": "Çiçekler açtı.", "seviye": "Kolay", "kategori": "Doğa"},
        {"icerik": "Kapıyı kapat.", "seviye": "Kolay", "kategori": "Emir"},
        {"icerik": "Çay demledim.", "seviye": "Kolay", "kategori": "Mutfak"},
        {"icerik": "Kuşlar uçuyor.", "seviye": "Kolay", "kategori": "Hayvanlar"},
        {"icerik": "Top oynadık.", "seviye": "Kolay", "kategori": "Oyun"},
        {"icerik": "Dişini fırçala.", "seviye": "Kolay", "kategori": "Sağlık"},

        # --- SEVİYE 2: ORTA (Biraz daha uzun cümleler) ---
        {"icerik": "Yarın sabah erkenden okula gideceğim.", "seviye": "Orta", "kategori": "Eğitim"},
        {"icerik": "Bugün hava düne göre daha serin.", "seviye": "Orta", "kategori": "Hava Durumu"},
        {"icerik": "Kütüphaneden aldığım kitabı bitirdim.", "seviye": "Orta", "kategori": "Eğitim"},
        {"icerik": "Arkadaşlarımla sinemaya gitmeyi planlıyoruz.", "seviye": "Orta", "kategori": "Sosyal"},
        {"icerik": "Annem akşam yemeği için mantı yaptı.", "seviye": "Orta", "kategori": "Yemek"},
        {"icerik": "Bilgisayar mühendisliği zor ama eğlenceli.", "seviye": "Orta", "kategori": "Eğitim"},
        {"icerik": "Hafta sonu dedemleri ziyarete gideceğiz.", "seviye": "Orta", "kategori": "Aile"},
        {"icerik": "Doktor ilaçlarımı düzenli kullanmamı söyledi.", "seviye": "Orta", "kategori": "Sağlık"},
        {"icerik": "Trafik çok yoğun olduğu için geç kaldım.", "seviye": "Orta", "kategori": "Ulaşım"},
        {"icerik": "Yeni aldığım bilgisayarın hızı çok yüksek.", "seviye": "Orta", "kategori": "Teknoloji"},
        {"icerik": "Basketbol maçını heyecanla izledik.", "seviye": "Orta", "kategori": "Spor"},
        {"icerik": "Yaz tatilinde Antalya'ya gitmek istiyoruz.", "seviye": "Orta", "kategori": "Seyahat"},
        {"icerik": "Marketten iki ekmek ve bir süt aldım.", "seviye": "Orta", "kategori": "Alışveriş"},

        # --- SEVİYE 3: ZOR (Tekerlemeler ve Karmaşık Cümleler) ---
        {"icerik": "Bir berber bir berbere gel beraber gidelim demiş.", "seviye": "Zor", "kategori": "Tekerleme"},
        {"icerik": "Şu köşe yaz köşesi şu köşe kış köşesi.", "seviye": "Zor", "kategori": "Tekerleme"},
        {"icerik": "Kartal kalkar dal sarkar dal sarkar kartal kalkar.", "seviye": "Zor", "kategori": "Tekerleme"},
        {"icerik": "Bu yoğurdu sarımsaklasak da mı saklasak?", "seviye": "Zor", "kategori": "Tekerleme"},
        {"icerik": "Çekoslovakyalılaştıramadıklarımızdan mısınız?", "seviye": "Zor", "kategori": "Tekerleme"},
        {"icerik": "Yapay zeka destekli mobil uygulamalar geliştiriyorum.", "seviye": "Zor", "kategori": "Teknoloji"},
        {"icerik": "Şemsi Paşa Pasajında sesi büzüşesiceler.", "seviye": "Zor", "kategori": "Tekerleme"},
        {"icerik": "Üç tunç tas has hoşaf.", "seviye": "Zor", "kategori": "Tekerleme"},
        {"icerik": "Sürdürülebilir enerji kaynakları üzerine araştırmalar yapılıyor.", "seviye": "Zor", "kategori": "Bilim"},
        {"icerik": "Nöropsikolojik testlerin sonuçlarını analiz ediyoruz.", "seviye": "Zor", "kategori": "Tıp"},
        {"icerik": "Endüstriyel tasarımda ergonomi çok önemlidir.", "seviye": "Zor", "kategori": "Mühendislik"},
        {"icerik": "Küresel ısınmanın ekolojik denge üzerindeki etkileri tartışılıyor.", "seviye": "Zor", "kategori": "Çevre"}
    ]

    print("Veriler veritabanına yükleniyor...")

    eklenen_sayisi = 0
    gecilen_sayisi = 0

    for v in veriler:
        # Aynı cümle var mı diye kontrol ediyoruz (Çift kayıt olmasın)
        var_mi = db.query(modeller.Metin).filter(modeller.Metin.icerik == v["icerik"]).first()

        if not var_mi:
            yeni = modeller.Metin(icerik=v["icerik"], seviye=v["seviye"], kategori=v["kategori"])
            db.add(yeni)
            print(f"+ Eklendi: {v['icerik']}")
            eklenen_sayisi += 1
        else:
            print(f"- Zaten var (Atlandı): {v['icerik']}")
            gecilen_sayisi += 1

    db.commit()
    db.close()
    print(f"\n✅ İŞLEM TAMAMLANDI!")
    print(f"Toplam Eklenen: {eklenen_sayisi}")
    print(f"Zaten Olan: {gecilen_sayisi}")


if __name__ == "__main__":
    yukle()