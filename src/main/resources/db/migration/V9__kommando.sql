CREATE TABLE kommando(
    id UUID PRIMARY KEY,
    kommando jsonb NOT NULL,
    opprettet TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX kommando_opprettet ON kommando (opprettet);

CREATE TABLE dialogmelding_pdf(
    melding_id UUID PRIMARY KEY,
    pdf bytea NOT NULL,
    opprettet TIMESTAMP NOT NULL DEFAULT now()
);
