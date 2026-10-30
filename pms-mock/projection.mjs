export const reservationKey = value => value.hotelId && value.reservationId
    ? value.hotelId + ':' + value.reservationId : 'legacy:' + value.eventId;

const normalized = value => ({
    eventId: value.eventId, hotelId: value.hotelId ?? null, reservationId: value.reservationId ?? null,
    code: value.code ?? null, checkIn: value.checkIn ?? null, checkOut: value.checkOut ?? null,
    status: value.status ?? 'CONFIRMED', version: value.version ?? 1, checkedOutAt: value.checkedOutAt ?? null
});
export const sameEvent = (first, second) => JSON.stringify(normalized(first)) === JSON.stringify(normalized(second));

/** Complete snapshots may arrive out of order. Only a greater source version replaces current state. */
export function projectReservation(current, receipt) {
    const next = { ...receipt, status: receipt.status ?? 'CONFIRMED', version: receipt.version ?? 1 };
    if (!current || next.version > current.version) return next;
    return current;
}
