CREATE TABLE instagram_comments (
    id UUID PRIMARY KEY,
    connection_id UUID NOT NULL REFERENCES channel_connections(id) ON DELETE CASCADE,
    instagram_account_id VARCHAR(255) NOT NULL,
    instagram_account_username VARCHAR(255) NOT NULL,
    external_comment_id VARCHAR(255) NOT NULL,
    external_media_id VARCHAR(255),
    commenter_id VARCHAR(255),
    commenter_username VARCHAR(255),
    comment_text TEXT,
    media_product_type VARCHAR(64),
    comment_verb VARCHAR(32),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reply_status VARCHAR(16) NOT NULL DEFAULT 'NONE',
    reply_id VARCHAR(255),
    reply_text TEXT,
    replied_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_instagram_comment_connection_account_external
        UNIQUE (connection_id, instagram_account_id, external_comment_id),
    CONSTRAINT ck_instagram_comment_reply_status
        CHECK (reply_status IN ('NONE', 'SENDING', 'SENT', 'FAILED', 'UNKNOWN'))
);

CREATE INDEX ix_instagram_comments_connection_received
    ON instagram_comments (connection_id, received_at DESC, id DESC);
