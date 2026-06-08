-- V4 silently skipped these grants because the 'purring' role did not yet exist
-- when that migration ran (Sqeletor creates the role after the main app starts).
-- This migration retries the grants unconditionally.
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'purring') THEN
        GRANT SELECT, INSERT, UPDATE ON dialog TO purring;
        GRANT SELECT, INSERT ON outbox TO purring;
        GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO purring;
    END IF;
END
$$;

