-- Dispatch delivery attempts and dead-letter records (SQLite dialect).

CREATE TABLE delivery (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    park_id         varchar(64) NOT NULL,
    event_id        varchar(64) NOT NULL,
    subscription_id varchar(64) NOT NULL,
    target_type     varchar(32) NOT NULL,
    target          varchar(512),
    status          varchar(16) NOT NULL,
    attempts        integer NOT NULL DEFAULT 0,
    last_error      varchar(2048),
    dead_letter     boolean NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_delivery_sub_event ON delivery (subscription_id, event_id);
CREATE INDEX idx_delivery_park_time ON delivery (park_id, created_at DESC);
CREATE INDEX idx_delivery_event ON delivery (event_id);
CREATE INDEX idx_delivery_dead_letter ON delivery (park_id, dead_letter);
