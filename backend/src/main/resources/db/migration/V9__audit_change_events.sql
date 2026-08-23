CREATE TABLE change_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    change_sequence BIGINT NOT NULL,
    operation VARCHAR(16) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id UUID NOT NULL,
    source_id VARCHAR(300),
    import_id UUID REFERENCES import_staging(id) ON DELETE SET NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL,
    payload TEXT NOT NULL,
    CONSTRAINT uq_change_event_owner_sequence UNIQUE (owner_id, change_sequence)
);
CREATE INDEX idx_change_event_owner_sequence ON change_event(owner_id, change_sequence);

CREATE TABLE change_event_sequence (
    owner_id UUID PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
    next_sequence BIGINT NOT NULL
);
