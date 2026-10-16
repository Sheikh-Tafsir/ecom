-- Chats --
CREATE INDEX IF NOT EXISTS idx_chat_participants_user ON chat_participants (user_id);

CREATE INDEX IF NOT EXISTS idx_chat_participants_chat ON chat_participants (chat_id);

CREATE INDEX IF NOT EXISTS idx_messages_chat_id ON messages (chat_id, id DESC);

CREATE INDEX IF NOT EXISTS idx_message_receipts_message ON message_receipts (message_id);