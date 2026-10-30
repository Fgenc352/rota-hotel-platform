import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { stripTypeScriptTypes } from 'node:module';
const sourceUrl = new URL('../src/app/features/pms/data-access/daily-service-request.ts', import.meta.url);
const source = await readFile(sourceUrl, 'utf8');
const javascript = stripTypeScriptTypes(source, { sourceUrl: sourceUrl.href });
const contract = JSON.parse(await readFile(new URL('../../doc/openapi.json', import.meta.url), 'utf8'));
const requestSchema = contract.components.schemas.DailyServiceRequest;
const { dailyServiceRequest } = await import('data:text/javascript;base64,' + Buffer.from(javascript).toString('base64'));
test('daily service writes send only the strict request fields and never audit or identity data', () => {
    const task = {
        reservationId: 'private-reservation', code: 'TRANSFER', date: '2026-10-29', status: 'PLANNED',
        time: '09:30', pickup: 'Airport', destination: 'Hotel', plate: '34 TEST', notes: 'Flight 123',
        passengers: 2, actor: 'staff', updatedAt: '2026-10-28T12:00:00Z', hotelId: 'forged-hotel'
    };
    const body = dailyServiceRequest(task);
    assert.deepEqual(body, {
        date: task.date, status: task.status, time: task.time, pickup: task.pickup,
        destination: task.destination, plate: task.plate, notes: task.notes, passengers: task.passengers
    });
    assert.equal(JSON.stringify(body).includes('private-reservation'), false);
    assert.equal(JSON.stringify(body).includes('forged-hotel'), false);
    assert.equal(task.actor, 'staff');
    assert.ok(Object.keys(body).every(key => Object.hasOwn(requestSchema.properties, key)));
    assert.ok((requestSchema.required || []).every(key => body[key] != null));
});
test('meal writes preserve an optional empty time and notes without adding read-model properties', () => {
    const body = dailyServiceRequest({
        reservationId: 'r', code: 'LUNCH', date: '2026-10-28', status: 'SERVED',
        time: null, pickup: '', destination: '', plate: '', notes: '', passengers: 0, actor: null, updatedAt: null
    });
    assert.equal(body.time, null);
    assert.equal(body.status, 'SERVED');
    assert.equal(Object.keys(body).length, 8);
});
