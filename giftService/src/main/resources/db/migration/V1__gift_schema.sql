CREATE TABLE wishes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    link VARCHAR(500),
    priority INTEGER NOT NULL CHECK (priority BETWEEN 1 AND 10)
);
CREATE INDEX idx_wishes_user_priority ON wishes(user_id, priority);
