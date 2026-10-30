ALTER TABLE room_type ADD COLUMN description varchar(2000) NOT NULL DEFAULT '';
ALTER TABLE room_type ADD COLUMN area_square_meters integer NOT NULL DEFAULT 0 CHECK(area_square_meters>=0);
ALTER TABLE room_type ADD COLUMN bed_description varchar(160) NOT NULL DEFAULT '';
ALTER TABLE room_type ADD COLUMN amenities jsonb NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE room_type ADD COLUMN images jsonb NOT NULL DEFAULT '[]'::jsonb;

-- Fictional presentation content for the existing five pilot room types. Prices/capacity are unchanged.
UPDATE room_type SET description='Yumuşak tonlar, doğal dokular ve dinlenmek için özenle tasarlanmış bir alan. Akdeniz günlerine sakin bir başlangıç.',
    area_square_meters=28,bed_description='1 çift kişilik yatak',
    amenities='["Klima","Wi-Fi","Özel banyo","Minibar","Çalışma alanı"]'::jsonb,
    images='["/images/showcase/lagos-room.webp","/images/showcase/lagos-hero.webp"]'::jsonb
WHERE id='20000000-0000-0000-0000-000000000001';
UPDATE room_type SET description='Denizden ilham alan ferah bir konaklama. Geniş yaşam alanı ve gün ışığıyla dolan bir oda.',
    area_square_meters=38,bed_description='1 çift kişilik yatak + 1 açılır yatak',
    amenities='["Deniz manzarası","Balkon","Klima","Wi-Fi","Minibar","Kahve köşesi"]'::jsonb,
    images='["/images/showcase/lagos-deluxe.webp","/images/showcase/lagos-hero.webp"]'::jsonb
WHERE id='20000000-0000-0000-0000-000000000002';
UPDATE room_type SET description='Birlikte geçirilen anlara daha fazla yer açın. Aile konaklamaları için geniş ve kullanışlı yaşam alanı.',
    area_square_meters=56,bed_description='1 çift kişilik yatak + 2 tek kişilik yatak',
    amenities='["Aile yaşam alanı","Balkon","Klima","Wi-Fi","Özel banyo","Kahve köşesi"]'::jsonb,
    images='["/images/showcase/lagos-family.webp","/images/showcase/lagos-hero.webp"]'::jsonb
WHERE id='20000000-0000-0000-0000-000000000003';
UPDATE room_type SET description='Kapadokya taşının doğal dokusuyla çevrili, kendine özgü bir sığınak. Sıcak ışık, kemerli detaylar ve derin bir sessizlik.',
    area_square_meters=32,bed_description='1 çift kişilik yatak',
    amenities='["Doğal taş mimari","Isıtma","Wi-Fi","Özel banyo","Çay ve kahve"]'::jsonb,
    images='["/images/showcase/peri-room.webp","/images/showcase/peri-hero.webp"]'::jsonb
WHERE id='20000000-0000-0000-0000-000000000004';
UPDATE room_type SET description='Vadinin ışığına açılan özel bir teras. Taş mimarisini geniş bir yaşam alanıyla buluşturan bir Kapadokya deneyimi.',
    area_square_meters=48,bed_description='1 çift kişilik yatak + 1 açılır yatak',
    amenities='["Özel teras","Vadi manzarası","Oturma alanı","Isıtma","Wi-Fi","Kahve köşesi"]'::jsonb,
    images='["/images/showcase/peri-suite.webp","/images/showcase/peri-hero.webp"]'::jsonb
WHERE id='20000000-0000-0000-0000-000000000005';
