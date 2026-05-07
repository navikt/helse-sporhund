CREATE TABLE outbox(
    id UUID PRIMARY KEY,
    event jsonb NOT NULL,
    opprettet timestamp NOT NULL,
    sendt_tidspunkt timestamp
);

CREATE INDEX sendt_tidspunkt_is_null ON outbox (sendt_tidspunkt)
WHERE sendt_tidspunkt IS NULL;
