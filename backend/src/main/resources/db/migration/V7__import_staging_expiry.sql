ALTER TABLE import_staging ADD COLUMN expires_at TIMESTAMPTZ;
UPDATE import_staging SET expires_at = created_at + INTERVAL '30 minutes' WHERE expires_at IS NULL;
ALTER TABLE import_staging ALTER COLUMN expires_at SET NOT NULL;
