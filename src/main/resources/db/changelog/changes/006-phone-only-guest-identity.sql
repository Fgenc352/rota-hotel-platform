-- Only the derived lookup index changes; guest/profile/history records are preserved.
DELETE FROM guest_identifier WHERE kind <> 'PHONE';

-- Previous email matches could register secondary phone numbers against the original profile.
DELETE FROM guest_identifier i
USING guest g
WHERE i.hotel_id=g.hotel_id AND i.guest_id=g.id AND i.kind='PHONE' AND i.value<>g.phone;

-- Preserve valid canonical mappings; legacy conflict profiles with the same phone are not merged.
INSERT INTO guest_identifier(hotel_id,kind,value,guest_id)
SELECT DISTINCT ON (hotel_id,phone) hotel_id,'PHONE',phone,id
FROM guest
ORDER BY hotel_id,phone,matching_conflict,id
ON CONFLICT (hotel_id,kind,value) DO NOTHING;
