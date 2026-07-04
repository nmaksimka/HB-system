CREATE TABLE collections (
    fundraiser_id UUID PRIMARY KEY,
    beneficiary_id UUID NOT NULL,
    target_amount NUMERIC(12, 2) NOT NULL CHECK (target_amount > 0),
    open BOOLEAN NOT NULL,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    fundraiser_id UUID NOT NULL REFERENCES collections(fundraiser_id),
    payer_id UUID NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    idempotency_key VARCHAR(64) NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_payment_idempotency UNIQUE (payer_id, idempotency_key)
);
CREATE INDEX idx_payments_fundraiser ON payments(fundraiser_id);

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    topic VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    attempts INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_outbox_unsent ON outbox_events(created_at) WHERE sent_at IS NULL;
