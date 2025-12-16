# modeller.py
from sqlalchemy import Column, Integer, String, ForeignKey, DateTime
from sqlalchemy.sql import func
from veritabani import Base


class Metin(Base):
    __tablename__ = "metinler"

    id = Column(Integer, primary_key=True, index=True)
    icerik = Column(String, index=True)
    seviye = Column(String)  # "Kolay", "Orta", "Zor"
    kategori = Column(String)  # "Tekerleme", "Günlük"


class Skor(Base):
    __tablename__ = "skorlar"

    id = Column(Integer, primary_key=True, index=True)
    metin_id = Column(Integer, ForeignKey("metinler.id"))  # Hangi kart?
    kullanici_id = Column(String, index=True)  # Kim okudu?
    alinan_puan = Column(Integer)  # Kaç aldı?
    tarih = Column(DateTime(timezone=True), server_default=func.now())  # Ne zaman?