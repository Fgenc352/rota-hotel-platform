CREATE TABLE pms_daily_service (
    id uuid PRIMARY KEY,
    hotel_id uuid NOT NULL,
    reservation_id uuid NOT NULL,
    code varchar(40) NOT NULL CHECK (code IN ('BREAKFAST','LUNCH','DINNER','VALET','TRANSFER')),
    service_date date NOT NULL,
    status varchar(20) NOT NULL CHECK (status IN ('PENDING','PREPARING','SERVED','PLANNED','EN_ROUTE','PICKED_UP','COMPLETED','RECEIVED','PARKED','REQUESTED','DELIVERED')),
    service_time time,
    pickup varchar(200) NOT NULL DEFAULT '',
    destination varchar(200) NOT NULL DEFAULT '',
    plate varchar(30) NOT NULL DEFAULT '',
    notes varchar(1000) NOT NULL DEFAULT '',
    passengers integer NOT NULL CHECK (passengers >= 0),
    actor varchar(120) NOT NULL,
    updated_at timestamptz NOT NULL,
    lock_version bigint NOT NULL DEFAULT 0,
    UNIQUE (hotel_id,reservation_id,code,service_date),
    FOREIGN KEY (hotel_id,reservation_id) REFERENCES reservation(hotel_id,id)
);
-- A selected transfer package is one journey and can be rescheduled, not duplicated across days.
CREATE UNIQUE INDEX pms_daily_transfer_once ON pms_daily_service(hotel_id,reservation_id,code) WHERE code='TRANSFER';
CREATE INDEX pms_daily_service_day ON pms_daily_service(hotel_id,service_date,code);
CREATE TABLE pms_daily_service_audit (
    id uuid PRIMARY KEY,
    hotel_id uuid NOT NULL,
    reservation_id uuid NOT NULL,
    code varchar(40) NOT NULL,
    service_date date NOT NULL,
    actor varchar(120) NOT NULL,
    before_state jsonb NOT NULL,
    after_state jsonb NOT NULL,
    created_at timestamptz NOT NULL,
    FOREIGN KEY (hotel_id,reservation_id) REFERENCES reservation(hotel_id,id)
);
CREATE INDEX pms_daily_audit_day ON pms_daily_service_audit(hotel_id,reservation_id,code,service_date,created_at DESC);
