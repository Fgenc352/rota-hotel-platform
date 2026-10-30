const photographedServices = new Set([
    'BREAKFAST', 'LUNCH', 'DINNER', 'VALET', 'TRANSFER'
]);
export function serviceImage(hotelKey: string, code: string): string | null {
    const hotel = hotelKey === 'lagos-beach' ? 'lagos' : hotelKey === 'peri-stone' ? 'peri' : null;
    return hotel && photographedServices.has(code) ? `/images/services/${hotel}-${code.toLowerCase()}.webp` : null;
}
