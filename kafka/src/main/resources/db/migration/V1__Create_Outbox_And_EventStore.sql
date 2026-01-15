CREATE TABLE IF NOT EXISTS outbox (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    correlation_id VARCHAR(255) NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_published ON outbox(published, created_at);
CREATE INDEX idx_outbox_aggregate ON outbox(aggregate_id, aggregate_type);

CREATE TABLE IF NOT EXISTS event_store (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    version INT NOT NULL,
    payload TEXT NOT NULL,
    correlation_id VARCHAR(255),
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_event_store_aggregate ON event_store(aggregate_id, aggregate_type, version);
CREATE INDEX idx_event_store_type ON event_store(event_type);
