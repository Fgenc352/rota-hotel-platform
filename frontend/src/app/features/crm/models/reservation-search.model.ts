export interface ReservationSearch {
    code?: string;
    firstName?: string;
    lastName?: string;
    email?: string;
    phone?: string;
    roomTypeId?: string;
    status?: string;
    checkInFrom?: string;
    checkInTo?: string;
    checkOutFrom?: string;
    checkOutTo?: string;
    adults?: number | null;
    children?: number | null;
    minTotal?: number | null;
    maxTotal?: number | null;
}
