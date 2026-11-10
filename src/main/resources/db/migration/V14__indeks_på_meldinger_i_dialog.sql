CREATE INDEX dialog_meldinger ON dialog USING GIN ((json -> 'meldinger') jsonb_path_ops);
