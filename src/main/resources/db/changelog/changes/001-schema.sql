CREATE TABLE hotel (
 id uuid PRIMARY KEY, public_key varchar(80) NOT NULL UNIQUE, name varchar(160) NOT NULL,
 location varchar(100) NOT NULL, timezone varchar(50) NOT NULL DEFAULT 'Europe/Istanbul'
);
CREATE TABLE hotel_origin (
 hotel_id uuid NOT NULL REFERENCES hotel, origin varchar(250) NOT NULL,
 PRIMARY KEY(hotel_id, origin)
);
CREATE TABLE staff_user (
 username varchar(120) PRIMARY KEY, password_hash varchar(100) NOT NULL,
 hotel_id uuid NOT NULL REFERENCES hotel
);
CREATE TABLE room_type (
 id uuid PRIMARY KEY, hotel_id uuid NOT NULL REFERENCES hotel, name varchar(160) NOT NULL,
 capacity integer NOT NULL CHECK(capacity > 0), rooms integer NOT NULL CHECK(rooms > 0),
 nightly_price numeric(12,2) NOT NULL CHECK(nightly_price > 0),
 weekend_multiplier numeric(5,2) NOT NULL DEFAULT 1 CHECK(weekend_multiplier >= 1),
 min_nights integer NOT NULL DEFAULT 1 CHECK(min_nights > 0), UNIQUE(hotel_id,id)
);
CREATE TABLE room_inventory (
 hotel_id uuid NOT NULL, room_type_id uuid NOT NULL, stay_date date NOT NULL,
 capacity integer NOT NULL CHECK(capacity > 0), sold integer NOT NULL DEFAULT 0,
 PRIMARY KEY(hotel_id,room_type_id,stay_date),
 FOREIGN KEY(hotel_id,room_type_id) REFERENCES room_type(hotel_id,id),
 CHECK(sold >= 0 AND sold <= capacity)
);
CREATE TABLE reservation (
 id uuid PRIMARY KEY, hotel_id uuid NOT NULL, room_type_id uuid NOT NULL,
 code varchar(30) NOT NULL UNIQUE, check_in date NOT NULL, check_out date NOT NULL,
 adults integer NOT NULL CHECK(adults > 0), children integer NOT NULL CHECK(children >= 0),
 first_name varchar(100) NOT NULL, last_name varchar(100) NOT NULL,
 email varchar(254) NOT NULL, phone varchar(20) NOT NULL,
 privacy_accepted boolean NOT NULL CHECK(privacy_accepted), marketing_allowed boolean NOT NULL,
 consent_version varchar(30) NOT NULL, consent_at timestamptz NOT NULL DEFAULT now(),
 status varchar(20) NOT NULL CHECK(status IN ('PENDING','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED','NO_SHOW')),
 total numeric(14,2) NOT NULL CHECK(total > 0), currency char(3) NOT NULL DEFAULT 'TRY',
 version integer NOT NULL DEFAULT 1, created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(hotel_id,id), CHECK(check_out > check_in),
 FOREIGN KEY(hotel_id,room_type_id) REFERENCES room_type(hotel_id,id)
);
CREATE INDEX reservation_hotel_dates ON reservation(hotel_id,check_in,check_out);
CREATE TABLE reservation_night (
 hotel_id uuid NOT NULL, reservation_id uuid NOT NULL, stay_date date NOT NULL,
 price numeric(12,2) NOT NULL CHECK(price > 0), PRIMARY KEY(hotel_id,reservation_id,stay_date),
 FOREIGN KEY(hotel_id,reservation_id) REFERENCES reservation(hotel_id,id)
);
CREATE TABLE idempotency_record (
 hotel_id uuid NOT NULL REFERENCES hotel, key varchar(100) NOT NULL, request_hash char(64) NOT NULL,
 response jsonb, created_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(hotel_id,key)
);
CREATE TABLE outbox_event (
 id uuid PRIMARY KEY, hotel_id uuid NOT NULL, reservation_id uuid NOT NULL,
 aggregate_version integer NOT NULL, event_type varchar(60) NOT NULL, schema_version integer NOT NULL DEFAULT 1,
 payload jsonb NOT NULL, trace_id varchar(64) NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(hotel_id,reservation_id,aggregate_version),
 FOREIGN KEY(hotel_id,reservation_id) REFERENCES reservation(hotel_id,id)
);
CREATE TABLE event_delivery (
 event_id uuid NOT NULL REFERENCES outbox_event, consumer varchar(30) NOT NULL,
 attempts integer NOT NULL DEFAULT 0, next_attempt timestamptz NOT NULL DEFAULT now(),
 processed_at timestamptz, dead boolean NOT NULL DEFAULT false, last_error varchar(250),
 PRIMARY KEY(event_id,consumer)
);
CREATE INDEX delivery_pending ON event_delivery(consumer,next_attempt) WHERE processed_at IS NULL AND NOT dead;
CREATE TABLE guest (
 id uuid PRIMARY KEY, hotel_id uuid NOT NULL REFERENCES hotel,
 first_name varchar(100) NOT NULL, last_name varchar(100) NOT NULL,
 email varchar(254) NOT NULL, phone varchar(20) NOT NULL,
 marketing_allowed boolean NOT NULL DEFAULT false, consent_at timestamptz NOT NULL,
 matching_conflict boolean NOT NULL DEFAULT false, UNIQUE(hotel_id,id)
);
CREATE TABLE guest_identifier (
 hotel_id uuid NOT NULL, kind varchar(10) NOT NULL CHECK(kind IN ('EMAIL','PHONE')),
 value varchar(254) NOT NULL, guest_id uuid NOT NULL,
 PRIMARY KEY(hotel_id,kind,value), FOREIGN KEY(hotel_id,guest_id) REFERENCES guest(hotel_id,id)
);
CREATE TABLE crm_reservation (
 hotel_id uuid NOT NULL, reservation_id uuid NOT NULL, guest_id uuid NOT NULL,
 code varchar(30) NOT NULL, check_in date NOT NULL, check_out date NOT NULL,
 status varchar(20) NOT NULL, total numeric(14,2) NOT NULL, version integer NOT NULL,
 PRIMARY KEY(hotel_id,reservation_id), FOREIGN KEY(hotel_id,guest_id) REFERENCES guest(hotel_id,id)
);
CREATE INDEX crm_guest ON crm_reservation(hotel_id,guest_id,check_in DESC);
CREATE TABLE guest_note (
 id uuid PRIMARY KEY, hotel_id uuid NOT NULL, guest_id uuid NOT NULL,
 body varchar(2000) NOT NULL, author varchar(120) NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
 FOREIGN KEY(hotel_id,guest_id) REFERENCES guest(hotel_id,id)
);
CREATE TABLE guest_tag (
 hotel_id uuid NOT NULL, guest_id uuid NOT NULL, tag varchar(40) NOT NULL,
 PRIMARY KEY(hotel_id,guest_id,tag), FOREIGN KEY(hotel_id,guest_id) REFERENCES guest(hotel_id,id)
);
CREATE TABLE funnel_event (
 id uuid PRIMARY KEY, hotel_id uuid NOT NULL REFERENCES hotel, session_id uuid NOT NULL,
 stage varchar(20) NOT NULL CHECK(stage IN ('SEARCH','ROOM_SELECTED','FORM','CONFIRMED')),
 created_at timestamptz NOT NULL DEFAULT now(), UNIQUE(hotel_id,session_id,stage)
);
