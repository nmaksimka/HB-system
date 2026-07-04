CREATE TABLE fundraisers (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    beneficiary_id UUID NOT NULL,
    wish_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    target_amount NUMERIC(12, 2) NOT NULL CHECK (target_amount > 0),
    collected_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_fundraisers_beneficiary ON fundraisers(beneficiary_id);
CREATE UNIQUE INDEX uq_fundraisers_active_wish ON fundraisers(wish_id) WHERE status = 'ACTIVE';

CREATE TABLE contributions (
    payment_id UUID PRIMARY KEY,
    fundraiser_id UUID NOT NULL REFERENCES fundraisers(id),
    payer_id UUID NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    paid_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_contributions_fundraiser ON contributions(fundraiser_id, paid_at);

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
