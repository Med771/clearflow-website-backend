CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_block BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    parent_id UUID NULL,
    session_version BIGINT NOT NULL DEFAULT 0,
    ozon_api_key_ciphertext VARCHAR(2048) NULL,
    ozon_api_key_key_version VARCHAR(50) NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    creator_id UUID NULL,
    CONSTRAINT fk_users_parent FOREIGN KEY (parent_id) REFERENCES users (id)
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_parent_id ON users(parent_id);
CREATE INDEX idx_users_active ON users(is_active);
