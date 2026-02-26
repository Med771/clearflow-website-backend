CREATE TABLE user_photos (
    user_id UUID PRIMARY KEY,
    storage_type VARCHAR(20) NOT NULL,
    relative_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    creator_id UUID NULL,
    CONSTRAINT fk_user_photos_user FOREIGN KEY (user_id) REFERENCES users (id)
);
