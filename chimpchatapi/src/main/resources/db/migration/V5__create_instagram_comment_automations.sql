CREATE TABLE instagram_comment_automations (
    id UUID PRIMARY KEY,
    connection_id UUID NOT NULL REFERENCES channel_connections(id) ON DELETE CASCADE,
    instagram_account_id VARCHAR(255) NOT NULL,
    external_media_id VARCHAR(255) NOT NULL,
    media_label VARCHAR(500) NOT NULL,
    match_type VARCHAR(16) NOT NULL,
    keyword VARCHAR(100),
    public_reply TEXT NOT NULL,
    direct_message TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_instagram_automation_connection_account_media
        UNIQUE (connection_id, instagram_account_id, external_media_id),
    CONSTRAINT ck_instagram_automation_match_type
        CHECK (match_type IN ('ANY_COMMENT', 'KEYWORD'))
);

CREATE TABLE instagram_comment_automation_executions (
    id UUID PRIMARY KEY,
    comment_id UUID NOT NULL REFERENCES instagram_comments(id) ON DELETE CASCADE,
    automation_id UUID NOT NULL REFERENCES instagram_comment_automations(id) ON DELETE CASCADE,
    public_reply_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    direct_message_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_instagram_automation_execution_comment_flow
        UNIQUE (comment_id, automation_id),
    CONSTRAINT ck_instagram_automation_execution_public_status
        CHECK (public_reply_status IN ('PENDING', 'SENT', 'FAILED', 'UNKNOWN')),
    CONSTRAINT ck_instagram_automation_execution_dm_status
        CHECK (direct_message_status IN ('PENDING', 'SENT', 'FAILED', 'UNKNOWN'))
);

CREATE INDEX ix_instagram_automation_account_media
    ON instagram_comment_automations (connection_id, instagram_account_id, external_media_id, enabled);
