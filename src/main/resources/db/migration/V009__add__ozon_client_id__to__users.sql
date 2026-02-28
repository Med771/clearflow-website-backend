ALTER TABLE users
    ADD COLUMN ozon_client_id VARCHAR(100) NULL;

CREATE INDEX idx_users_ozon_client_id ON users(ozon_client_id);
