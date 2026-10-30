export type BookingFrameMessage = {
    type: 'rota:premium-widget:resize';
    height: number;
} | {
    type: 'rota:booking:confirmed';
    height: number;
    top: number;
    cardHeight: number;
};
/** Only layout coordinates cross the iframe boundary; no guest or receipt data. */
export function readBookingFrameMessage(value: unknown): BookingFrameMessage | null {
    if (!value || typeof value !== 'object')
        return null;
    const data = value as Record<string, unknown>;
    const height = data['height'];
    if (typeof height !== 'number' || !Number.isFinite(height) || height < 200 || height > 10000)
        return null;
    if (data['type'] === 'rota:premium-widget:resize')
        return { type: data['type'], height };
    const top = data['top'];
    const cardHeight = data['cardHeight'];
    if (data['type'] === 'rota:booking:confirmed' && typeof top === 'number'
        && Number.isFinite(top) && top >= 0 && top < height
        && typeof cardHeight === 'number' && Number.isFinite(cardHeight) && cardHeight > 0
        && top + cardHeight <= height + 1) {
        return {
            type: data['type'], height, top, cardHeight
        };
    }
    return null;
}
export function centeredViewportOffset(viewportHeight: number, headerHeight: number, contentHeight: number): number {
    return headerHeight + Math.max(24, (viewportHeight - headerHeight - contentHeight) / 2);
}
/** Center the full card in the viewport area below the fixed hotel header. */
export function confirmationScrollTop(frameTop: number, scrollY: number, cardTop: number, headerHeight: number, viewportHeight: number, cardHeight: number): number {
    return Math.max(0, frameTop + scrollY + cardTop - centeredViewportOffset(viewportHeight, headerHeight, cardHeight));
}
