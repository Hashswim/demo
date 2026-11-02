CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE app_users (
    username varchar(255) PRIMARY KEY,
    password_hash varchar(255) NOT NULL,
    role varchar(255) NOT NULL CHECK (role IN ('ACCOUNT','MANAGER')),
    enabled boolean NOT NULL DEFAULT true
);
CREATE TABLE employees (
    id varchar(255) PRIMARY KEY,
    data jsonb NOT NULL,
    version bigint NOT NULL DEFAULT 0
);
CREATE TABLE staffing_requests (
    id varchar(255) PRIMARY KEY,
    owner varchar(255) NOT NULL REFERENCES app_users(username),
    data jsonb NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX staffing_requests_owner_idx ON staffing_requests(owner,created_at DESC);
CREATE TABLE analysis_jobs (
    id varchar(255) PRIMARY KEY,
    request_id varchar(255) NOT NULL REFERENCES staffing_requests(id),
    payload jsonb NOT NULL,
    status varchar(255) NOT NULL CHECK(status IN ('QUEUED','RUNNING','SUCCEEDED','FAILED')),
    result jsonb,
    error text,
    attempts integer NOT NULL DEFAULT 0,
    lease_until timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX analysis_jobs_poll_idx ON analysis_jobs(status,created_at);
CREATE UNIQUE INDEX analysis_jobs_one_active_idx ON analysis_jobs(request_id) WHERE status IN ('QUEUED','RUNNING');
CREATE TABLE shortlists (
    id varchar(255) PRIMARY KEY,
    request_id varchar(255) NOT NULL REFERENCES staffing_requests(id),
    employee_id varchar(255) NOT NULL REFERENCES employees(id),
    owner varchar(255) NOT NULL REFERENCES app_users(username),
    reason varchar(2000) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(owner,request_id,employee_id)
);
CREATE TABLE ai_embedding_cache (
    content_hash varchar(64) PRIMARY KEY,
    model_id text NOT NULL,
    embedding vector(1024) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE audit_events (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor varchar(255) NOT NULL,
    action varchar(100) NOT NULL,
    target_id varchar(255),
    created_at timestamptz NOT NULL DEFAULT now()
);
