CREATE TABLE users (
       id            UUID PRIMARY KEY,
       email         TEXT NOT NULL UNIQUE,
       password_hash TEXT NOT NULL,
       created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE transactions (
      id          UUID PRIMARY KEY,
      user_id     UUID NOT NULL REFERENCES users(id),
      date        DATE NOT NULL,
      merchant    TEXT NOT NULL,
      category    TEXT NOT NULL DEFAULT 'Needs review',
      amount      NUMERIC(12,2) NOT NULL,
      type        TEXT NOT NULL CHECK (type IN
                                       ('expense','income')),
      account     TEXT NOT NULL DEFAULT 'Imported account',
      tags        JSONB NOT NULL DEFAULT '[]',
      receipt     BOOLEAN NOT NULL DEFAULT false,
      source      TEXT NOT NULL,
      fingerprint TEXT NOT NULL,
      created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
      UNIQUE (user_id, fingerprint)
);

CREATE INDEX idx_transactions_user_date ON transactions (user_id, date DESC);
CREATE INDEX idx_transactions_user_category ON transactions (user_id, category);

CREATE TABLE tags (
      user_id    UUID NOT NULL REFERENCES users(id),
      name       TEXT NOT NULL,
      created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      PRIMARY KEY (user_id, name)
);

CREATE TABLE rules (
       id         UUID PRIMARY KEY,
       user_id    UUID NOT NULL REFERENCES users(id),
       when_text  TEXT NOT NULL,
       then_text  TEXT NOT NULL,
       enabled    BOOLEAN NOT NULL DEFAULT true,
       created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_rules_user ON rules (user_id);

CREATE TABLE settings (
      user_id    UUID NOT NULL REFERENCES users(id),
      key        TEXT NOT NULL,
      value      JSONB NOT NULL,
      updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      PRIMARY KEY (user_id, key)
);

CREATE TABLE documents (
       id         UUID PRIMARY KEY,
       user_id    UUID NOT NULL REFERENCES users(id),
       filename   TEXT NOT NULL,
       mime_type  TEXT NOT NULL,
       size       BIGINT NOT NULL,
       object_key TEXT NOT NULL UNIQUE,
       status     TEXT NOT NULL,
       source     TEXT NOT NULL,
       created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_documents_user_created ON documents (user_id, created_at DESC);