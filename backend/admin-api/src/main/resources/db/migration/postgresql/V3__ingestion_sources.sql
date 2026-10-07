-- Registered event sources and their credentials.

CREATE TABLE event_source (
    id          varchar(64) PRIMARY KEY,
    name        varchar(128) NOT NULL,
    source_type varchar(32) NOT NULL,
    park_id     varchar(64) NOT NULL,
    secret_hash varchar(128) NOT NULL,
    enabled     boolean NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_event_source_park ON event_source (park_id);
