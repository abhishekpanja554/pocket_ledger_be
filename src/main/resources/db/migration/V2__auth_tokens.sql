ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE auth_tokens (
     id          UUID PRIMARY KEY,
     user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
     token_hash  TEXT NOT NULL UNIQUE,
     purpose     TEXT NOT NULL CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD')),
     expires_at  TIMESTAMPTZ NOT NULL,
     used_at     TIMESTAMPTZ,
     created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_auth_tokens_user ON auth_tokens (user_id);