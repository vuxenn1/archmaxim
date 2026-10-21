import pytest
from fastapi.testclient import TestClient

import main


class FakeVectors(list):
    def tolist(self):
        return list(self)


class FakeModel:
    """Encodes each text as [len(text), index] so tests can check order."""

    def get_embedding_dimension(self):
        return 2

    def encode(self, texts, **kwargs):
        assert kwargs["normalize_embeddings"] is True
        return FakeVectors([float(len(text)), float(i)] for i, text in enumerate(texts))


@pytest.fixture
def client(monkeypatch):
    monkeypatch.setattr(main, "load_model", FakeModel)
    with TestClient(main.app) as test_client:
        yield test_client


def test_health_reports_model_and_dimension(client):
    for path in ("/health", "/"):
        response = client.get(path)
        assert response.status_code == 200
        assert response.json() == {"status": "ok", "model": main.MODEL_NAME, "dimension": 2}


def test_embed_returns_one_vector(client):
    response = client.post("/embed", json={"text": "programlama 2 vize"})
    assert response.status_code == 200
    assert response.json() == {"vector": [18.0, 0.0]}


def test_embed_strips_surrounding_whitespace(client):
    response = client.post("/embed", json={"text": "  vize  "})
    assert response.json()["vector"][0] == 4.0


@pytest.mark.parametrize("text", ["", "   ", "x" * (main.MAX_TEXT_LENGTH + 1)])
def test_embed_rejects_blank_or_oversized_text(client, text):
    assert client.post("/embed", json={"text": text}).status_code == 422


def test_batch_keeps_input_order(client):
    response = client.post("/embed/batch", json={"texts": ["a", "bbb", "cc"]})
    assert response.status_code == 200
    assert response.json() == {"vectors": [[1.0, 0.0], [3.0, 1.0], [2.0, 2.0]]}


def test_batch_rejects_empty_and_oversized_lists(client):
    assert client.post("/embed/batch", json={"texts": []}).status_code == 422
    too_many = ["x"] * (main.MAX_BATCH_ITEMS + 1)
    assert client.post("/embed/batch", json={"texts": too_many}).status_code == 422


def test_batch_rejects_blank_item(client):
    assert client.post("/embed/batch", json={"texts": ["ok", " "]}).status_code == 422
