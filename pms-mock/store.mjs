import { appendFile, mkdir, readFile } from 'node:fs/promises';
import { join } from 'node:path';
import { reservationKey, projectReservation, sameEvent } from './projection.mjs';

function records(text) {
    return text.split('\n').filter(Boolean).flatMap(line => {
        try { return [JSON.parse(line)]; } catch { return []; }
    });
}

/** Local durable inbox. One queue protects idempotency checks and journal writes. */
export async function pmsStore(directory) {
    await mkdir(directory, { recursive: true });
    const file = name => join(directory, name);
    const read = name => readFile(file(name), 'utf8').catch(error => {
        if (error.code === 'ENOENT') return '';
        throw error;
    });
    const accepted = new Set((await read('accepted.jsonl')).split('\n').filter(Boolean));
    const receipts = new Map(records(await read('reservations.jsonl')).map(receipt => [receipt.eventId, receipt]));
    const current = new Map();
    for (const receipt of receipts.values()) {
        accepted.add(receipt.eventId);
        const key = reservationKey(receipt);
        current.set(key, projectReservation(current.get(key), receipt));
    }
    const attempts = records(await read('requests.jsonl')).slice(-1000);
    let queue = Promise.resolve();
    const serialize = action => {
        const result = queue.then(action);
        queue = result.catch(() => {});
        return result;
    };
    return {
        accept(event) {
            return serialize(async () => {
                const duplicate = accepted.has(event.eventId);
                const existing = receipts.get(event.eventId);
                if (existing && !sameEvent(existing, event)) {
                    throw Object.assign(new Error('The event ID was reused with different data'), { status: 409 });
                }
                const key = reservationKey(event);
                const before = current.get(key);
                let applied = false;
                if (!receipts.has(event.eventId)) {
                    const receipt = { ...event, acceptedAt: new Date().toISOString() };
                    await appendFile(file('reservations.jsonl'), JSON.stringify(receipt) + '\n');
                    receipts.set(event.eventId, receipt);
                    const projected = projectReservation(before, receipt);
                    applied = projected !== before;
                    current.set(key, projected);
                }
                if (!duplicate) {
                    await appendFile(file('accepted.jsonl'), event.eventId + '\n');
                    accepted.add(event.eventId);
                }
                return { duplicate, applied, stale: !duplicate && !applied, receipt: receipts.get(event.eventId) };
            });
        },
        recordAttempt(attempt) {
            return serialize(async () => {
                const entry = { ...attempt, receivedAt: new Date().toISOString() };
                await appendFile(file('requests.jsonl'), JSON.stringify(entry) + '\n');
                attempts.push(entry);
                if (attempts.length > 1000) attempts.shift();
            });
        },
        reservations() { return [...current.values()].sort((a, b) => b.acceptedAt.localeCompare(a.acceptedAt)); },
        history(hotel, reservation) {
            return [...receipts.values()].filter(row => row.hotelId === hotel && row.reservationId === reservation)
                .sort((a, b) => (b.version ?? 1) - (a.version ?? 1) || b.acceptedAt.localeCompare(a.acceptedAt)).slice(0, 100);
        },
        attempts() { return [...attempts].reverse(); },
        summary() {
            return {
                accepted: accepted.size,
                visibleReservations: current.size,
                legacyEvents: accepted.size - receipts.size,
                attempts: attempts.length,
                duplicates: attempts.filter(item => item.duplicate).length,
                failed: attempts.filter(item => item.status >= 400).length
            };
        }
    };
}
