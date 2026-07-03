CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    subject_user_id UUID NOT NULL,
    author_id UUID NOT NULL,
    text VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_chat_subject_created ON chat_messages(subject_user_id, created_at);
