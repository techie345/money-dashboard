CREATE TABLE import_staging (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    status VARCHAR(16) NOT NULL, filename VARCHAR(500) NOT NULL, profile VARCHAR(100) NOT NULL,
    imported_at TIMESTAMPTZ NOT NULL, confirmation_token VARCHAR(100) NOT NULL, normalized_json TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_import_staging_owner ON import_staging(owner_id);
CREATE TABLE import_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), import_id UUID NOT NULL REFERENCES import_staging(id),
    owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE, action VARCHAR(32) NOT NULL,
    source_filename VARCHAR(500) NOT NULL, profile VARCHAR(100) NOT NULL, imported_at TIMESTAMPTZ NOT NULL,
    source_identifiers TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_import_audit_owner ON import_audit(owner_id);
