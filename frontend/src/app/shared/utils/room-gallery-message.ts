export interface RoomGalleryRequest {
    roomId: string;
    currency: 'TRY' | 'EUR';
    language: 'tr' | 'en';
    selectable: boolean;
}
export function readRoomGalleryRequest(value: unknown): RoomGalleryRequest | null {
    if (!value || typeof value !== 'object')
        return null;
    const data = value as Record<string, unknown>;
    if (data['type'] !== 'rota:gallery:open'
        || typeof data['roomId'] !== 'string'
        || !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(data['roomId'])
        || (data['currency'] !== 'TRY' && data['currency'] !== 'EUR')
        || (data['language'] !== 'tr' && data['language'] !== 'en')
        || typeof data['selectable'] !== 'boolean')
        return null;
    return {
        roomId: data['roomId'], currency: data['currency'] as 'TRY' | 'EUR',
        language: data['language'] as 'tr' | 'en', selectable: data['selectable']
    };
}
