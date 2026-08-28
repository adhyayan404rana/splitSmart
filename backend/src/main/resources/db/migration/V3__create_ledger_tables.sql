-- =============================================================================
-- V3 - Create ledger tables
-- =============================================================================
-- Append-only event store, consensus draft workspace, and CQRS read-side
-- materialized balance view for the SplitSmart ledger domain.
--
-- Design principles:
--   - events:         immutable; never updated or deleted after INSERT.
--   - drafts:         mutable until finalized; status-machine enforced by app.
--   - group_balances: materialized view updated incrementally by the
--                     LedgerProjectionWorker on each DraftApproved /
--                     SettlementRecorded event.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- events (append-only event store)
-- ---------------------------------------------------------------------------
-- Each row is a single, immutable domain event scoped to a group.
-- The (group_id, version) pair is unique, enforcing OCC at the DB level.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS events (
    id              VARCHAR(64)  PRIMARY KEY,
    group_id        VARCHAR(64)  NOT NULL REFERENCES groups(id) ON DELETE CASCADE,

    -- Monotonically increasing per-group sequence number (starts at 1)
    version         BIGINT       NOT NULL,

    -- Discriminator: DraftCreated | DraftApproved | DraftModified |
    --                DraftDisputed | DraftFinalized | SettlementRecorded |
    --                MemberJoined | ConflictResolved
    event_type      VARCHAR(80)  NOT NULL,

    -- User who triggered the event; NULL for system-generated events
    actor_id        VARCHAR(64)  REFERENCES users(id) ON DELETE SET NULL,

    -- Full event payload serialized as JSONB
    payload         JSONB        NOT NULL DEFAULT '{}',

    -- Wall-clock timestamp assigned by the database at insert time
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- Correlation ID from the inbound webhook envelope (for idempotency)
    correlation_id  VARCHAR(64),

    -- OCC constraint: no two events in the same group may share a version
    CONSTRAINT uq_events_group_version UNIQUE (group_id, version)
);

-- Immutability: prevent UPDATE and DELETE on events
CREATE RULE events_no_update AS ON UPDATE TO events DO INSTEAD NOTHING;
CREATE RULE events_no_delete AS ON DELETE TO events DO INSTEAD NOTHING;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_events_group_version
    ON events (group_id, version ASC);

CREATE INDEX IF NOT EXISTS idx_events_group_created
    ON events (group_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_events_correlation
    ON events (correlation_id)
    WHERE correlation_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_events_type
    ON events (event_type);

-- RLS
ALTER TABLE events ENABLE ROW LEVEL SECURITY;

CREATE POLICY events_select_group_member
    ON events FOR SELECT
    USING (
        group_id IN (
            SELECT gm.group_id FROM group_members gm
            WHERE gm.user_id = current_setting('app.current_user_id', TRUE)
        )
    );

CREATE POLICY events_insert_group_member
    ON events FOR INSERT
    WITH CHECK (TRUE);   -- actor validation handled at application layer

GRANT SELECT, INSERT ON events TO splitsmart_app;

-- ---------------------------------------------------------------------------
-- drafts (consensus workspace)
-- ---------------------------------------------------------------------------
-- Short-lived mutable records for expense proposals awaiting approval quorum.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS drafts (
    id                  VARCHAR(64)  PRIMARY KEY,
    group_id            VARCHAR(64)  NOT NULL REFERENCES groups(id) ON DELETE CASCADE,

    -- Link to the event that created this draft
    origin_event_id     VARCHAR(64)  REFERENCES events(id) ON DELETE SET NULL,

    -- Idempotency key from inbound webhook
    correlation_id      VARCHAR(64),

    -- ── Expense fields ──────────────────────────────────────────────────────
    description         VARCHAR(255) NOT NULL,
    amount_minor        BIGINT       NOT NULL CHECK (amount_minor > 0),
    currency            CHAR(3)      NOT NULL DEFAULT 'INR',
    payer_identifier    VARCHAR(120) NOT NULL,
    split_type          VARCHAR(20)  NOT NULL DEFAULT 'EQUAL'
                        CHECK (split_type IN ('EQUAL', 'EXACT', 'PERCENTAGE')),
    category            VARCHAR(40)  DEFAULT 'Bills',

    -- Actual real-world transaction date (distinct from created_at)
    transaction_date    DATE,

    -- Comma-separated participant identifiers (denormalized for fast reads)
    participants        TEXT,

    -- ── Consensus tracking ──────────────────────────────────────────────────
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','APPROVED','DISPUTED','EXPIRED','FINALIZED')),
    approval_count      INT          NOT NULL DEFAULT 0,
    required_approvals  INT          NOT NULL DEFAULT 2,

    -- Comma-separated user IDs who have approved
    approved_by         TEXT,

    -- Dispute info
    disputed_by         VARCHAR(64)  REFERENCES users(id) ON DELETE SET NULL,
    dispute_reason      VARCHAR(500),

    -- ── Provenance ──────────────────────────────────────────────────────────
    extraction_source   VARCHAR(30),
    extraction_tier     SMALLINT     CHECK (extraction_tier BETWEEN 1 AND 3),
    confidence          SMALLINT     CHECK (confidence BETWEEN 0 AND 100),

    -- ── Timestamps ──────────────────────────────────────────────────────────
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMPTZ
);

CREATE TRIGGER drafts_set_updated_at
    BEFORE UPDATE ON drafts
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

-- Indexes
CREATE INDEX IF NOT EXISTS idx_drafts_group_status
    ON drafts (group_id, status);

CREATE INDEX IF NOT EXISTS idx_drafts_group_created
    ON drafts (group_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_drafts_correlation
    ON drafts (correlation_id)
    WHERE correlation_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_drafts_expires
    ON drafts (expires_at)
    WHERE status = 'PENDING' AND expires_at IS NOT NULL;

-- RLS
ALTER TABLE drafts ENABLE ROW LEVEL SECURITY;

CREATE POLICY drafts_select_group_member
    ON drafts FOR SELECT
    USING (
        group_id IN (
            SELECT gm.group_id FROM group_members gm
            WHERE gm.user_id = current_setting('app.current_user_id', TRUE)
        )
    );

CREATE POLICY drafts_insert_group_member
    ON drafts FOR INSERT
    WITH CHECK (TRUE);

CREATE POLICY drafts_update_group_member
    ON drafts FOR UPDATE
    USING (
        group_id IN (
            SELECT gm.group_id FROM group_members gm
            WHERE gm.user_id = current_setting('app.current_user_id', TRUE)
        )
    );

GRANT SELECT, INSERT, UPDATE ON drafts TO splitsmart_app;

-- ---------------------------------------------------------------------------
-- group_balances (CQRS materialized view)
-- ---------------------------------------------------------------------------
-- Pre-computed per-member net balance within each group.
-- Updated incrementally by LedgerProjectionWorker.
-- (group_id, member_id) is unique — one row per member per group.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS group_balances (
    id                  VARCHAR(64)  PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    group_id            VARCHAR(64)  NOT NULL REFERENCES groups(id) ON DELETE CASCADE,

    -- May be a user ID or a display name for non-registered participants
    member_id           VARCHAR(120) NOT NULL,

    -- Denormalized display name for fast read
    member_name         VARCHAR(120),

    -- Cumulative paid / owed amounts (major units with 2 d.p.)
    total_paid          NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_owed          NUMERIC(15,2) NOT NULL DEFAULT 0.00,

    -- net_balance = total_paid - total_owed
    -- Positive: member is owed; Negative: member owes
    net_balance         NUMERIC(15,2) NOT NULL DEFAULT 0.00,

    currency            CHAR(3)       NOT NULL DEFAULT 'INR',

    -- Last event version reflected in this projection row
    last_event_version  BIGINT        NOT NULL DEFAULT 0,

    draft_count         INT           NOT NULL DEFAULT 0,

    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_group_balances_group_member UNIQUE (group_id, member_id)
);

CREATE TRIGGER group_balances_set_updated_at
    BEFORE UPDATE ON group_balances
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

-- Indexes
CREATE INDEX IF NOT EXISTS idx_group_balances_group
    ON group_balances (group_id);

CREATE INDEX IF NOT EXISTS idx_group_balances_member
    ON group_balances (member_id);

CREATE INDEX IF NOT EXISTS idx_group_balances_net
    ON group_balances (group_id, net_balance DESC);

-- RLS
ALTER TABLE group_balances ENABLE ROW LEVEL SECURITY;

CREATE POLICY group_balances_select_member
    ON group_balances FOR SELECT
    USING (
        group_id IN (
            SELECT gm.group_id FROM group_members gm
            WHERE gm.user_id = current_setting('app.current_user_id', TRUE)
        )
    );

CREATE POLICY group_balances_upsert_member
    ON group_balances FOR ALL
    WITH CHECK (TRUE);   -- projection worker runs as admin role

GRANT SELECT, INSERT, UPDATE ON group_balances TO splitsmart_app;
