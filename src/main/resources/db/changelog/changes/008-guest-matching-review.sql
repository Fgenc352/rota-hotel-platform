ALTER TABLE guest ADD COLUMN merged_into uuid;
ALTER TABLE guest ADD CONSTRAINT guest_merge_target FOREIGN KEY(hotel_id,merged_into) REFERENCES guest(hotel_id,id);
ALTER TABLE guest ADD CONSTRAINT guest_merge_not_self CHECK(merged_into IS NULL OR merged_into<>id);
CREATE INDEX guest_contact_email ON guest_identifier(hotel_id,email);
CREATE INDEX guest_contact_phone ON guest_identifier(hotel_id,phone);
CREATE TABLE guest_match_audit (
    id uuid PRIMARY KEY,
    hotel_id uuid NOT NULL,
    source_guest_id uuid NOT NULL,
    target_guest_id uuid NOT NULL,
    resolved_by varchar(120) NOT NULL,
    resolved_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(hotel_id,source_guest_id),
    FOREIGN KEY(hotel_id,source_guest_id) REFERENCES guest(hotel_id,id),
    FOREIGN KEY(hotel_id,target_guest_id) REFERENCES guest(hotel_id,id),
    CHECK(source_guest_id<>target_guest_id)
);
