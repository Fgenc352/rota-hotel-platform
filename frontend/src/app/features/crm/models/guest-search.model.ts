export interface GuestSearch {
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    checkInFrom: string;
    checkInTo: string;
    checkOutFrom: string;
    checkOutTo: string;
}
export function emptyGuestSearch(): GuestSearch {
    return {
        firstName: '', lastName: '', email: '', phone: '',
        checkInFrom: '', checkInTo: '', checkOutFrom: '', checkOutTo: ''
    };
}
