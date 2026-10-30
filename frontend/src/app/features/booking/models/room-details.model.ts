export interface RoomDetails {
    id: string;
    name: string;
    description: string;
    capacity: number;
    areaSquareMeters: number;
    bedDescription: string;
    nightlyPrice: number;
    currency: string;
    minNights: number;
    amenities: string[];
    images: string[];
}
