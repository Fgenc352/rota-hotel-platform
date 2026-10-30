import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { stripTypeScriptTypes } from 'node:module';
const path = new URL('../src/app/features/showcase/state/booking-navigation-policy.ts', import.meta.url);
const source = stripTypeScriptTypes(await readFile(path, 'utf8'), { sourceUrl: path.href });
const { canVisitBookingStep, stayFingerprint } = await import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'));
const initial = {
    busy: false, confirmed: false, quoteCurrent: false, roomAvailable: false
};
test('dates are reachable initially but future booking stages cannot be skipped', () => {
    assert.equal(canVisitBookingStep(1, initial), true);
    for (const step of [2, 3, 4])
        assert.equal(canVisitBookingStep(step, initial), false);
});
test('a current quote allows room navigation, and a remembered room allows returning to guest details', () => {
    const state = {
        ...initial, quoteCurrent: true, roomAvailable: true
    };
    for (const step of [1, 2, 3])
        assert.equal(canVisitBookingStep(step, state), true);
    assert.equal(canVisitBookingStep(4, state), false);
    assert.equal(canVisitBookingStep(3, { ...state, roomAvailable: false }), false);
});
test('editing stay criteria invalidates room and guest navigation even when an earlier room is remembered', () => {
    const stay = {
        checkIn: '2027-02-10', checkOut: '2027-02-12', adults: 2, children: 0
    };
    assert.equal(stayFingerprint(stay), stayFingerprint({
        children: 0, adults: 2, checkOut: stay.checkOut, checkIn: stay.checkIn
    }));
    for (const change of [
        { checkIn: '2027-02-11' }, { checkOut: '2027-02-13' }, { adults: 3 }, { children: 1 }
    ]) {
        assert.notEqual(stayFingerprint(stay), stayFingerprint({ ...stay, ...change }));
    }
    for (const step of [2, 3])
        assert.equal(canVisitBookingStep(step, { ...initial, roomAvailable: true }), false);
});
test('in-flight and confirmed reservations cannot be reopened through the stepper', () => {
    for (const state of [{ busy: true, confirmed: false }, { busy: false, confirmed: true }]) {
        for (const step of [
            1, 2, 3, 4
        ]) {
            assert.equal(canVisitBookingStep(step, {
                ...state, quoteCurrent: true, roomAvailable: true
            }), false);
        }
    }
});
