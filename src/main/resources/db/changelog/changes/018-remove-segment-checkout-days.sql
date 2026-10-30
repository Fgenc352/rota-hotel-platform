-- The optional checkout-day restriction is no longer part of saved segments.
ALTER TABLE guest_segment DROP COLUMN days_since_checkout;
