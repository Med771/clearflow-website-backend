CREATE TABLE promo_codes (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    action_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    creator_id UUID NULL,
    CONSTRAINT fk_promo_codes_seller FOREIGN KEY (seller_id) REFERENCES users (id)
);

CREATE UNIQUE INDEX uq_promo_codes_seller_action_id ON promo_codes (seller_id, action_id);
CREATE INDEX idx_promo_codes_seller_id ON promo_codes (seller_id);
CREATE INDEX idx_promo_codes_name ON promo_codes (name);
