CREATE TABLE inbox
(
    id UUID PRIMARY KEY,
    event_type   VARCHAR(50),
    payload JSONB,
    status       VARCHAR(20) DEFAULT 'RECEIVED',
    received_at  TIMESTAMP   DEFAULT now(),
    processed_at TIMESTAMP
);