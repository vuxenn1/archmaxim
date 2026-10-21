FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    HF_HOME=/home/app/.cache/huggingface

# The model cache directory must exist and belong to the app user before the
# named volume is mounted, so the volume inherits that ownership.
RUN useradd --uid 10001 --create-home --shell /usr/sbin/nologin app \
    && mkdir -p "$HF_HOME" \
    && chown -R app:app /home/app/.cache

WORKDIR /app

COPY requirements.txt .
# The default torch wheel bundles CUDA (several GB); this service runs on CPU.
RUN pip install --index-url https://download.pytorch.org/whl/cpu "$(grep '^torch==' requirements.txt)" \
    && pip install -r requirements.txt

COPY main.py .

USER app

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
