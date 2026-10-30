CREATE TABLE pms_service_task (
 id uuid PRIMARY KEY, hotel_id uuid NOT NULL, reservation_id uuid NOT NULL,
 code varchar(40) NOT NULL, progress varchar(20) NOT NULL CHECK(progress IN ('PENDING','IN_PROGRESS','COMPLETED')),
 updated_by varchar(120) NOT NULL, updated_at timestamptz NOT NULL, lock_version bigint NOT NULL DEFAULT 0,
 UNIQUE(hotel_id,reservation_id,code),
 FOREIGN KEY(hotel_id,reservation_id) REFERENCES reservation(hotel_id,id)
);
CREATE TABLE pms_service_audit (
 id uuid PRIMARY KEY, hotel_id uuid NOT NULL, reservation_id uuid NOT NULL, code varchar(40) NOT NULL,
 previous_state varchar(20) NOT NULL, next_state varchar(20) NOT NULL,
 actor varchar(120) NOT NULL, created_at timestamptz NOT NULL,
 FOREIGN KEY(hotel_id,reservation_id) REFERENCES reservation(hotel_id,id)
);
CREATE INDEX pms_service_history ON pms_service_audit(hotel_id,reservation_id,created_at DESC);
