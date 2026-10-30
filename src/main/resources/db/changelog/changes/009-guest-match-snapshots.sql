-- Old audit rows cannot be reconstructed reliably: version 0 / NULL snapshots are intentional.
ALTER TABLE guest_match_audit ADD COLUMN snapshot_version integer NOT NULL DEFAULT 0;
ALTER TABLE guest_match_audit ADD COLUMN before_state jsonb;
ALTER TABLE guest_match_audit ADD COLUMN after_state jsonb;
ALTER TABLE guest_match_audit ADD CONSTRAINT guest_match_snapshot_complete CHECK (
    (snapshot_version=0 AND before_state IS NULL AND after_state IS NULL)
    OR (snapshot_version>0 AND before_state IS NOT NULL AND after_state IS NOT NULL
        AND jsonb_typeof(before_state)='object' AND jsonb_typeof(after_state)='object')
);
CREATE INDEX guest_match_audit_history ON guest_match_audit(hotel_id,resolved_at DESC);
