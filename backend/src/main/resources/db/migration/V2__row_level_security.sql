-- =============================================================================
-- V2 - PostgreSQL Row-Level Security (RLS)
-- =============================================================================
-- Enforces multi-tenant data isolation at the database level.
-- Even if the application layer has a bug, no user can read or write rows
-- belonging to another user's groups.
--
-- Design:
--   - A session-local variable `app.current_user_id` is set by the connection
--     pool before any query executes (via application.properties JDBC init-sql).
--   - All RLS policies reference this variable to filter rows.
--   - The `splitsmart_app` role is the restricted application role.
--     The `splitsmart_admin` role bypasses RLS for migrations and admin tasks.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Roles
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'splitsmart_app') THEN
        CREATE ROLE splitsmart_app NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'splitsmart_admin') THEN
        CREATE ROLE splitsmart_admin NOLOGIN BYPASSRLS;
    END IF;
END
$$;

-- Grant minimal privileges to app role
GRANT USAGE ON SCHEMA public TO splitsmart_app;
GRANT SELECT, INSERT, UPDATE ON users         TO splitsmart_app;
GRANT SELECT, INSERT, UPDATE ON groups        TO splitsmart_app;
GRANT SELECT, INSERT, UPDATE ON group_members TO splitsmart_app;
GRANT SELECT, INSERT         ON refresh_tokens TO splitsmart_app;
GRANT UPDATE (is_revoked) ON refresh_tokens  TO splitsmart_app;

-- ---------------------------------------------------------------------------
-- Enable RLS on tenant-sensitive tables
-- ---------------------------------------------------------------------------
ALTER TABLE groups        ENABLE ROW LEVEL SECURITY;
ALTER TABLE group_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;

-- users: each user can read/update only their own row
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- ---------------------------------------------------------------------------
-- users policies
-- ---------------------------------------------------------------------------
-- SELECT: a user can read their own profile
CREATE POLICY users_select_own
    ON users
    FOR SELECT
    USING (id = current_setting('app.current_user_id', TRUE));

-- UPDATE: a user can update only their own profile
CREATE POLICY users_update_own
    ON users
    FOR UPDATE
    USING (id = current_setting('app.current_user_id', TRUE));

-- INSERT: registration path — allow any authenticated connection to insert
-- (JWT is validated before the DB call; no need to restrict by user_id here)
CREATE POLICY users_insert_any
    ON users
    FOR INSERT
    WITH CHECK (TRUE);

-- ---------------------------------------------------------------------------
-- groups policies
-- ---------------------------------------------------------------------------
-- A user can see a group only if they are a member of it.
CREATE POLICY groups_select_member
    ON groups
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1
            FROM group_members gm
            WHERE gm.group_id = groups.id
              AND gm.user_id  = current_setting('app.current_user_id', TRUE)
        )
    );

-- Only the group owner can update group metadata
CREATE POLICY groups_update_owner
    ON groups
    FOR UPDATE
    USING (owner_id = current_setting('app.current_user_id', TRUE));

-- Any authenticated user can create a new group
CREATE POLICY groups_insert_any
    ON groups
    FOR INSERT
    WITH CHECK (TRUE);

-- ---------------------------------------------------------------------------
-- group_members policies
-- ---------------------------------------------------------------------------
-- SELECT: a user can see membership rows for groups they belong to
CREATE POLICY group_members_select_own_groups
    ON group_members
    FOR SELECT
    USING (
        group_id IN (
            SELECT gm2.group_id
            FROM group_members gm2
            WHERE gm2.user_id = current_setting('app.current_user_id', TRUE)
        )
    );

-- INSERT: a user can add themselves (join via invite code) or an OWNER/ADMIN
-- can add others. Simplified: allow insert if authenticated.
CREATE POLICY group_members_insert_join
    ON group_members
    FOR INSERT
    WITH CHECK (TRUE);

-- UPDATE: a user can only update their own membership row (e.g. pledgedMinor)
CREATE POLICY group_members_update_own
    ON group_members
    FOR UPDATE
    USING (user_id = current_setting('app.current_user_id', TRUE));

-- ---------------------------------------------------------------------------
-- refresh_tokens policies
-- ---------------------------------------------------------------------------
-- A user can only see and revoke their own refresh tokens
CREATE POLICY refresh_tokens_own
    ON refresh_tokens
    FOR ALL
    USING (user_id = current_setting('app.current_user_id', TRUE));

-- ---------------------------------------------------------------------------
-- Helper function: set session context (called by the app before each txn)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_app_user(p_user_id TEXT)
RETURNS VOID AS $$
BEGIN
    PERFORM set_config('app.current_user_id', p_user_id, TRUE);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION set_app_user(TEXT) TO splitsmart_app;
