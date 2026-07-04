ALTER TABLE wishes
    ADD COLUMN reserved_by UUID,
    ADD COLUMN reserved_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_wishes_reserved_by ON wishes(reserved_by);
