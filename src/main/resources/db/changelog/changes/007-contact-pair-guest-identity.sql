-- Rebuild the derived identity lookup, keeping guest/profile/reservation/note data untouched.
DROP TABLE guest_identifier;

CREATE TABLE guest_identifier (
    hotel_id uuid NOT NULL,
    email varchar(254) NOT NULL,
    phone varchar(20) NOT NULL,
    guest_id uuid NOT NULL,
    PRIMARY KEY(hotel_id,email,phone),
    FOREIGN KEY(hotel_id,guest_id) REFERENCES guest(hotel_id,id)
);

INSERT INTO guest_identifier(hotel_id,email,phone,guest_id)
SELECT DISTINCT ON (hotel_id,lower(btrim(email)),phone)
       hotel_id,lower(btrim(email)),phone,id
FROM guest
ORDER BY hotel_id,lower(btrim(email)),phone,matching_conflict,id;
