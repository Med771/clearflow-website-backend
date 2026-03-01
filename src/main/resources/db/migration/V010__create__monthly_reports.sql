CREATE TABLE monthly_reports (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL,
    report_month VARCHAR(7) NOT NULL,
    invoice_date DATE NOT NULL,
    pdf_content BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    creator_id UUID NULL,
    CONSTRAINT fk_monthly_reports_seller FOREIGN KEY (seller_id) REFERENCES users (id),
    CONSTRAINT uq_monthly_reports_seller_month UNIQUE (seller_id, report_month)
);

CREATE INDEX idx_monthly_reports_seller ON monthly_reports (seller_id);
CREATE INDEX idx_monthly_reports_month ON monthly_reports (report_month);
