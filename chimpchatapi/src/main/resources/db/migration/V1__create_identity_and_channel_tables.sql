CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE external_identities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id),
    provider VARCHAR(32) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_external_identity_provider_subject UNIQUE (provider, subject),
    CONSTRAINT uq_external_identity_user_provider UNIQUE (user_id, provider)
);

CREATE TABLE workspaces (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL UNIQUE REFERENCES app_users(id),
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE email_login_tokens (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    request_ip VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_email_login_tokens_email_created
    ON email_login_tokens(email, created_at);

CREATE TABLE channel_connections (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id),
    provider VARCHAR(32) NOT NULL,
    external_business_id VARCHAR(255) NOT NULL,
    encrypted_access_token TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_channel_connection_workspace_provider UNIQUE (workspace_id, provider)
);

CREATE TABLE channel_accounts (
    id UUID PRIMARY KEY,
    connection_id UUID NOT NULL REFERENCES channel_connections(id) ON DELETE CASCADE,
    external_account_id VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    CONSTRAINT uq_channel_account_connection_external UNIQUE (connection_id, external_account_id)
);
