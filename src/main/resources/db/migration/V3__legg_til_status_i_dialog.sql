UPDATE dialog SET json = jsonb_set(json, '{status}', '"ForespørselSendt"') WHERE json->>'status' IS NULL;

