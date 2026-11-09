DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'purring') THEN
        GRANT INSERT ON kommando TO purring;
        GRANT USAGE ON SEQUENCE kommando_sekvens_seq TO purring;
    END IF;
END
$$;
