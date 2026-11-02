import os
from pathlib import Path

MODE = os.getenv("AI_MODE", "baseline")
if MODE not in {"baseline", "local"}:
    raise RuntimeError("AI_MODE must be baseline or local")
DEFAULT_DATA_DIR = next((p / "data" for p in Path(__file__).resolve().parents if (p / "data").is_dir()), Path("/app/data"))
DATA_DIR = Path(os.getenv("DATA_DIR", str(DEFAULT_DATA_DIR)))
DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://staffing:staffing@localhost:5432/staffing")
MODEL_BASE_URL = os.getenv("MODEL_BASE_URL", "http://localhost:8080/v1").rstrip("/")
MODEL_NAME = os.getenv("MODEL_NAME", "qwen-local")
EMBEDDING_MODEL_PATH = os.getenv("EMBEDDING_MODEL_PATH", "/models/embedding")
LOCAL_FILES_ONLY = os.getenv("LOCAL_FILES_ONLY", "true").lower() == "true"
LEASE_SECONDS = int(os.getenv("WORKER_LEASE_SECONDS", "600"))
MAX_ATTEMPTS = int(os.getenv("WORKER_MAX_ATTEMPTS", "3"))
POLL_SECONDS = float(os.getenv("WORKER_POLL_SECONDS", "2"))
MODEL_TIMEOUT_SECONDS = float(os.getenv("MODEL_TIMEOUT_SECONDS", "120"))
