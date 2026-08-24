-- Retter opp i feilaktig verdi for fagområde på OpprettUtgåendeJournalpost outbox-meldinger.
-- Fagområdet UnntakFraArbeidsgiveransvar ble ved en feil serialisert som den lesbare
-- teksten "Unntak fra arbeidsgiveransvar" i stedet for enum-navnet "UnntakFraArbeidsgiveransvar".

UPDATE outbox
SET event = jsonb_set(event, '{fagområde}', '"UnntakFraArbeidsgiveransvar"')
WHERE event ->> 'type' = 'OpprettUtgåendeJournalpost'
  AND event ->> 'fagområde' = 'Unntak fra arbeidsgiveransvar';
