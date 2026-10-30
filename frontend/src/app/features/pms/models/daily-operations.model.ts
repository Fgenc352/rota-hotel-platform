export type DailyStatus = 'PENDING' | 'PREPARING' | 'SERVED' | 'PLANNED' | 'EN_ROUTE' | 'PICKED_UP' | 'COMPLETED' | 'RECEIVED' | 'PARKED' | 'REQUESTED' | 'DELIVERED';
export interface DailyTask {
    reservationId: string;
    code: string;
    date: string;
    status: DailyStatus;
    time: string | null;
    pickup: string;
    destination: string;
    plate: string;
    notes: string;
    passengers: number;
    actor: string | null;
    updatedAt: string | null;
}
export interface DailyService {
    reservationId: string;
    bookingCode: string;
    guestName: string;
    phone: string;
    roomNumber: string;
    reservationStatus: string;
    serviceName: string;
    quantity: number;
    task: DailyTask;
}
export interface DailyStay {
    reservationId: string;
    code: string;
    guestName: string;
    phone: string;
    roomNumber: string;
    roomType: string;
    people: number;
    checkIn: string;
    checkOut: string;
    status: string;
    arrival: boolean;
    departure: boolean;
    checkedOutAt?: string | null;
}
export interface DailyOperations {
    date: string;
    stays: DailyStay[];
    services: DailyService[];
    upcomingTransfers: DailyService[];
    totalRooms: number;
    occupiedNow: number;
    emptyNow: number;
    plannedOccupied: number;
    arrivals: number;
    departures: number;
}
export interface DailyAudit {
    id: string;
    actor: string;
    beforeState: string;
    afterState: string;
    createdAt: string;
}
export const dailyStatusLabel = (value: string) => ({
    PENDING: 'Bekliyor', PREPARING: 'Hazırlanıyor', SERVED: 'Servis edildi', PLANNED: 'Planlandı',
    EN_ROUTE: 'Araç yolda', PICKED_UP: 'Misafir alındı', COMPLETED: 'Transfer tamamlandı',
    RECEIVED: 'Araç teslim alındı', PARKED: 'Park edildi', REQUESTED: 'Teslim istendi', DELIVERED: 'Araç teslim edildi'
} as Record<string, string>)[value] || value;
export function workflow(code: string): DailyStatus[] {
    if (['BREAKFAST', 'LUNCH', 'DINNER'].includes(code))
        return ['PENDING', 'PREPARING', 'SERVED'];
    if (code === 'TRANSFER')
        return [
            'PENDING', 'PLANNED', 'EN_ROUTE', 'PICKED_UP', 'COMPLETED'
        ];
    if (code === 'VALET')
        return [
            'PENDING', 'RECEIVED', 'PARKED', 'REQUESTED', 'DELIVERED'
        ];
    return [];
}
