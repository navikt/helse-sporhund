-- Adds kvitteringMottatt: false to all FRA_NAV and FRA_SYSTEM entries in the
-- meldinger array that do not already have the field.
--
-- Before: { "meldinger": [{ "type": "FRA_NAV", ... }, ...] }
-- After:  { "meldinger": [{ "type": "FRA_NAV", ..., "kvitteringMottatt": false }, ...] }

UPDATE dialog
SET json = jsonb_set(
    json,
    '{meldinger}',
    (
        SELECT jsonb_agg(
            CASE
                WHEN (melding ->> 'type') IN ('FRA_NAV', 'FRA_SYSTEM')
                    AND NOT (melding ? 'kvitteringMottatt')
                THEN melding || '{"kvitteringMottatt": false}'::jsonb
                ELSE melding
            END
        )
        FROM jsonb_array_elements(json -> 'meldinger') AS melding
    )
)
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements(json -> 'meldinger') AS melding
    WHERE (melding ->> 'type') IN ('FRA_NAV', 'FRA_SYSTEM')
      AND NOT (melding ? 'kvitteringMottatt')
);

