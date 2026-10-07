-- Subscription definitions with filter and transform rules (SQLite dialect).

CREATE TABLE subscription (
    id                     varchar(64) PRIMARY KEY,
    name                   varchar(128) NOT NULL,
    park_id                varchar(64) NOT NULL,
    event_type             varchar(128) NOT NULL,
    enabled                boolean NOT NULL DEFAULT true,
    filter_expression      varchar(2048),
    transform_rules        text,
    target_type            varchar(32) NOT NULL,
    target_url             varchar(512),
    notification_channel_id varchar(64),
    template_id            varchar(64),
    ordered_delivery       boolean NOT NULL DEFAULT false,
    max_retries            integer NOT NULL DEFAULT 3,
    backoff_millis         bigint NOT NULL DEFAULT 1000,
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subscription_park_enabled ON subscription (park_id, enabled);
CREATE INDEX idx_subscription_park_type ON subscription (park_id, event_type);
