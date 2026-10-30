const uuid = /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/i;
const date = /^\d{4}-\d{2}-\d{2}$/;
const validDate = value => {
    if (typeof value !== 'string' || !date.test(value)) return false;
    const parsed = new Date(value + 'T00:00:00Z');
    return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
};

/** Whitelist the integration contract: guest contact details are never journaled. */
export function reservationMessage(body, idempotencyKey) {
    if (!body || typeof body !== 'object' || typeof idempotencyKey !== 'string'
        || !uuid.test(idempotencyKey) || body.eventId !== idempotencyKey) {
        throw new Error('Invalid event ID');
    }
    const result = { eventId: idempotencyKey };
    for (const key of ['hotelId', 'reservationId']) {
        if (body[key] !== undefined) {
            if (typeof body[key] !== 'string' || !uuid.test(body[key])) throw new Error('Invalid reference');
            result[key] = body[key];
        }
    }
    if (body.code !== undefined) {
        if (typeof body.code !== 'string' || !body.code.length || body.code.length > 100) throw new Error('Invalid code');
        result.code = body.code;
    }
    for (const key of ['checkIn', 'checkOut']) {
        if (body[key] !== undefined) {
            if (!validDate(body[key])) throw new Error('Invalid stay date');
            result[key] = body[key];
        }
    }
    if (result.checkIn && result.checkOut && result.checkOut <= result.checkIn) throw new Error('Invalid stay interval');
    if (body.status !== undefined) {
        if (!['PENDING','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED','NO_SHOW'].includes(body.status)
            || !Number.isSafeInteger(body.version) || body.version < 1) throw new Error('Invalid source state');
        result.status = body.status;
        result.version = body.version;
    } else if (body.version !== undefined) {
        throw new Error('Status required with source version');
    }
    if (body.checkedOutAt != null) {
        if (typeof body.checkedOutAt !== 'string' || !/^\d{4}-\d{2}-\d{2}T/.test(body.checkedOutAt)
            || Number.isNaN(Date.parse(body.checkedOutAt))) throw new Error('Invalid checkout time');
        result.checkedOutAt = body.checkedOutAt;
    }
    return result;
}

export async function receiveReservation({ body, headers, store, simulation, failFirst, requestNumber }) {
    let message;
    try {
        message = reservationMessage(body, headers['idempotency-key']);
    } catch {
        return { status: 400, body: { error: 'Invalid PMS request' } };
    }
    if (simulation.paused || requestNumber <= failFirst) {
        await store.recordAttempt({ ...message, status: 503, duplicate: false });
        return { status: 503, body: { error: 'Simulated PMS outage' } };
    }
    if (simulation.delayMs) await new Promise(resolve => setTimeout(resolve, simulation.delayMs));
    try {
        const { duplicate, stale, applied } = await store.accept(message);
        await store.recordAttempt({ ...message, reservationStatus: message.status, status: 200, duplicate, stale });
        return { status: 200, body: { accepted: true, eventId: message.eventId, duplicate, stale, applied } };
    } catch (error) {
        if (error.status !== 409) throw error;
        await store.recordAttempt({ ...message, reservationStatus: message.status, status: 409, duplicate: false });
        return { status: 409, body: { error: 'Event ID payload conflict' } };
    }
}
