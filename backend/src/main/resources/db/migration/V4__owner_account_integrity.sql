ALTER TABLE account ADD CONSTRAINT account_owner_id_id_key UNIQUE (owner_id, id);
ALTER TABLE financial_transaction DROP CONSTRAINT IF EXISTS financial_transaction_account_fk;
ALTER TABLE financial_transaction ADD CONSTRAINT financial_transaction_owner_account_fk
    FOREIGN KEY (owner_id, account_id) REFERENCES account(owner_id, id) ON DELETE RESTRICT;
ALTER TABLE holding DROP CONSTRAINT IF EXISTS holding_account_fk;
ALTER TABLE holding ADD CONSTRAINT holding_owner_account_fk
    FOREIGN KEY (owner_id, account_id) REFERENCES account(owner_id, id) ON DELETE RESTRICT;
ALTER TABLE liability DROP CONSTRAINT IF EXISTS liability_account_id_fkey;
ALTER TABLE liability ADD CONSTRAINT liability_owner_account_fk
    FOREIGN KEY (owner_id, account_id) REFERENCES account(owner_id, id) ON DELETE RESTRICT;
ALTER TABLE merchant_rule DROP CONSTRAINT IF EXISTS merchant_rule_account_id_fkey;
ALTER TABLE merchant_rule ADD CONSTRAINT rule_owner_account_fk
    FOREIGN KEY (owner_id, account_id) REFERENCES account(owner_id, id) ON DELETE CASCADE;
ALTER TABLE net_worth_snapshot DROP CONSTRAINT IF EXISTS net_worth_snapshot_account_id_fkey;
ALTER TABLE net_worth_snapshot ADD CONSTRAINT snapshot_owner_account_fk
    FOREIGN KEY (owner_id, account_id) REFERENCES account(owner_id, id) ON DELETE RESTRICT;
ALTER TABLE net_worth_snapshot ADD CONSTRAINT snapshot_net_worth_check
    CHECK (net_worth_cents = assets_cents - liabilities_cents);
ALTER TABLE asset ADD CONSTRAINT asset_value_nonnegative CHECK (value_cents >= 0);
ALTER TABLE holding ADD CONSTRAINT holding_money_nonnegative CHECK (cost_basis_cents >= 0 AND market_value_cents >= 0);
ALTER TABLE holding ADD CONSTRAINT holding_shares_nonnegative CHECK (shares >= 0);
ALTER TABLE liability ADD CONSTRAINT liability_minimum_nonnegative CHECK (minimum_payment_cents IS NULL OR minimum_payment_cents >= 0);
