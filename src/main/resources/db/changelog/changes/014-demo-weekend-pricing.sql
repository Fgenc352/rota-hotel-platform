-- Demo tariff: Saturday and Sunday nights cost 15% more than the base rate.
-- Only future quotes/bookings use this tariff; historical reservation snapshots stay unchanged.
UPDATE room_type
SET weekend_multiplier = 1.15
WHERE hotel_id IN (
    SELECT id FROM hotel WHERE public_key IN ('lagos-beach', 'peri-stone')
);
