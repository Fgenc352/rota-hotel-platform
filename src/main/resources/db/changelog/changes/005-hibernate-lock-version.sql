ALTER TABLE reservation ADD COLUMN lock_version bigint NOT NULL DEFAULT 0;
