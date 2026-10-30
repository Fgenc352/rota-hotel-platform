import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { stripTypeScriptTypes } from 'node:module';
const sourceUrl = new URL('../src/app/features/booking/state/booking-attempt.ts', import.meta.url);
const source = await readFile(sourceUrl, 'utf8');
const javascript = stripTypeScriptTypes(source, { sourceUrl: sourceUrl.href });
const { BookingAttempt } = await import('data:text/javascript;base64,' + Buffer.from(javascript).toString('base64'));
const booking = {
    checkIn: '2026-11-10',
    checkOut: '2026-11-12',
    adults: 2,
    children: 0,
    roomTypeId: 'room-a',
    firstName: 'Deniz',
    lastName: 'Test',
    email: 'deniz@example.com',
    phone: '+905551234567',
    privacyAccepted: true,
    marketingAllowed: false,
    sessionId: 'session-a'
};
function attempts() {
    let counter = 0;
    return new BookingAttempt(() => `attempt-${++counter}`);
}
test('the first submission always has a key and an unchanged retry reuses it', () => {
    const attempt = attempts();
    const first = attempt.keyFor(booking);
    assert.equal(first, 'attempt-1');
    assert.equal(attempt.keyFor({ ...booking }), first);
});
test('changing the booking creates a different key', () => {
    const attempt = attempts();
    const first = attempt.keyFor(booking);
    assert.notEqual(attempt.keyFor({
        ...booking,
        email: 'other@example.com'
    }), first);
});
test('returning to an earlier room preserves its pending request key', () => {
    const attempt = attempts();
    const originalRoom = attempt.keyFor(booking);
    attempt.keyFor({
        ...booking,
        roomTypeId: 'room-b'
    });
    assert.equal(attempt.keyFor(booking), originalRoom);
});
test('a new booking after completion receives a fresh key even with identical details', () => {
    const attempt = attempts();
    const first = attempt.keyFor(booking);
    attempt.complete();
    assert.notEqual(attempt.keyFor(booking), first);
});
