CREATE TABLE reminders (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL,
    birthday_user_id UUID NOT NULL,
    birthday_user_name VARCHAR(120) NOT NULL,
    birthday_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_reminder UNIQUE (recipient_id, birthday_user_id, birthday_date)
);
CREATE INDEX idx_reminders_recipient_date ON reminders(recipient_id, birthday_date);
