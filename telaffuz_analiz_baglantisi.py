import numpy as np
import difflib
import re


def _normalize_text(text: str) -> str:

    if not isinstance(text, str):
        return ""

    text = text.lower()

    text = re.sub(r"[^a-zçğıöşü0-9\s]", " ", text)

    text = re.sub(r"\s+", " ", text).strip()
    return text


def _metin_benzerligi_hesapla(hedef: str, bulunan: str) -> float:

    hedef_norm = _normalize_text(hedef)
    bulunan_norm = _normalize_text(bulunan)

    if not hedef_norm:
        return 0.0


    ratio = difflib.SequenceMatcher(None, hedef_norm, bulunan_norm).ratio()
    return float(ratio)


def _logit_guven_skoru_hesapla(logit_verisi) -> float:

    if logit_verisi is None:
        return 0.0

    try:
        logit_verisi = np.array(logit_verisi)


        if logit_verisi.size == 0 or logit_verisi.ndim < 2:
            return 0.0


        max_vals = np.max(logit_verisi, axis=1, keepdims=True)
        exp_logits = np.exp(logit_verisi - max_vals)
        probs = exp_logits / np.sum(exp_logits, axis=1, keepdims=True)


        max_probs = np.max(probs, axis=1)
        return float(np.mean(max_probs))

    except Exception as e:

        print(f"Güven skoru hatası: {e}")
        return 0.0


def telaffuzu_puanla(ses_yolu, asr_metni, logits, referans_metin):



    metin_skoru = _metin_benzerligi_hesapla(referans_metin, asr_metni)


    akustik_skor = _logit_guven_skoru_hesapla(logits)


    final_skor = (0.6 * metin_skoru) + (0.4 * akustik_skor)


    puan_int = int(final_skor * 100)
    puan_int = max(0, min(100, puan_int))

    return puan_int