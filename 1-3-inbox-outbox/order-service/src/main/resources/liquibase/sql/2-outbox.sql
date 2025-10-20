CREATE TABLE outbox
(
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50),
    aggregate_id UUID,
    event_type     VARCHAR(50),
    payload JSONB,
    status         VARCHAR(20) DEFAULT 'NEW',
    created_at     TIMESTAMP   DEFAULT now(),
    processed_at   TIMESTAMP
);
