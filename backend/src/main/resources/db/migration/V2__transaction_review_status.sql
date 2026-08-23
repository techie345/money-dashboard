ALTER TABLE financial_transaction
    ADD COLUMN IF NOT EXISTS review_status VARCHAR(16);
