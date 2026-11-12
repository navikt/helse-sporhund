-- Kotliquery ber om genererte nøkler, og da legger Postgres-driveren RETURNING * på INSERT. Det krever SELECT.
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'purring') THEN
        GRANT SELECT ON kommando TO purring;
    END IF;
END
$$;
