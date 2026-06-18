-- Backfills søkerFødselsdato on unprocessed OpprettUtgåendeJournalpost outbox messages
-- that were written before fødselsdato was introduced.

UPDATE outbox
SET event = event || '{"søkerFødselsdato": "1970-01-01"}'
WHERE sendt_tidspunkt IS NULL
  AND event ->> 'type' = 'OpprettUtgåendeJournalpost'
  AND event -> 'søkerFødselsdato' IS NULL;

