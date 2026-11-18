ALTER TABLE instagram_comment_automation_executions
    ADD COLUMN direct_message_id VARCHAR(255);

ALTER TABLE instagram_comment_automation_executions
    ADD COLUMN link_button_status VARCHAR(16) NOT NULL DEFAULT 'NOT_APPLICABLE';

ALTER TABLE instagram_comment_automation_executions
    ADD CONSTRAINT ck_instagram_automation_execution_link_button_status
    CHECK (link_button_status IN ('NOT_APPLICABLE', 'PENDING', 'PROCESSING', 'SENT', 'FAILED', 'UNKNOWN'));

CREATE INDEX ix_instagram_comments_account_commenter
    ON instagram_comments (instagram_account_id, commenter_id);

CREATE INDEX ix_instagram_automation_link_button_status
    ON instagram_comment_automation_executions (direct_message_status, link_button_status, created_at);
