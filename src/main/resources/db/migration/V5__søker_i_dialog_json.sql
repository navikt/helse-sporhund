-- Migrates existing dialog JSON from a flat søkernavn field
-- to a nested søker object matching the new SøkerDto structure.
-- fødselsdato is set to 1970-01-01 for existing rows where it is unknown.
--
-- Before: { "søkernavn": { "fornavn": "...", ... }, ... }
-- After:  { "søker": { "navn": { "fornavn": "...", ... }, "fødselsdato": "1970-01-01" }, ... }

UPDATE dialog
SET json = (json - 'søkernavn')
    || jsonb_build_object(
        'søker', jsonb_build_object(
            'navn', json -> 'søkernavn',
            'fødselsdato', '1970-01-01'
        )
    )
WHERE json ? 'søkernavn';



