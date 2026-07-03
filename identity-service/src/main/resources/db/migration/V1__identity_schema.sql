CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    birth_date DATE NOT NULL
);

CREATE TABLE friend_groups (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL UNIQUE,
    owner_id UUID NOT NULL REFERENCES users(id)
);

CREATE TABLE group_memberships (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES friend_groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_group_member UNIQUE (group_id, user_id)
);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    subscriber_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type VARCHAR(10) NOT NULL CHECK (target_type IN ('USER', 'GROUP')),
    target_id UUID NOT NULL,
    CONSTRAINT uq_subscription UNIQUE (subscriber_id, target_type, target_id)
);

CREATE INDEX idx_memberships_user ON group_memberships(user_id);
CREATE INDEX idx_subscriptions_subscriber ON subscriptions(subscriber_id);
