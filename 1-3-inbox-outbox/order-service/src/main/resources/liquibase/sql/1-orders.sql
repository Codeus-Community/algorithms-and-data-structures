CREATE TABLE orders
(
    id UUID PRIMARY KEY,
    description TEXT,
    status      VARCHAR(20),
    created_at  TIMESTAMP DEFAULT now()
);