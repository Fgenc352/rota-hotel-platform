import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import ts from 'typescript';
import '@angular/compiler';
import { InjectionToken, createEnvironmentInjector, signal } from '@angular/core';
import { FormBuilder } from '@angular/forms';
function moduleUrl(source) {
    const { outputText } = ts.transpileModule(source, { compilerOptions: {
            target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022, experimentalDecorators: true
        } });
    return 'data:text/javascript;base64,' + Buffer.from(outputText).toString('base64');
}
const load = path => readFile(new URL('../src/app/' + path, import.meta.url), 'utf8');
const apiToken = new InjectionToken('BookingServicesTestApi');
const presentationToken = new InjectionToken('BookingServicesTestPresentation');
globalThis.__serviceTestApi = apiToken;
globalThis.__serviceTestPresentation = presentationToken;
const dateModule = moduleUrl(await load('shared/utils/display-date.ts'));
const attemptModule = moduleUrl(await load('features/booking/state/booking-attempt.ts'));
const apiErrorModule = moduleUrl(await load('core/http/api-error.ts'));
const errorModule = moduleUrl((await load('features/booking/presentation/booking-error-message.ts'))
    .replace("from '@core/http/api-error'", `from '${apiErrorModule}'`));
const storeSource = (await load('features/booking/state/booking.store.ts'))
    .replace("import { BookingApi } from '../data-access/booking-api.service';", 'const BookingApi = globalThis.__serviceTestApi;')
    .replace("import { BookingPresentation } from '../presentation/booking-presentation.service';", 'const BookingPresentation = globalThis.__serviceTestPresentation;')
    .replace("from '@shared/utils/display-date'", `from '${dateModule}'`)
    .replace("from './booking-attempt'", `from '${attemptModule}'`)
    .replace("from '../presentation/booking-error-message'", `from '${errorModule}'`)
    .replaceAll("from '@angular/core'", `from '${import.meta.resolve('@angular/core')}'`)
    .replaceAll("from '@angular/core/rxjs-interop'", `from '${import.meta.resolve('@angular/core/rxjs-interop')}'`)
    .replaceAll("from '@angular/forms'", `from '${import.meta.resolve('@angular/forms')}'`);
const { BookingStore } = await import(moduleUrl(storeSource));
delete globalThis.__serviceTestApi;
delete globalThis.__serviceTestPresentation;
globalThis.location = { search: '?hotel=lagos-beach' };
function setup(t, fail = false) {
    const requests = [];
    const api = {
        async services() {
            if (fail)
                throw new Error('Unavailable');
            return [
                {
                    code: 'BREAKFAST', name: 'Kahvaltı', price: 0, unit: 'PER_PERSON_PER_NIGHT', included: true
                },
                {
                    code: 'LUNCH', name: 'Öğle yemeği', price: 650, unit: 'PER_PERSON_PER_NIGHT', included: false
                },
                {
                    code: 'VALET', name: 'Vale', price: 300, unit: 'PER_NIGHT', included: false
                }
            ];
        },
        async book(key, body, idempotency) {
            requests.push({
                key, body, idempotency
            });
            return {
                id: 'booking', total: 8550, code: 'RT-TEST', status: 'CONFIRMED'
            };
        }
    };
    const presentation = {
        lang: signal('tr'), currency: signal('TRY'), hotel: signal(null), t: tr => tr, money: String
    };
    const injector = createEnvironmentInjector([
        { provide: apiToken, useValue: api }, { provide: presentationToken, useValue: presentation },
        { provide: FormBuilder, useValue: new FormBuilder() }, BookingStore
    ], null);
    t.after(() => injector.destroy());
    const store = injector.get(BookingStore);
    store.searchForm.patchValue({
        checkIn: '2027-02-10', checkOut: '2027-02-13', adults: 2, children: 1
    });
    store.guestForm.patchValue({
        firstName: 'Deniz', lastName: 'Test', email: 'deniz@example.com', phone: '+905551234567', privacyAccepted: true
    });
    store.selected.set({ roomTypeId: 'room', total: 1800 });
    return { store, requests };
}
test('included and selected services use actual stay occupancy and react to date edits', async (t) => {
    const { store } = setup(t);
    await store.loadServices();
    assert.equal(store.extrasTotal(), 0);
    assert.equal(store.serviceLines()[0].quantity, 9);
    store.toggleService('LUNCH', true);
    store.toggleService('VALET', true);
    assert.equal(store.extrasTotal(), 6750);
    assert.equal(store.bookingTotal(), 8550);
    store.searchForm.patchValue({ checkOut: '2027-02-12' });
    assert.equal(store.extrasTotal(), 4500);
    store.toggleService('VALET', false);
    assert.equal(store.extrasTotal(), 3900);
});
test('booking submits only selected service codes and receives authoritative receipt', async (t) => {
    const { store, requests } = setup(t);
    await store.loadServices();
    store.toggleService('VALET', true);
    store.toggleService('LUNCH', true);
    await store.book();
    assert.deepEqual(requests[0].body.serviceCodes, ['LUNCH', 'VALET']);
    assert.equal('total' in requests[0].body, false);
    assert.equal(store.receipt().total, 8550);
    store.startNewBooking();
    assert.deepEqual(store.selectedServices(), []);
});
test('service catalog failure blocks booking instead of omitting charges silently', async (t) => {
    const { store, requests } = setup(t, true);
    await store.loadServices();
    await store.book();
    assert.equal(store.servicesReady(), false);
    assert.equal(requests.length, 0);
    assert.match(store.error(), /Hizmet bilgileri/);
});
