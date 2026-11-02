FROM python:3.11-slim
WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 DATA_DIR=/app/data
COPY requirements.txt requirements-local.txt ./
ARG INSTALL_LOCAL_MODELS=false
RUN pip install --no-cache-dir -r requirements.txt && if [ "$INSTALL_LOCAL_MODELS" = "true" ]; then pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu && pip install --no-cache-dir -r requirements-local.txt; fi
RUN useradd --create-home --uid 10001 appuser
COPY app ./app
USER appuser
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
