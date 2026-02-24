CREATE TABLE seller_profiles (
    user_id UUID PRIMARY KEY,
    company_name VARCHAR(255) NULL,
    bank_name VARCHAR(255) NULL,
    inn VARCHAR(12) NULL,
    bik VARCHAR(9) NULL,
    settlement_account VARCHAR(20) NULL,
    corporate_account VARCHAR(20) NULL,
    address VARCHAR(500) NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    creator_id UUID NULL,
    CONSTRAINT fk_seller_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
);
