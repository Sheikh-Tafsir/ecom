DROP TABLE IF EXISTS message_views CASCADE;
DROP TABLE IF EXISTS messages CASCADE;
DROP TABLE IF EXISTS chat_participants CASCADE;
DROP TABLE IF EXISTS chats CASCADE;

CREATE TABLE chats
(
    id           BIGSERIAL PRIMARY KEY,
    type         VARCHAR(32) NOT NULL,
    name         VARCHAR(100),
    image        VARCHAR(511),
    last_message TEXT,
    last_sent    TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version      INT                  DEFAULT 0
);

CREATE TABLE chat_participants
(
    id             BIGSERIAL PRIMARY KEY,
    chat_id        BIGINT      NOT NULL REFERENCES chats (id) ON UPDATE CASCADE ON DELETE CASCADE,
    user_id        BIGINT      NOT NULL REFERENCES users (id) ON UPDATE CASCADE ON DELETE SET NULL,
    role           VARCHAR(32) NOT NULL DEFAULT 'member', -- 'admin', 'member'
    unread_message INTEGER     NOT NULL DEFAULT 0,
    last_seen      TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version        INT                  DEFAULT 0,
    CONSTRAINT uq_chat_participants_chat_user UNIQUE (chat_id, user_id)
);

CREATE TABLE messages
(
    id           BIGSERIAL PRIMARY KEY,
    chat_id      BIGINT      NOT NULL REFERENCES chats (id) ON UPDATE CASCADE ON DELETE CASCADE,
    sender_id    BIGINT      NOT NULL REFERENCES users (id) ON UPDATE CASCADE ON DELETE SET NULL,
    content      TEXT        NOT NULL,
    content_type VARCHAR(32) NOT NULL DEFAULT 'text', -- 'text', 'image'
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version      INT                  DEFAULT 0
);

CREATE TABLE message_receipts
(
    id           BIGSERIAL PRIMARY KEY,
    message_id   BIGINT    NOT NULL REFERENCES messages (id) ON DELETE CASCADE ON UPDATE CASCADE,
    user_id      BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    delivered_at TIMESTAMP,
    read_at      TIMESTAMP,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version      INT                DEFAULT 0,
    CONSTRAINT uq_message_receipts_message_user UNIQUE (message_id, user_id)
);
