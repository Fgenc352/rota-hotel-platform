import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';

const web = process.env.DEMO_WEB_URL || 'http://localhost:4200';
const pms = process.env.DEMO_PMS_URL || 'http://localhost:8090';
async function request(path, options = {}) {
    const response = await fetch(web + path, { signal: AbortSignal.timeout(15000), ...options });
    assert.ok(response.ok, `${options.method || 'GET'} ${path}: HTTP ${response.status}`);
    return response;
}
async function eventually(check, timeout = 30000) {
    const end = Date.now() + timeout;
    let last;
    while (Date.now() < end) {
        try { return await check(); } catch (error) { last = error; }
        await new Promise(resolve => setTimeout(resolve, 500));
    }
    throw last;
}
// The widget host runs in its own container and exposes public booking routes only.
const widget = process.env.DEMO_WIDGET_URL || 'http://localhost:4300';
const widgetFetch = path => fetch(widget + path, { signal: AbortSignal.timeout(15000) });
assert.ok((await widgetFetch('/healthz')).ok);
for (const edition of ['', '-premium', '-signature', '-atelier', '-maison', '-quiet']) {
    const loader = await widgetFetch(`/widget${edition}.js`);
    assert.ok(loader.ok);
    assert.match(await loader.text(), /hotelKey/);
    for (const hotel of ['lagos-beach', 'peri-stone']) {
        const page = await widgetFetch(`/widget${edition}?hotel=${hotel}`);
        assert.ok(page.ok);
        assert.match(await page.text(), /<app-root/);
    }
}
assert.equal((await (await widgetFetch('/api/public/hotels/peri-stone')).json()).key, 'peri-stone');
assert.equal((await widgetFetch('/login')).status, 404);
assert.equal((await widgetFetch('/api/crm/guests')).status, 404);
assert.match(await (await request('/signature/lagos-beach')).text(), /<app-root/);
assert.match(await (await request('/widget.js')).text(), /hotelKey/);
assert.equal((await (await request('/api/public/hotels/peri-stone')).json()).key, 'peri-stone');
const date = new Date();
date.setUTCDate(date.getUTCDate() + 80);
const checkIn = date.toISOString().slice(0, 10);
date.setUTCDate(date.getUTCDate() + 2);
const checkOut = date.toISOString().slice(0, 10);
const offers = await (await request(`/api/public/hotels/lagos-beach/availability?checkIn=${checkIn}&checkOut=${checkOut}&adults=2&children=0`)).json();
assert.ok(offers.length);
const body = {
    roomTypeId: offers[0].roomTypeId, checkIn, checkOut, adults: 2, children: 0,
    firstName: 'Container', lastName: 'Smoke', email: `smoke-${randomUUID()}@example.com`,
    phone: '+905551111119', privacyAccepted: true, marketingAllowed: false, serviceCodes: ['TRANSFER']
};
const headers = { 'Content-Type': 'application/json', 'Idempotency-Key': randomUUID(), Origin: web };
const receipt = await (await request('/api/public/hotels/lagos-beach/reservations', { method: 'POST', headers, body: JSON.stringify(body) })).json();
const replay = await (await request('/api/public/hotels/lagos-beach/reservations', { method: 'POST', headers, body: JSON.stringify(body) })).json();
assert.equal(replay.id, receipt.id);
const authorization = 'Basic ' + Buffer.from('lagos:DemoHotel2026!').toString('base64');
const login = await request('/api/auth/me', { headers: { Authorization: authorization } });
assert.equal((await login.json()).hotelKey, 'lagos-beach');
const cookie = login.headers.get('set-cookie')?.split(';')[0];
const csrf = login.headers.get('x-csrf-token');
assert.ok(cookie && csrf, 'Proxy must preserve session and CSRF response headers');
assert.equal((await (await request('/api/auth/me', { headers: { Cookie: cookie } })).json()).username, 'lagos');
const response = await fetch(web + '/api/crm/pms/board', { signal: AbortSignal.timeout(10000) });
assert.equal(response.status, 401);
const calendar = await (await request('/api/crm/pms/daily', { headers: { Cookie: cookie } })).json();
assert.match(calendar.date, /^\d{4}-\d{2}-\d{2}$/);
const missingCsrf = await fetch(web + '/api/auth/logout', { method: 'POST', headers: { Cookie: cookie }, signal: AbortSignal.timeout(10000) });
assert.equal(missingCsrf.status, 403);
await request('/api/auth/logout', { method: 'POST', headers: { Cookie: cookie, 'X-CSRF-TOKEN': csrf } });
const loggedOut = await fetch(web + '/api/auth/me', { headers: { Cookie: cookie }, signal: AbortSignal.timeout(10000) });
assert.equal(loggedOut.status, 401);

await eventually(async () => {
    const list = await (await fetch(`${pms}/api/reservations?q=${encodeURIComponent(receipt.code)}`, { signal: AbortSignal.timeout(5000) })).json();
    assert.equal(list.items[0]?.reservationId, receipt.id);
});
console.log('Container smoke passed: routes, widget loader, API proxy, booking/replay, session/CSRF, tenant authentication, daily PMS and outbox delivery.');
