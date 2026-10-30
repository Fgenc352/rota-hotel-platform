CREATE TABLE hotel_service (
    id uuid PRIMARY KEY,
    hotel_id uuid NOT NULL REFERENCES hotel(id),
    code varchar(40) NOT NULL,
    name varchar(120) NOT NULL,
    description varchar(500) NOT NULL,
    price numeric(14,2) NOT NULL CHECK(price>=0),
    unit varchar(30) NOT NULL CHECK(unit IN ('PER_STAY','PER_NIGHT','PER_PERSON_PER_NIGHT')),
    included boolean NOT NULL DEFAULT false,
    active boolean NOT NULL DEFAULT true,
    UNIQUE(hotel_id,code),
    CHECK(NOT included OR price=0)
);
ALTER TABLE reservation ADD COLUMN room_total numeric(14,2);
UPDATE reservation SET room_total=total;
ALTER TABLE reservation ALTER COLUMN room_total SET NOT NULL;
ALTER TABLE reservation ADD COLUMN service_lines jsonb NOT NULL DEFAULT '[]'::jsonb;

-- Case study service plans; the selected plan is snapshotted on new reservations only.
INSERT INTO hotel_service(id,hotel_id,code,name,description,price,unit,included)
SELECT md5(h.id::text||s.code)::uuid,h.id,s.code,s.name,s.description,s.price,s.unit,s.included
FROM hotel h CROSS JOIN (VALUES
    ('BREAKFAST','Sabah kahvaltısı','Her konaklama gecesi için tüm misafirlere kahvaltı dahildir.',0::numeric,'PER_PERSON_PER_NIGHT',true),
    ('WIFI','Wi-Fi','Oda ve ortak alanlarda internet erişimi dahildir.',0::numeric,'PER_STAY',true),
    ('LUNCH','Öğle yemeği','Yetişkin ve çocuk başına, konaklama gecesi adedince ücretlendirilir.',650::numeric,'PER_PERSON_PER_NIGHT',false),
    ('DINNER','Akşam yemeği','Yetişkin ve çocuk başına, konaklama gecesi adedince ücretlendirilir.',950::numeric,'PER_PERSON_PER_NIGHT',false),
    ('VALET','Vale ve otopark','Bir araç için konaklama gecesi başına ücretlendirilir.',300::numeric,'PER_NIGHT',false),
    ('TRANSFER','Havalimanı transferi','Rezervasyon başına tek yön özel araç transferidir.',1800::numeric,'PER_STAY',false)
) AS s(code,name,description,price,unit,included);
