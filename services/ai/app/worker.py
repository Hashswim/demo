"""One durable PostgreSQL polling worker. Run `python -m app.worker`."""
import logging
import signal
import threading
import time

import psycopg
from psycopg.rows import dict_row
from psycopg.types.json import Jsonb
from . import config
from .schemas import AnalysisPayload
from .engine import recommend

logger = logging.getLogger("staffing.worker")
stop = threading.Event()


def connect():
    return psycopg.connect(config.DATABASE_URL, row_factory=dict_row)


def claim(connection):
    with connection.transaction():
        connection.execute("""UPDATE analysis_jobs SET status='FAILED', error=%s, updated_at=now(), lease_until=NULL
            WHERE status='RUNNING' AND lease_until < now() AND attempts >= %s""",
            ("분석 재시도 횟수를 초과했습니다.", config.MAX_ATTEMPTS))
        return connection.execute("""
            WITH picked AS (
                SELECT id FROM analysis_jobs
                WHERE (status='QUEUED' OR (status='RUNNING' AND lease_until < now()))
                  AND attempts < %s
                ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 1
            )
            UPDATE analysis_jobs j SET status='RUNNING', attempts=j.attempts+1,
                lease_until=now() + %s * interval '1 second', updated_at=now(), error=NULL
            FROM picked WHERE j.id=picked.id RETURNING j.*
            """, (config.MAX_ATTEMPTS, config.LEASE_SECONDS)).fetchone()


def heartbeat(job_id, attempt, done):
    while not done.wait(max(1, config.LEASE_SECONDS / 3)):
        try:
            with connect() as connection:
                changed = connection.execute("""UPDATE analysis_jobs SET lease_until=now() + %s * interval '1 second',
                    updated_at=now() WHERE id=%s AND status='RUNNING' AND attempts=%s""",
                    (config.LEASE_SECONDS, job_id, attempt)).rowcount
                if changed == 0:
                    return
        except psycopg.Error:
            logger.warning("Worker heartbeat failed for job_id=%s", job_id)


def process(job):
    done = threading.Event()
    thread = threading.Thread(target=heartbeat, args=(job["id"], job["attempts"], done), daemon=True)
    thread.start()
    try:
        payload = AnalysisPayload.model_validate(job["payload"])
        result = recommend(payload.request, payload.employees)
        with connect() as connection:
            # Attempt number fences stale workers whose lease was reclaimed.
            connection.execute("""UPDATE analysis_jobs SET status='SUCCEEDED', result=%s, error=NULL,
                lease_until=NULL, updated_at=now() WHERE id=%s AND status='RUNNING' AND attempts=%s""",
                (Jsonb(result), job["id"], job["attempts"]))
        logger.info("Analysis completed job_id=%s", job["id"])
    except Exception:
        with connect() as connection:
            final = job["attempts"] >= config.MAX_ATTEMPTS
            connection.execute("""UPDATE analysis_jobs SET status=%s, error=%s, lease_until=NULL,
                updated_at=now() WHERE id=%s AND status='RUNNING' AND attempts=%s""",
                ("FAILED" if final else "QUEUED", "분석 실행에 실패했습니다. 모델 또는 데이터 설정을 확인하세요.",
                 job["id"], job["attempts"]))
        logger.warning("Analysis failed job_id=%s attempt=%s", job["id"], job["attempts"])
    finally:
        done.set()
        thread.join(timeout=2)


def main():
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    signal.signal(signal.SIGTERM, lambda *_: stop.set())
    signal.signal(signal.SIGINT, lambda *_: stop.set())
    logger.info("Worker starting mode=%s", config.MODE)
    while not stop.is_set():
        try:
            with connect() as connection:
                job = claim(connection)
            if job:
                process(job)
            else:
                stop.wait(config.POLL_SECONDS)
        except psycopg.Error:
            logger.warning("Database unavailable; worker will retry")
            stop.wait(config.POLL_SECONDS)


if __name__ == "__main__":
    main()
