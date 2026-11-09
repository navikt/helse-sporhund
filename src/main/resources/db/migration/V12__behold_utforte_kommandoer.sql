ALTER TABLE kommando
    ADD COLUMN utført_tidspunkt TIMESTAMPTZ;

-- Køen leser bare kommandoer som ikke er utført, så indeksene dekker bare dem
DROP INDEX kommando_sekvens;
DROP INDEX kommando_type_conversation_ref_sekvens;
CREATE INDEX kommando_sekvens ON kommando (sekvens) WHERE utført_tidspunkt IS NULL;
CREATE INDEX kommando_type_conversation_ref_sekvens ON kommando (type, conversation_ref, sekvens) WHERE utført_tidspunkt IS NULL;
