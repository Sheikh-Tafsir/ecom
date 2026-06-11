-- Chats --
CREATE INDEX idx_chat_participants_user ON chat_participants (user_id);

CREATE INDEX idx_chat_participants_chat ON chat_participants (chat_id);

CREATE INDEX idx_messages_chat ON messages (chat_id);

CREATE INDEX idx_message_views_message ON message_views (message_id);