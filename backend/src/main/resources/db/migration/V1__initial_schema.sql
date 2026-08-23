CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    google_subject VARCHAR(255) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    email VARCHAR(320) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_user_google_subject UNIQUE (google_subject)
);

CREATE INDEX idx_app_user_email ON app_user (email);

CREATE TABLE account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    institution VARCHAR(200) NOT NULL,
    name VARCHAR(200) NOT NULL,
    kind VARCHAR(32) NOT NULL,
    balance_cents BIGINT NOT NULL,
    last_imported_at TIMESTAMPTZ,
    source VARCHAR(100),
    source_file VARCHAR(500),
    imported_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_account_owner ON account(owner_id);

CREATE TABLE financial_transaction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    transaction_date DATE NOT NULL,
    merchant VARCHAR(300) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    kind VARCHAR(40) NOT NULL,
    category VARCHAR(64) NOT NULL,
    direction VARCHAR(16),
    category_source VARCHAR(16),
    source VARCHAR(100) NOT NULL,
    source_file VARCHAR(500) NOT NULL,
    source_id VARCHAR(300),
    review_status VARCHAR(16),
    transfer_type VARCHAR(40),
    imported_at TIMESTAMPTZ,
    action VARCHAR(200),
    symbol VARCHAR(32),
    shares NUMERIC(24, 8),
    price_cents BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_transaction_owner_date ON financial_transaction(owner_id, transaction_date);
CREATE INDEX idx_transaction_owner_account ON financial_transaction(owner_id, account_id);
CREATE INDEX idx_transaction_owner_source ON financial_transaction(owner_id, source, source_id);

CREATE TABLE asset (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    kind VARCHAR(32) NOT NULL,
    value_cents BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_asset_owner ON asset(owner_id);

CREATE TABLE holding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    symbol VARCHAR(32) NOT NULL,
    shares NUMERIC(24, 8) NOT NULL,
    cost_basis_cents BIGINT NOT NULL,
    market_value_cents BIGINT NOT NULL,
    source VARCHAR(100),
    source_file VARCHAR(500),
    imported_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_holding_owner_account ON holding(owner_id, account_id);

CREATE TABLE liability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    account_id UUID REFERENCES account(id) ON DELETE SET NULL,
    name VARCHAR(200) NOT NULL,
    kind VARCHAR(40) NOT NULL,
    balance_cents BIGINT NOT NULL CHECK (balance_cents > 0),
    interest_rate NUMERIC(12, 6),
    minimum_payment_cents BIGINT,
    source VARCHAR(100),
    source_file VARCHAR(500),
    imported_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_liability_owner ON liability(owner_id);

CREATE TABLE recurring_obligation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(64) NOT NULL,
    amount_cents BIGINT NOT NULL CHECK (amount_cents >= 0),
    frequency VARCHAR(16) NOT NULL,
    next_due_date DATE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_obligation_owner ON recurring_obligation(owner_id);

CREATE TABLE savings_goal (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    target_cents BIGINT NOT NULL CHECK (target_cents >= 0),
    current_cents BIGINT NOT NULL CHECK (current_cents >= 0),
    deadline DATE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_goal_owner ON savings_goal(owner_id);

CREATE TABLE merchant_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    pattern VARCHAR(500) NOT NULL,
    category VARCHAR(64) NOT NULL,
    account_id UUID REFERENCES account(id) ON DELETE CASCADE,
    kind VARCHAR(40),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_rule_owner ON merchant_rule(owner_id);

CREATE TABLE net_worth_snapshot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    snapshot_date DATE NOT NULL,
    assets_cents BIGINT NOT NULL,
    liabilities_cents BIGINT NOT NULL,
    net_worth_cents BIGINT NOT NULL,
    account_id UUID REFERENCES account(id) ON DELETE SET NULL,
    source VARCHAR(100),
    source_file VARCHAR(500),
    imported_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_snapshot_owner_date ON net_worth_snapshot(owner_id, snapshot_date);
