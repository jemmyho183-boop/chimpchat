ALTER TABLE instagram_comment_automations
    ADD COLUMN definition_version INTEGER NOT NULL DEFAULT 1;

ALTER TABLE instagram_comment_automations
    ADD COLUMN definition_json TEXT;

ALTER TABLE instagram_comment_automation_executions
    ADD COLUMN automation_version INTEGER NOT NULL DEFAULT 1;

ALTER TABLE instagram_comment_automation_executions
    ADD COLUMN definition_snapshot TEXT;

CREATE TABLE automation_step_executions (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL REFERENCES instagram_comment_automation_executions(id) ON DELETE CASCADE,
    step_id VARCHAR(100) NOT NULL,
    step_type VARCHAR(100) NOT NULL,
    status VARCHAR(16) NOT NULL,
    provider_result_id VARCHAR(255),
    error_message VARCHAR(1000),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_automation_step_execution UNIQUE (execution_id, step_id),
    CONSTRAINT ck_automation_step_execution_status
        CHECK (status IN ('PROCESSING', 'SENT', 'FAILED', 'UNKNOWN'))
);

CREATE INDEX ix_automation_step_execution_status
    ON automation_step_executions (status, started_at);
