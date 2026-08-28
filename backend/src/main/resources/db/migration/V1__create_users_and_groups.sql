-- =============================================================================
-- V1 - Create users and groups tables
-- =============================================================================
-- SplitSmart baseline schema: authentication, group management, and member
-- relationships. All monetary values are stored as INTEGER minor units (paise)
-- to avoid floating-point rounding errors.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Extensions
-- ---------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "citext";     -- case-insensitive email comparison

-- ---------------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------------
-- Stores registered SplitSmart users. Passwords are stored as BCrypt hashes;
-- the raw password is never persisted.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id               VARCHAR(64)  PRIMARY KEY,
    email            CITEXT       NOT NULL,
    name             VARCHAR(120) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,

    -- UPI virtual payment address (e.g. "rahul@gpay")
    upi_vpa          VARCHAR(64),

    -- Profile metadata
    avatar_url       VARCHAR(512),
    phone            VARCHAR(20),
    preferred_currency CHAR(3)    NOT NULL DEFAULT 'INR',

    -- Soft-delete and audit
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_login_at    TIMESTAMPTZ,

    CONSTRAINT uq_users_email UNIQUE (email)
);

-- Trigger: auto-update updated_at on every row change
CREATE OR REPLACE FUNCTION trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_set_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

-- Indexes
CREATE INDEX IF NOT EXISTS idx_users_email      ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_active     ON users (is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users (created_at DESC);

-- ---------------------------------------------------------------------------
-- groups
-- ---------------------------------------------------------------------------
-- A group represents a shared expense pool (e.g. a trip, a flat, a team).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS groups (
    id                  VARCHAR(64)  PRIMARY KEY,
    name                VARCHAR(120) NOT NULL,
    emoji               VARCHAR(8)   NOT NULL DEFAULT '🏝️',
    description         VARCHAR(500),

    -- 8-character uppercase invite code (e.g. "VZAWWNMR")
    invite_code         VARCHAR(16)  NOT NULL,

    -- Budget goal in paise (minor units)
    budget_goal_minor   BIGINT       NOT NULL DEFAULT 0,

    -- ISO-4217 currency for the group
    currency            CHAR(3)      NOT NULL DEFAULT 'INR',

    -- Group creator
    owner_id            VARCHAR(64)  REFERENCES users(id) ON DELETE SET NULL,

    -- Soft-delete and audit
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_groups_invite_code UNIQUE (invite_code)
);

CREATE TRIGGER groups_set_updated_at
    BEFORE UPDATE ON groups
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

CREATE INDEX IF NOT EXISTS idx_groups_invite_code ON groups (invite_code);
CREATE INDEX IF NOT EXISTS idx_groups_owner       ON groups (owner_id);
CREATE INDEX IF NOT EXISTS idx_groups_active      ON groups (is_active) WHERE is_active = TRUE;

-- ---------------------------------------------------------------------------
-- group_members
-- ---------------------------------------------------------------------------
-- Many-to-many relationship between users and groups.
-- Tracks per-member pledged amount and payment role.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS group_members (
    id                  VARCHAR(64)  PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    group_id            VARCHAR(64)  NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id             VARCHAR(64)  NOT NULL REFERENCES users(id)  ON DELETE CASCADE,

    -- Role within the group: OWNER, ADMIN, or MEMBER
    role                VARCHAR(20)  NOT NULL DEFAULT 'MEMBER'
                        CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),

    -- Pledged contribution in paise
    pledged_minor       BIGINT       NOT NULL DEFAULT 0,

    -- Amount actually paid in paise (updated when settlements recorded)
    paid_minor          BIGINT       NOT NULL DEFAULT 0,

    joined_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_group_members_pair UNIQUE (group_id, user_id)
);

CREATE TRIGGER group_members_set_updated_at
    BEFORE UPDATE ON group_members
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

CREATE INDEX IF NOT EXISTS idx_group_members_group ON group_members (group_id);
CREATE INDEX IF NOT EXISTS idx_group_members_user  ON group_members (user_id);

-- ---------------------------------------------------------------------------
-- refresh_tokens
-- ---------------------------------------------------------------------------
-- Stores JWT refresh tokens for secure token rotation.
-- Expired and revoked tokens are cleaned up by a scheduled job.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id           VARCHAR(64)  PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    user_id      VARCHAR(64)  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash   VARCHAR(255) NOT NULL,
    device_info  VARCHAR(255),
    is_revoked   BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at   TIMESTAMPTZ  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user      ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires   ON refresh_tokens (expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_active    ON refresh_tokens (is_revoked, expires_at)
    WHERE is_revoked = FALSE;
