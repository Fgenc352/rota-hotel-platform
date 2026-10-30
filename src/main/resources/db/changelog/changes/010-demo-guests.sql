-- Local case-study fixture. Explicit @demo context prevents accidental execution in tests.
-- UUID namespaces d1/d2/d3/d4 are reserved for these demo records; existing user rows are not updated.
CREATE TEMPORARY TABLE demo_guest_seed ON COMMIT DROP AS
SELECT n,
    ((n-1)%25)+1 AS local_number,
    ('d1000000-0000-0000-0000-'||lpad(n::text,12,'0'))::uuid AS id,
    CASE WHEN n<=25 THEN '10000000-0000-0000-0000-000000000001'::uuid
        ELSE '10000000-0000-0000-0000-000000000002'::uuid END AS hotel_id,
    CASE WHEN n<=25 THEN '20000000-0000-0000-0000-000000000001'::uuid
        ELSE '20000000-0000-0000-0000-000000000004'::uuid END AS room_type_id,
    (ARRAY['Ali','Veli','Ali','Ayşe','Fatma','Mehmet','Zeynep','Mustafa','Elif','Ahmet',
        'Deniz','Emre','Selin','Murat','Derya','Can','Ece','Burak','İrem','Onur',
        'Ceren','Oğuz','Buse','Kerem','Aslı'])[((n-1)%25)+1] AS first_name,
    (ARRAY['Yılmaz','Kaya','Yılmaz','Demir','Çelik','Şahin','Aydın','Arslan','Koç','Yıldız',
        'Yılmaz','Kaya','Demir','Çelik','Şahin','Aydın','Arslan','Koç','Yıldız','Aksoy',
        'Öztürk','Güneş','Polat','Kılıç','Tekin'])[((n-1)%25)+1] AS last_name,
    'test'||lpad((CASE WHEN n IN (3,28) THEN n-2 ELSE n END)::text,2,'0')||'@example.com' AS email,
    '+90555'||lpad((CASE WHEN n IN (3,28) THEN n-1 ELSE n END)::text,7,'0') AS phone,
    n%3=0 AS marketing_allowed,
    n IN (3,28) AS matching_conflict
FROM generate_series(1,50) AS series(n);

INSERT INTO guest(id,hotel_id,first_name,last_name,email,phone,marketing_allowed,consent_at,matching_conflict)
SELECT id,hotel_id,first_name,last_name,email,phone,marketing_allowed,now()-interval '150 days',matching_conflict
FROM demo_guest_seed ON CONFLICT DO NOTHING;

INSERT INTO guest_identifier(hotel_id,email,phone,guest_id)
SELECT hotel_id,email,phone,id FROM demo_guest_seed ON CONFLICT DO NOTHING;

-- Ten guests have an additional completed stay, so returning-guest segments are populated.
CREATE TEMPORARY TABLE demo_booking_seed ON COMMIT DROP AS
SELECT ('d'||CASE WHEN repeat.visit=0 THEN '2' ELSE '3' END||'000000-0000-0000-0000-'||lpad(g.n::text,12,'0'))::uuid AS id,
    g.id AS guest_id,g.hotel_id,g.room_type_id,g.first_name,g.last_name,g.email,g.phone,g.marketing_allowed,
    'RT-DEMO-'||lpad(g.n::text,2,'0')||'-'||repeat.visit AS code,
    dates.check_in,dates.check_in+2 AS check_out,
    CASE WHEN repeat.visit=1 THEN 'CHECKED_OUT'
        WHEN g.local_number%10=0 THEN 'CONFIRMED'
        WHEN g.local_number%10=5 THEN 'CANCELLED'
        ELSE 'CHECKED_OUT' END AS status,
    room.nightly_price,room.nightly_price*2 AS total
FROM demo_guest_seed g
JOIN room_type room ON room.hotel_id=g.hotel_id AND room.id=g.room_type_id
CROSS JOIN LATERAL (SELECT 0 AS visit UNION ALL SELECT 1 WHERE g.local_number%5=1) repeat
CROSS JOIN LATERAL (
    SELECT (now() AT TIME ZONE 'Europe/Istanbul')::date
        + CASE WHEN repeat.visit=1 THEN -110-g.local_number*3
            WHEN g.local_number%10=0 THEN g.local_number
            ELSE -7-g.local_number*3 END AS check_in
) dates;

-- Only newly inserted fixture reservations contribute to inventory, even if the SQL is replayed manually.
DELETE FROM demo_booking_seed b USING reservation r WHERE r.id=b.id;

INSERT INTO reservation(id,hotel_id,room_type_id,code,check_in,check_out,adults,children,
    first_name,last_name,email,phone,privacy_accepted,marketing_allowed,consent_version,consent_at,
    status,total,currency,version,created_at,checked_out_at)
SELECT id,hotel_id,room_type_id,code,check_in,check_out,2,0,first_name,last_name,email,phone,
    true,marketing_allowed,'demo-v1',now()-interval '150 days',status,total,'TRY',1,
    least(now(),(check_in-14)::timestamp AT TIME ZONE 'Europe/Istanbul'),
    CASE WHEN status='CHECKED_OUT' THEN (check_out+time '11:00') AT TIME ZONE 'Europe/Istanbul' END
FROM demo_booking_seed;

INSERT INTO reservation_night(hotel_id,reservation_id,stay_date,price)
SELECT b.hotel_id,b.id,night.day::date,b.nightly_price
FROM demo_booking_seed b
CROSS JOIN LATERAL generate_series(b.check_in::timestamp,(b.check_out-1)::timestamp,interval '1 day') night(day);

INSERT INTO room_inventory(hotel_id,room_type_id,stay_date,capacity,sold)
SELECT b.hotel_id,b.room_type_id,night.day::date,room.rooms,count(*)::integer
FROM demo_booking_seed b
JOIN room_type room ON room.hotel_id=b.hotel_id AND room.id=b.room_type_id
CROSS JOIN LATERAL generate_series(b.check_in::timestamp,(b.check_out-1)::timestamp,interval '1 day') night(day)
WHERE b.status<>'CANCELLED'
GROUP BY b.hotel_id,b.room_type_id,night.day::date,room.rooms
ON CONFLICT(hotel_id,room_type_id,stay_date) DO UPDATE
SET sold=room_inventory.sold+excluded.sold;

-- Fixture reservations are already projected. No outbox delivery is created, so migration sends no PMS requests.
INSERT INTO crm_reservation(hotel_id,reservation_id,guest_id,code,check_in,check_out,status,total,version,checked_out_at)
SELECT hotel_id,id,guest_id,code,check_in,check_out,status,total,1,
    CASE WHEN status='CHECKED_OUT' THEN (check_out+time '11:00') AT TIME ZONE 'Europe/Istanbul' END
FROM demo_booking_seed;

INSERT INTO guest_note(id,hotel_id,guest_id,body,author,created_at)
SELECT ('d4000000-0000-0000-0000-'||lpad(n::text,12,'0'))::uuid,hotel_id,id,
    'Demo misafir notu '||lpad(n::text,2,'0'),CASE WHEN n<=25 THEN 'lagos' ELSE 'peri' END,now()
FROM demo_guest_seed ON CONFLICT DO NOTHING;

INSERT INTO guest_tag(hotel_id,guest_id,tag)
SELECT hotel_id,id,'DEMO' FROM demo_guest_seed ON CONFLICT DO NOTHING;
INSERT INTO guest_tag(hotel_id,guest_id,tag)
SELECT hotel_id,id,'VIP' FROM demo_guest_seed WHERE local_number%5=1 ON CONFLICT DO NOTHING;
