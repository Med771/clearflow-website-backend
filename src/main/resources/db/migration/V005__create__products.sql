CREATE TABLE products (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    ozon_product_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    creator_id UUID NULL,
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES users (id)
);

CREATE UNIQUE INDEX uq_products_seller_ozon_product_id ON products (seller_id, ozon_product_id);
CREATE INDEX idx_products_seller_id ON products (seller_id);
CREATE INDEX idx_products_name ON products (name);
