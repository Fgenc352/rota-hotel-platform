import type { StayInput } from './stay-input.model';
export interface BookingInput extends StayInput {
    roomTypeId: string;
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    privacyAccepted: boolean;
    marketingAllowed: boolean;
    sessionId: string;
    serviceCodes?: string[];
}
