import logging
import os
from contextlib import asynccontextmanager
from typing import Annotated

from fastapi import FastAPI, Request
from pydantic import BaseModel, Field, StringConstraints

MODEL_NAME = os.getenv("EMBEDDING_MODEL", "BAAI/bge-m3")
MAX_TEXT_LENGTH = int(os.getenv("EMBEDDING_MAX_TEXT_LENGTH", "8000"))
MAX_BATCH_ITEMS = int(os.getenv("EMBEDDING_MAX_BATCH_ITEMS", "64"))
ENCODE_BATCH_SIZE = int(os.getenv("EMBEDDING_ENCODE_BATCH_SIZE", "16"))

logging.basicConfig(
    level=os.getenv("LOG_LEVEL", "INFO"),
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
)
log = logging.getLogger("embedding")
# The Hugging Face client logs every request it makes while resolving the model.
for noisy in ("httpx", "httpx2", "urllib3"):
    logging.getLogger(noisy).setLevel(logging.WARNING)

Text = Annotated[str, StringConstraints(strip_whitespace=True, min_length=1, max_length=MAX_TEXT_LENGTH)]


def load_model():
    # Imported here so the API can be tested without torch installed.
    from sentence_transformers import SentenceTransformer

    return SentenceTransformer(MODEL_NAME)


@asynccontextmanager
async def lifespan(app: FastAPI):
    log.info("Loading model %s", MODEL_NAME)
    app.state.model = load_model()
    app.state.dimension = app.state.model.get_embedding_dimension()
    log.info("Model ready, dimension %d", app.state.dimension)
    yield


app = FastAPI(title="Archmaxim Embedding Service", version="1.0.0", lifespan=lifespan)


class EmbedRequest(BaseModel):
    text: Text


class EmbedResponse(BaseModel):
    vector: list[float]


class BatchEmbedRequest(BaseModel):
    texts: list[Text] = Field(min_length=1, max_length=MAX_BATCH_ITEMS)


class BatchEmbedResponse(BaseModel):
    vectors: list[list[float]]


class HealthResponse(BaseModel):
    status: str
    model: str
    dimension: int


def encode(request: Request, texts: list[str]) -> list[list[float]]:
    vectors = request.app.state.model.encode(
        texts,
        batch_size=ENCODE_BATCH_SIZE,
        normalize_embeddings=True,
        convert_to_numpy=True,
    )
    return vectors.tolist()


@app.get("/health")
@app.get("/", include_in_schema=False)
def health(request: Request) -> HealthResponse:
    return HealthResponse(status="ok", model=MODEL_NAME, dimension=request.app.state.dimension)


@app.post("/embed")
def embed(body: EmbedRequest, request: Request) -> EmbedResponse:
    return EmbedResponse(vector=encode(request, [body.text])[0])


@app.post("/embed/batch")
def embed_batch(body: BatchEmbedRequest, request: Request) -> BatchEmbedResponse:
    return BatchEmbedResponse(vectors=encode(request, body.texts))
