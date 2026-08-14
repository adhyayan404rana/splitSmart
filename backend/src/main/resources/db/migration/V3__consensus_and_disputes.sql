-- SplitSmart Migration V3: Consensus Approvals, Disputes & Deduplication Baseline

-- 1. Add dispute & frozen settlement flags to expense_drafts
ALTER TABLE expense_drafts
ADD COLUMN IF NOT EXISTS is_disputed BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS dispute_reason TEXT;

-- 2. Participant Approvals Table (Multi-User Consensus Tracking)
CREATE TABLE IF NOT EXISTS participant_approvals (
    draft_id UUID NOT NULL REFERENCES expense_drafts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_role VARCHAR(50) NOT NULL DEFAULT 'DEBTOR', -- PAYER, DEBTOR
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (draft_id, user_id)
);

CREATE INDEX idx_approvals_draft ON participant_approvals(draft_id);
