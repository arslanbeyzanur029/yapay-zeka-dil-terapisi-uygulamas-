# veritabani.py
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

# Veri tabanı dosyamız
SQLALCHEMY_DATABASE_URL = "sqlite:///./terapi.db"

# Motoru çalıştırıyoruz
engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
)

# Oturum fabrikası
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# Temel sınıf
Base = declarative_base()