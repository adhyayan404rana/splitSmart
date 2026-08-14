-- SplitSmart Initial Schema Baseline
-- PostgreSQL Flyway Migration V1

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(50) UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. User Groups Table
CREATE TABLE IF NOT EXISTS user_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Group Members Table (Join Table with Roles)
CREATE TABLE IF NOT EXISTS group_members (
    group_id UUID NOT NULL REFERENCES user_groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, user_id)
);

-- 4. Immutable Event Store (Event Sourcing Core)
CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    version INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_events_aggregate ON events(aggregate_type, aggregate_id);
CREATE INDEX idx_events_created_at ON events(created_at);

-- 5. Expense Drafts Table (Draft State Machine with Optimistic Concurrency Control)
CREATE TABLE IF NOT EXISTS expense_drafts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES user_groups(id) ON DELETE CASCADE,
    payer_id UUID NOT NULL REFERENCES users(id),
    total_amount_cents BIGINT NOT NULL, -- Fowler Money Pattern: stored in lowest currency unit (cents/paise)
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    description TEXT,
    category VARCHAR(100),
    split_logic JSONB NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, COMMITTED, REJECTED
    version INT NOT NULL DEFAULT 1, -- OCC version integer
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_drafts_group ON expense_drafts(group_id);
CREATE INDEX idx_drafts_status ON expense_drafts(status);

-- 6. Materialized Read Model for Current Group Balances (CQRS Read View)
CREATE TABLE IF NOT EXISTS group_balances_view (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES user_groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    net_balance_cents BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_group_user_balance UNIQUE (group_id, user_id)
);

CREATE INDEX idx_balances_group ON group_balances_view(group_id);
