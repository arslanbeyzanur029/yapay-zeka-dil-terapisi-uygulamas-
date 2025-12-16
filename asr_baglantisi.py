import torch
import librosa
import numpy as np
import os
from transformers import Wav2Vec2ForCTC, Wav2Vec2Processor


MODEL_PATH = "./turkish_asr_model"
SAMPLING_RATE = 16000



def load_asr_model_and_processor(model_path: str) -> tuple:
    if not os.path.exists(model_path) or not os.listdir(model_path):
        return None, None
    try:
        print(f"LOKAL MODEL YÜKLENİYOR...")
        processor = Wav2Vec2Processor.from_pretrained(model_path, local_files_only=True)
        model = Wav2Vec2ForCTC.from_pretrained(model_path, local_files_only=True)
        model.eval()
        print("MODEL HAZIR.")
        return processor, model
    except Exception:
        return None, None


processor, model = load_asr_model_and_processor(MODEL_PATH)




def run_asr_and_get_logits(file_path: str, processor, model) -> tuple:
    if processor is None or model is None:
        return "MODEL YOK", np.zeros((100, 32))

    try:

        speech_array, _ = librosa.load(file_path, sr=SAMPLING_RATE)


        inputs = processor(speech_array, sampling_rate=SAMPLING_RATE, return_tensors="pt", padding=True)

        with torch.no_grad():
            output = model(inputs.input_values)
            logits = output.logits
            predicted_ids = torch.argmax(logits, dim=-1)
            transcription = processor.batch_decode(predicted_ids, skip_special_tokens=True)[0]
            transcription = transcription.lower()
            logits_numpy = logits.squeeze(0).cpu().numpy()
            return transcription, logits_numpy

    except Exception as e:
        print(f"İşleme Hatası: {e}")
        return "HATA OLUŞTU", np.zeros((10, 10))


def sesi_metne_cevir(dosya_yolu: str) -> tuple:
    if not os.path.exists(dosya_yolu):
        return "Dosya Bulunamadı", np.array([0])

    return run_asr_and_get_logits(dosya_yolu, processor, model)