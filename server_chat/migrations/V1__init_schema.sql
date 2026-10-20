CREATE TABLE IF NOT EXISTS chats
(
    id             UUID PRIMARY KEY,
    type           VARCHAR(32) NOT NULL,
    name           VARCHAR(100),
    image          VARCHAR(511),
    last_message   TEXT,
    last_sent      TIMESTAMPTZ            DEFAULT NOW(),
    last_sender_id UUID,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version        INT                  DEFAULT 0
);

CREATE TABLE IF NOT EXISTS chat_participants
(
    id             UUID PRIMARY KEY,
    chat_id        UUID        NOT NULL REFERENCES chats (id) ON UPDATE CASCADE ON DELETE CASCADE,
    user_id        UUID        NOT NULL,
    role           VARCHAR(32) NOT NULL DEFAULT 'member', -- 'admin', 'member'
    unread_message INTEGER     NOT NULL DEFAULT 0,
    last_seen      TIMESTAMPTZ            DEFAULT NOW(),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version        INT                  DEFAULT 0,
    CONSTRAINT uq_chat_participants_chat_user UNIQUE (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS messages
(
    id           UUID PRIMARY KEY,
    chat_id      UUID        NOT NULL REFERENCES chats (id) ON UPDATE CASCADE ON DELETE CASCADE,
    sender_id    UUID,
    content      TEXT        NOT NULL,
    content_type VARCHAR(32) NOT NULL DEFAULT 'text', -- 'text', 'image'
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version      INT                  DEFAULT 0
);

CREATE TABLE IF NOT EXISTS message_receipts
(
    id           UUID PRIMARY KEY,
    message_id   UUID      NOT NULL REFERENCES messages (id) ON DELETE CASCADE ON UPDATE CASCADE,
    user_id      UUID      NOT NULL,
    delivered_at TIMESTAMPTZ,
    read_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version      INT                DEFAULT 0,
    CONSTRAINT uq_message_receipts_message_user UNIQUE (message_id, user_id)
);

CREATE TABLE IF NOT EXISTS blocked_users
(
    id              UUID PRIMARY KEY,
    user_id         UUID        NOT NULL,
    blocked_user_id UUID        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_blocked_users UNIQUE (user_id, blocked_user_id)
);
