CREATE TABLE dialog(
    conversation_ref UUID NOT NULL PRIMARY KEY,
    identitetsnummer VARCHAR(255) NOT NULL,
    dialog_opprettet TIMESTAMP NOT NULL default now(),
    json jsonb NOT NULL
);
