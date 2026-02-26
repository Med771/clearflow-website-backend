CREATE TABLE promo_stats_daily (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL,
    promo_code_id UUID NOT NULL,
    product_id UUID NOT NULL,
    stat_date DATE NOT NULL,
    orders_count BIGINT NOT NULL DEFAULT 0,
    items_count BIGINT NOT NULL DEFAULT 0,
    revenue NUMERIC(18,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    creator_id UUID NULL,
    CONSTRAINT fk_promo_stats_daily_seller FOREIGN KEY (seller_id) REFERENCES users (id),
    CONSTRAINT fk_promo_stats_daily_promo_code FOREIGN KEY (promo_code_id) REFERENCES promo_codes (id),
    CONSTRAINT fk_promo_stats_daily_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uq_promo_stats_daily UNIQUE (seller_id, promo_code_id, product_id, stat_date)
);

CREATE INDEX idx_promo_stats_daily_seller_date ON promo_stats_daily (seller_id, stat_date);
CREATE INDEX idx_promo_stats_daily_promo_code_date ON promo_stats_daily (promo_code_id, stat_date);
