-- Local-runtime tables backing the SQLite adapters (embedded replacements for
-- Redis dedup keys and the Kafka event bus). These tables are only created for
-- the local profile; the production profile keeps Redis and Kafka instead.

CREATE TABLE idempotency_key (
    dedup_key  varchar(255) PRIMARY KEY,
    expires_at bigint NOT NULL
);

CREATE INDEX idx_idempotency_expires ON idempotency_key (expires_at);

CREATE TABLE event_outbox (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id        varchar(64) NOT NULL,
    park_id         varchar(64) NOT NULL,
    payload         text NOT NULL,
    status          varchar(16) NOT NULL,
    attempts        integer NOT NULL DEFAULT 0,
    last_error      varchar(2048),
    next_attempt_at bigint,
    created_at      bigint NOT NULL,
    updated_at      bigint NOT NULL
);

CREATE INDEX idx_event_outbox_status ON event_outbox (status, next_attempt_at);
CREATE INDEX idx_event_outbox_event ON event_outbox (event_id);
