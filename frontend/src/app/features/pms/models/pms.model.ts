export type Housekeeping = 'CLEAN' | 'DIRTY' | 'INSPECTING';
export interface PhysicalRoom {
    id: string;
    roomTypeId: string;
    roomTypeName: string;
    number: string;
    housekeeping: Housekeeping;
    occupantId: string | null;
    occupantCode: string | null;
    upcomingAssignments: number;
}
export interface RoomAssignment {
    reservationId: string;
    roomId: string;
    code: string;
    checkIn: string;
    checkOut: string;
    status: string;
}
export interface RoomAudit {
    id: string;
    roomId: string;
    reservationId: string | null;
    roomNumber: string;
    action: string;
    actor: string;
    beforeState: string;
    afterState: string;
    createdAt: string;
}
export interface PmsBoard {
    rooms: PhysicalRoom[];
    assignments: RoomAssignment[];
}
export const housekeepingLabel = (state: string) => ({
    CLEAN: 'Hazır', DIRTY: 'Temizlik gerekli', INSPECTING: 'Kontrolde'
} as Record<string, string>)[state] || state;
export type ServiceProgress = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
export interface ServiceTask {
    line: import('@booking/models/service-option.model').ServiceLine;
    progress: ServiceProgress;
    trackable: boolean;
    actor: string | null;
    updatedAt: string | null;
}
export interface ServiceTaskAudit {
    id: string;
    code: string;
    previousState: ServiceProgress;
    nextState: ServiceProgress;
    actor: string;
    createdAt: string;
}
export interface ReservationServices {
    reservationId: string;
    code: string;
    status: string;
    services: ServiceTask[];
    history: ServiceTaskAudit[];
}
export interface RoomAuditPage {
    items: RoomAudit[];
    total: number;
    page: number;
    size: number;
}
