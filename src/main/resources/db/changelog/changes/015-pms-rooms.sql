CREATE TABLE pms_room (
 id uuid PRIMARY KEY, hotel_id uuid NOT NULL REFERENCES hotel(id), room_type_id uuid NOT NULL,
 number varchar(20) NOT NULL, housekeeping varchar(20) NOT NULL DEFAULT 'CLEAN'
 CHECK (housekeeping IN ('CLEAN','DIRTY','INSPECTING')),
 lock_version bigint NOT NULL DEFAULT 0,
 UNIQUE (hotel_id,id), UNIQUE(hotel_id,number),
 FOREIGN KEY(hotel_id,room_type_id) REFERENCES room_type(hotel_id,id)
);
CREATE TABLE pms_room_assignment (
 reservation_id uuid PRIMARY KEY, hotel_id uuid NOT NULL, room_id uuid NOT NULL,
 code varchar(30) NOT NULL, check_in date NOT NULL, check_out date NOT NULL,
 status varchar(20) NOT NULL, lock_version bigint NOT NULL DEFAULT 0,
 FOREIGN KEY(hotel_id,reservation_id) REFERENCES reservation(hotel_id,id),
 FOREIGN KEY(hotel_id,room_id) REFERENCES pms_room(hotel_id,id),
 CHECK(check_out > check_in),
 CHECK(status IN ('PENDING','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED','NO_SHOW'))
);
CREATE INDEX pms_assignment_room_dates ON pms_room_assignment(hotel_id,room_id,check_in,check_out);
CREATE TABLE pms_room_audit (
 id uuid PRIMARY KEY, hotel_id uuid NOT NULL REFERENCES hotel(id), room_id uuid NOT NULL,
 reservation_id uuid, action varchar(40) NOT NULL, actor varchar(120) NOT NULL,
 before_state jsonb NOT NULL, after_state jsonb NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
 FOREIGN KEY(hotel_id,room_id) REFERENCES pms_room(hotel_id,id),
 FOREIGN KEY(hotel_id,reservation_id) REFERENCES reservation(hotel_id,id)
);
CREATE INDEX pms_audit_hotel_date ON pms_room_audit(hotel_id,created_at DESC);

-- Physical room numbers belong to the existing room types. No additional sellable stock is created.
INSERT INTO pms_room(id,hotel_id,room_type_id,number)
SELECT md5(t.hotel_id::text || ':' || t.id::text || ':' || n::text)::uuid,
 t.hotel_id,t.id,(t.type_number*100+n)::text
FROM (
 SELECT r.*,row_number() OVER(PARTITION BY hotel_id ORDER BY id) AS type_number FROM room_type r
) t CROSS JOIN LATERAL generate_series(1,t.rooms) n;
