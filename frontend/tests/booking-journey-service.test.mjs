import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import ts from 'typescript';
import '@angular/compiler';
import { InjectionToken, createEnvironmentInjector, signal } from '@angular/core';
import { FormBuilder } from '@angular/forms';
// Run the actual service with Angular signals and forms; no DOM or mocked Angular runtime.
function moduleUrl(source) {
    const { outputText } = ts.transpileModule(source, { compilerOptions: {
            target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022, experimentalDecorators: true
        } });
    return 'data:text/javascript;base64,' + Buffer.from(outputText).toString('base64');
}
const root = '../src/app/features/showcase/state/';
const policy = moduleUrl(await readFile(new URL(root + 'booking-navigation-policy.ts', import.meta.url), 'utf8'));
const token = new InjectionToken('JourneyTestStore');
globalThis.__journeyStore = token;
const source = (await readFile(new URL(root + 'booking-navigation.service.ts', import.meta.url), 'utf8'))
    .replace("import { BookingStore } from '@booking/state/booking.store';", 'const BookingStore = globalThis.__journeyStore;')
    .replaceAll("from '@angular/core'", `from '${import.meta.resolve('@angular/core')}'`)
    .replaceAll("from '@angular/core/rxjs-interop'", `from '${import.meta.resolve('@angular/core/rxjs-interop')}'`)
    .replaceAll("from './booking-navigation-policy'", `from '${policy}'`);
const { BookingNavigation } = await import(moduleUrl(source));
delete globalThis.__journeyStore;
function setup(t, response) {
    const forms = new FormBuilder().nonNullable;
    const offer = { roomTypeId: 'room-a', total: 6400 };
    const store = {
        searchForm: forms.group({
            checkIn: '2027-02-10', checkOut: '2027-02-12', adults: 2, children: 0
        }),
        guestForm: forms.group({ firstName: 'Deniz', email: 'saved@example.com' }),
        offers: signal([]), selected: signal(null), searched: signal(false), receipt: signal(null),
        busy: signal(false), error: signal(''),
        async search() {
            this.busy.set(true);
            this.offers.set([]);
            this.selected.set(null);
            try {
                this.offers.set(response ? await response : [offer]);
                this.searched.set(true);
            }
            finally {
                this.busy.set(false);
            }
        },
        select(value) {
            this.selected.set(value);
        },
        startNewBooking() {
            this.receipt.set(null);
            this.selected.set(null);
            this.searched.set(false);
        }
    };
    const injector = createEnvironmentInjector([{ provide: token, useValue: store }, BookingNavigation], null);
    t.after(() => injector.destroy());
    return {
        store, journey: injector.get(BookingNavigation), offer
    };
}
test('room step and guest step can be revisited without losing guest inputs or re-searching', async (t) => {
    const { journey, store, offer } = setup(t);
    await journey.search();
    assert.equal(journey.select(offer.roomTypeId), true);
    assert.equal(journey.step(), 3);
    const offers = store.offers();
    journey.goTo(2);
    assert.equal(journey.step(), 2);
    assert.equal(store.selected(), null);
    assert.equal(store.offers(), offers);
    journey.goTo(3);
    assert.equal(store.selected(), offer);
    assert.deepEqual(store.guestForm.getRawValue(), { firstName: 'Deniz', email: 'saved@example.com' });
});
test('date edits invalidate the actual quote and selected room, while preserving guest inputs', async (t) => {
    const { journey, store, offer } = setup(t);
    await journey.search();
    journey.select(offer.roomTypeId);
    journey.goTo(1);
    store.searchForm.patchValue({ checkOut: '2027-02-14' });
    assert.equal(journey.quoteCurrent(), false);
    assert.equal(journey.canVisit(2), false);
    assert.equal(journey.canVisit(3), false);
    assert.deepEqual(store.offers(), []);
    assert.equal(store.selected(), null);
    assert.equal(store.guestForm.controls.email.value, 'saved@example.com');
});
test('a response after stay criteria changed cannot restore stale room offers', async (t) => {
    let complete;
    const response = new Promise(resolve => {
        complete = resolve;
    });
    const { journey, store, offer } = setup(t, response);
    const pending = journey.search();
    store.searchForm.patchValue({ adults: 3 });
    complete([offer]);
    assert.equal(await pending, false);
    assert.deepEqual(store.offers(), []);
    assert.equal(journey.step(), 1);
});
test('confirmed bookings lock navigation until an explicit new booking', async (t) => {
    const { journey, store, offer } = setup(t);
    await journey.search();
    journey.select(offer.roomTypeId);
    store.receipt.set({ code: 'RT-TEST' });
    journey.goTo(2);
    assert.equal(journey.step(), 4);
    assert.equal(journey.canVisit(1), false);
    journey.reset();
    assert.equal(journey.step(), 1);
    assert.equal(store.receipt(), null);
    assert.equal(journey.rememberedRoom(), undefined);
});
