-- Dedicated five-image galleries for the existing five pilot rooms; no common hotel images.

UPDATE room_type SET images='["/images/showcase/lagos-room.webp","/images/showcase/lagos-standard-reverse.webp","/images/showcase/lagos-standard-bathroom.webp","/images/showcase/lagos-standard-living.webp","/images/showcase/lagos-standard-evening.webp"]'::jsonb
WHERE id='20000000-0000-0000-0000-000000000001';

UPDATE room_type SET images='["/images/showcase/lagos-deluxe.webp","/images/showcase/lagos-deluxe-reverse.webp","/images/showcase/lagos-deluxe-bathroom.webp","/images/showcase/lagos-deluxe-living.webp","/images/showcase/lagos-deluxe-evening.webp"]'::jsonb
WHERE id='20000000-0000-0000-0000-000000000002';

UPDATE room_type SET images='["/images/showcase/lagos-family.webp","/images/showcase/lagos-family-reverse.webp","/images/showcase/lagos-family-bathroom.webp","/images/showcase/lagos-family-living.webp","/images/showcase/lagos-family-evening.webp"]'::jsonb
WHERE id='20000000-0000-0000-0000-000000000003';

UPDATE room_type SET images='["/images/showcase/peri-room.webp","/images/showcase/peri-cave-reverse.webp","/images/showcase/peri-cave-bathroom.webp","/images/showcase/peri-cave-living.webp","/images/showcase/peri-cave-evening.webp"]'::jsonb
WHERE id='20000000-0000-0000-0000-000000000004';

UPDATE room_type SET images='["/images/showcase/peri-suite.webp","/images/showcase/peri-terrace-reverse.webp","/images/showcase/peri-terrace-bathroom.webp","/images/showcase/peri-terrace-living.webp","/images/showcase/peri-terrace-evening.webp"]'::jsonb
WHERE id='20000000-0000-0000-0000-000000000005';
