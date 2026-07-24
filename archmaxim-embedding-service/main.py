from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

app = FastAPI()

# Loaded once, when the service starts
model = SentenceTransformer("BAAI/bge-m3")

class EmbedRequest(BaseModel):
    text: str

@app.get("/")
def root():
    return {"status": "embedding service is running"}

@app.post("/embed")
def embed(request: EmbedRequest):
    vector = model.encode(request.text)
    return {"vector": vector.tolist()}