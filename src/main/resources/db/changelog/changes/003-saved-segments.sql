CREATE TABLE guest_segment (
 id uuid PRIMARY KEY, hotel_id uuid NOT NULL REFERENCES hotel,
 name varchar(100) NOT NULL, min_stays integer NOT NULL DEFAULT 0 CHECK(min_stays BETWEEN 0 AND 100),
 months integer NOT NULL DEFAULT 12 CHECK(months BETWEEN 1 AND 120),
 days_since_checkout integer CHECK(days_since_checkout BETWEEN 0 AND 3650),
 marketing_required boolean NOT NULL DEFAULT false,
 UNIQUE(hotel_id,name)
);
