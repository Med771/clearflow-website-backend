CREATE TABLE promo_code_products (
    promo_code_id UUID NOT NULL,
    product_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    creator_id UUID NULL,
    PRIMARY KEY (promo_code_id, product_id),
    CONSTRAINT fk_promo_code_products_promo_code FOREIGN KEY (promo_code_id) REFERENCES promo_codes (id),
    CONSTRAINT fk_promo_code_products_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_promo_code_products_product_id ON promo_code_products (product_id);
