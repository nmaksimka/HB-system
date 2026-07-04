ALTER TABLE reminders
    ADD COLUMN threshold_days INTEGER NOT NULL DEFAULT 30;

ALTER TABLE reminders
    DROP CONSTRAINT uq_reminder;

ALTER TABLE reminders
    ADD CONSTRAINT uq_reminder UNIQUE (recipient_id, birthday_user_id, birthday_date, threshold_days);
