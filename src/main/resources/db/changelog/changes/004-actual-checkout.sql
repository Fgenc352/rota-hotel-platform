ALTER TABLE reservation ADD COLUMN checked_out_at timestamptz;
ALTER TABLE crm_reservation ADD COLUMN checked_out_at timestamptz;
CREATE INDEX crm_completed_stays ON crm_reservation(hotel_id,guest_id,checked_out_at) WHERE status='CHECKED_OUT';
