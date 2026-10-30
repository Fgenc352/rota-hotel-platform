import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { randomUUID } from 'node:crypto';
import { pmsServer } from './server.mjs';

test('PMS retries survive outages, deduplicate concurrent requests and persist across restart', async () => {
    const directory = await mkdtemp(join(tmpdir(), 'rota-pms-'));
    let server;
    try {
        server = await pmsServer({
            directory,
            failFirst: 1
        });
        await new Promise(r => server.listen(0, '127.0.0.1', r));
        const id = randomUUID();
        const send = () => fetch('http://127.0.0.1:' + server.address().port + '/reservations', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Idempotency-Key': id
            },
            body: JSON.stringify({ eventId: id })
        });
        assert.equal((await send()).status, 503);
        assert.deepEqual(await Promise.all([send(), send()].map(async (r) => (await r).status)), [200, 200]);
        assert.equal(await readFile(directory + '/accepted.jsonl', 'utf8'), id + '\n');
        await new Promise(r => server.close(r));
        server = await pmsServer({ directory });
        await new Promise(r => server.listen(0, '127.0.0.1', r));
        assert.equal((await send()).status, 200);
        assert.equal(await readFile(directory + '/accepted.jsonl', 'utf8'), id + '\n');
    }
    finally {
        if (server) {
            await new Promise(r => server.close(r));
        }
        await rm(directory, {
            recursive: true,
            force: true
        });
    }
});

test('dashboard endpoints show accepted and rejected requests and isolate simulation writes', async () => {
    const directory = await mkdtemp(join(tmpdir(), 'rota-pms-dashboard-'));
    const server = await pmsServer({ directory });
    try {
        await new Promise((resolve, reject) => {
            server.once('error', reject);
            server.listen(0, '127.0.0.1', resolve);
        });
        const base = 'http://127.0.0.1:' + server.address().port;
        assert.match(await (await fetch(base + '/')).text(), /PMS/);
        assert.equal(await (await fetch(base + '/health')).text(), 'ok');
        const control = (paused, origin) => fetch(base + '/api/simulation', {
            method: 'POST', headers: { 'Content-Type': 'application/json', ...(origin ? { Origin: origin } : {}) },
            body: JSON.stringify({ paused, delayMs: 0 })
        });
        assert.equal((await control(true, 'https://foreign.example')).status, 403);
        assert.equal((await control(true)).status, 200);
        const eventId = randomUUID();
        const send = () => fetch(base + '/reservations', {
            method: 'POST', headers: { 'Content-Type': 'application/json', 'Idempotency-Key': eventId },
            body: JSON.stringify({ eventId, code: 'RT-DASHBOARD' })
        });
        assert.equal((await send()).status, 503);
        await control(false);
        assert.equal((await send()).status, 200);
        assert.equal((await send()).status, 200);
        const status = await (await fetch(base + '/api/status')).json();
        assert.equal(status.accepted, 1);
        assert.equal(status.failed, 1);
        assert.equal(status.duplicates, 1);
        const list = await (await fetch(base + '/api/reservations?q=dashboard&page=0&size=20')).json();
        assert.equal(list.total, 1);
        assert.equal(list.items[0].code, 'RT-DASHBOARD');
        assert.equal((await (await fetch(base + '/api/requests')).json()).items.length, 3);
        assert.equal((await fetch(base + '/../server.mjs')).status, 404);
    } finally {
        await new Promise(resolve => server.close(resolve));
        await rm(directory, { recursive: true, force: true });
    }
});
