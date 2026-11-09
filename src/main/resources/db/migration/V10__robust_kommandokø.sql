ALTER TABLE kommando
    ADD COLUMN type TEXT,
    ADD COLUMN conversation_ref UUID,
    ADD COLUMN sekvens BIGSERIAL;

UPDATE kommando
SET type             = kommando ->> 'type',
    conversation_ref = (kommando ->> 'conversationRef')::uuid;

ALTER TABLE kommando
    ALTER COLUMN type SET NOT NULL,
    ALTER COLUMN conversation_ref SET NOT NULL;

DROP INDEX kommando_opprettet;
CREATE INDEX kommando_sekvens ON kommando (sekvens);
CREATE INDEX kommando_type_conversation_ref_sekvens ON kommando (type, conversation_ref, sekvens);

ALTER TABLE kommando
    ADD COLUMN antall_forsøk INT NOT NULL DEFAULT 0,
    ADD COLUMN neste_forsøk TIMESTAMPTZ,
    ADD COLUMN siste_feil TEXT;
