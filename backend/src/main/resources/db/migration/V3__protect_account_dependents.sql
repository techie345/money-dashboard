ALTER TABLE financial_transaction DROP CONSTRAINT IF EXISTS financial_transaction_account_id_fkey;
ALTER TABLE financial_transaction ADD CONSTRAINT financial_transaction_account_fk
    FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE RESTRICT;
ALTER TABLE holding DROP CONSTRAINT IF EXISTS holding_account_id_fkey;
ALTER TABLE holding ADD CONSTRAINT holding_account_fk
    FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE RESTRICT;
