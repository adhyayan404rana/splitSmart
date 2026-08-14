-- SplitSmart Migration V2: Authentication & Row-Level Security (RLS) Baseline

-- 1. Add password_hash to users table
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255) NOT NULL DEFAULT '$2a$12$eImiTXuWVxfM37uY4JANjO5v./QhQxV./5x./5x./5x./5x'; -- Default hashed dummy placeholder

-- 2. Add invite_code to user_groups table
ALTER TABLE user_groups 
ADD COLUMN IF NOT EXISTS invite_code VARCHAR(32) UNIQUE;

-- 3. Refresh Tokens Table (Hashed Opaque Refresh Tokens)
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);

-- 4. Enable Row-Level Security (RLS) on User Groups & Group Members
ALTER TABLE user_groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE group_members ENABLE ROW LEVEL SECURITY;

-- 5. Define RLS Policy for user_groups:
-- Users can only SELECT/QUERY groups where they are an active member, or if no session context is bound
CREATE POLICY user_groups_isolation_policy ON user_groups
    FOR ALL
    USING (
        id IN (
            SELECT group_id FROM group_members WHERE user_id::text = current_setting('app.current_user_id', true)
        )
        OR current_setting('app.current_user_id', true) IS NULL
        OR current_setting('app.current_user_id', true) = ''
    );

-- 6. Define RLS Policy for group_members:
CREATE POLICY group_members_isolation_policy ON group_members
    FOR ALL
    USING (
        group_id IN (
            SELECT group_id FROM group_members WHERE user_id::text = current_setting('app.current_user_id', true)
        )
        OR current_setting('app.current_user_id', true) IS NULL
        OR current_setting('app.current_user_id', true) = ''
    );
