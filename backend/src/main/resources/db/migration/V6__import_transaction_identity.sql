CREATE UNIQUE INDEX uq_transaction_owner_source_id
    ON financial_transaction(owner_id, source, source_id)
    WHERE source_id IS NOT NULL;
