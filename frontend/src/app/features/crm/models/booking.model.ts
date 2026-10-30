export interface Booking {
    id?: string;
    reservation_id?: string;
    code: string;
    first_name?: string;
    last_name?: string;
    check_in: string;
    check_out: string;
    status: string;
    total: number;
    room_name?: string;
    checked_out_at?: string;
    version?: number;
    guest_id?: string;
    room_type_id?: string;
    email?: string;
    phone?: string;
    adults?: number;
    children?: number;
    currency?: string;
    room_total?: number;
    created_at?: string;
    services?: import('@booking/models/service-option.model').ServiceLine[];
}
