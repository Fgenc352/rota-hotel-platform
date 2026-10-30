export interface Guest {
    id: string;
    hotel_id?: string;
    consent_at?: string;
    first_name: string;
    last_name: string;
    email: string;
    phone: string;
    marketing_allowed: boolean;
    stays: number;
    spend: number;
    active_reservations: number;
    booked_value: number;
}
