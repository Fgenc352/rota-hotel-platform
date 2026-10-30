import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { randomUUID } from 'node:crypto';
import { pmsStore } from './store.mjs';
import { receiveReservation, reservationMessage } from './reservation-handler.mjs';

const message = () => ({ eventId: randomUUID(), hotelId: randomUUID(), reservationId: randomUUID(), code: 'RT-PMS-TEST', checkIn: '2026-11-07', checkOut: '2026-11-09' });
async function setup(t) {
    const directory = await mkdtemp(join(tmpdir(), 'rota-pms-unit-'));
    t.after(() => rm(directory, { recursive: true, force: true }));
    return { directory, store: await pmsStore(directory) };
}

test('concurrent duplicates persist one reservation and survive a store restart', async t => {
    const { directory, store } = await setup(t);
    const event = message();
    const results = await Promise.all(Array.from({ length: 20 }, () => store.accept(event)));
    assert.equal(results.filter(result => !result.duplicate).length, 1);
    assert.equal(store.reservations().length, 1);
    assert.equal(await readFile(join(directory, 'accepted.jsonl'), 'utf8'), event.eventId + '\n');
    const restarted = await pmsStore(directory);
    assert.equal((await restarted.accept(event)).duplicate, true);
    assert.equal(restarted.reservations()[0].code, event.code);
    assert.equal(restarted.summary().accepted, 1);
});

test('old accepted ID journal remains idempotent and can be enriched with display metadata', async t => {
    const { directory } = await setup(t);
    const event = message();
    await writeFile(join(directory, 'accepted.jsonl'), event.eventId + '\n');
    const store = await pmsStore(directory);
    assert.equal((await store.accept(event)).duplicate, true);
    assert.equal(store.reservations()[0].checkOut, event.checkOut);
    assert.equal(await readFile(join(directory, 'accepted.jsonl'), 'utf8'), event.eventId + '\n');
});

test('the PMS contract does not persist extra guest contact data', () => {
    const event = message();
    const safe = reservationMessage({ ...event, email: 'private@example.com', firstName: '<script>', phone: '+905551111111' }, event.eventId);
    assert.deepEqual(safe, event);
    for (const invalid of [null, {}, { ...event, eventId: 'bad' }, { ...event, hotelId: '../crm' }, { ...event, checkIn: 'yesterday' }]) {
        assert.throws(() => reservationMessage(invalid, event.eventId));
    }
});

test('outage, recovery, duplicate and failed request history are visible separately', async t => {
    const { directory, store } = await setup(t);
    const event = message();
    const simulation = { paused: true, delayMs: 0 };
    const send = number => receiveReservation({ body: event, headers: { 'idempotency-key': event.eventId }, store, simulation, failFirst: 0, requestNumber: number });
    assert.equal((await send(1)).status, 503);
    assert.equal(store.reservations().length, 0);
    simulation.paused = false;
    assert.equal((await send(2)).body.duplicate, false);
    assert.equal((await send(3)).body.duplicate, true);
    assert.deepEqual(store.summary(), { accepted: 1, visibleReservations: 1, legacyEvents: 0, attempts: 3, duplicates: 1, failed: 1 });
    const restarted = await pmsStore(directory);
    assert.equal(restarted.attempts().length, 3);
    assert.equal(restarted.reservations().length, 1);
});

test('invalid requests never enter the accepted journal', async t => {
    const { store } = await setup(t);
    const result = await receiveReservation({ body: message(), headers: {}, store, simulation: { paused: false, delayMs: 0 }, failFirst: 0, requestNumber: 1 });
    assert.equal(result.status, 400);
    assert.equal(store.summary().accepted, 0);
});

test('fail-first simulation rejects a valid request before accepting the retry', async t => {
    const { store } = await setup(t);
    const event = message();
    const send = number => receiveReservation({ body: event, headers: { 'idempotency-key': event.eventId }, store, simulation: { paused: false, delayMs: 0 }, failFirst: 1, requestNumber: number });
    assert.equal((await send(1)).status, 503);
    assert.equal((await send(2)).status, 200);
    assert.equal(store.summary().failed, 1);
});

test('one reservation keeps its latest status and version when old events arrive after checkout', async t => {
    const { directory, store } = await setup(t);
    const base = message();
    const checkout = { ...base, eventId: randomUUID(), status: 'CHECKED_OUT', version: 3, checkedOutAt: '2026-11-09T10:00:00Z' };
    await store.accept(checkout);
    await store.accept({ ...base, status: 'CONFIRMED', version: 1 });
    const stale = await store.accept({ ...base, eventId: randomUUID(), status: 'CHECKED_IN', version: 2 });
    assert.equal(stale.stale, true);
    assert.equal(store.reservations().length, 1);
    assert.equal(store.reservations()[0].status, 'CHECKED_OUT');
    assert.equal(store.history(base.hotelId, base.reservationId).length, 3);
    const restarted = await pmsStore(directory);
    assert.equal(restarted.reservations()[0].version, 3);
    assert.equal(restarted.reservations()[0].checkedOutAt, checkout.checkedOutAt);
});

test('reusing an event ID with a different status is rejected without changing current state', async t => {
    const { store } = await setup(t);
    const event = { ...message(), status: 'CONFIRMED', version: 1 };
    await store.accept(event);
    await assert.rejects(store.accept({ ...event, status: 'CANCELLED', version: 2 }), error => error.status === 409);
    assert.equal(store.reservations()[0].status, 'CONFIRMED');
});

test('different hotel reservations cannot overwrite each other in the projection', async t => {
    const { store } = await setup(t);
    const first = { ...message(), status: 'CONFIRMED', version: 1 };
    await store.accept(first);
    await store.accept({ ...first, eventId: randomUUID(), hotelId: randomUUID(), status: 'CHECKED_IN', version: 2 });
    assert.equal(store.reservations().length, 2);
    assert.equal(store.history(first.hotelId, first.reservationId).length, 1);
});

test('status contract rejects unknown states, invalid versions and invalid actual checkout times', () => {
    const base = message();
    for (const extra of [{ status: 'INVALID', version: 2 }, { status: 'CHECKED_IN', version: 0 }, { version: 2 }, { status: 'CHECKED_OUT', version: 2, checkedOutAt: 'yesterday' }]) {
        assert.throws(() => reservationMessage({ ...base, ...extra }, base.eventId));
    }
});
