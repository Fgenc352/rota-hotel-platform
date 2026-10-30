import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { parseArgs } from 'node:util';
import { pmsStore } from './store.mjs';
import { receiveReservation } from './reservation-handler.mjs';

const assets = new Map([
    ['/', ['index.html', 'text/html; charset=utf-8']],
    ['/styles.css', ['styles.css', 'text/css; charset=utf-8']],
    ['/app.mjs', ['app.mjs', 'text/javascript; charset=utf-8']]
]);
const json = (res, status, body) => {
    res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
    res.end(JSON.stringify(body));
};
async function requestBody(req) {
    let data = '';
    for await (const chunk of req) {
        data += chunk;
        if (Buffer.byteLength(data) > 16384) throw Object.assign(new Error('Request too large'), { status: 413 });
    }
    return JSON.parse(data);
}

export async function pmsServer({ directory = fileURLToPath(new URL('./data/', import.meta.url)), failFirst = 0, delayMs = 0, crmUrl = process.env.PMS_CRM_URL || 'http://localhost:4205/crm/pms' } = {}) {
    const store = await pmsStore(directory);
    const simulation = { paused: false, delayMs };
    let requests = 0;
    return createServer(async (req, res) => {
        res.setHeader('X-Content-Type-Options', 'nosniff');
        res.setHeader('Referrer-Policy', 'same-origin');
        try {
            const url = new URL(req.url, 'http://localhost');
            if (req.method === 'GET' && url.pathname === '/health') {
                res.writeHead(200, { 'Content-Type': 'text/plain' });
                res.end('ok');
            } else if (req.method === 'GET' && assets.has(url.pathname)) {
                const [name, type] = assets.get(url.pathname);
                const content = await readFile(new URL('./public/' + name, import.meta.url));
                res.writeHead(200, {
                    'Content-Type': type,
                    'Content-Security-Policy': "default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; img-src 'self' data:; frame-ancestors 'none'"
                });
                res.end(name === 'index.html' ? content.toString().replace('http://localhost:4205/crm/pms', crmUrl) : content);
            } else if (req.method === 'GET' && url.pathname === '/api/status') {
                json(res, 200, { ...store.summary(), simulation });
            } else if (req.method === 'GET' && url.pathname === '/api/reservations') {
                const query = (url.searchParams.get('q') || '').trim().toLocaleLowerCase('tr');
                const hotel = url.searchParams.get('hotel') || '';
                const status = url.searchParams.get('status') || '';
                const page = Math.max(0, Math.floor(Number(url.searchParams.get('page')) || 0));
                const size = Math.min(100, Math.max(1, Math.floor(Number(url.searchParams.get('size')) || 20)));
                const all = store.reservations().filter(row => (!hotel || row.hotelId === hotel) && (!status || row.status === status)
                    && (!query || [row.code, row.reservationId, row.eventId].some(value => String(value || '').toLocaleLowerCase('tr').includes(query))));
                json(res, 200, { items: all.slice(page * size, (page + 1) * size), total: all.length, page, size });
            } else if (req.method === 'GET' && /^\/api\/reservations\/[a-f0-9-]{36}\/history$/i.test(url.pathname)) {
                const id = url.pathname.split('/')[3];
                const hotel = url.searchParams.get('hotel');
                json(res, 200, { items: store.history(hotel, id) });
            } else if (req.method === 'GET' && url.pathname === '/api/requests') {
                json(res, 200, { items: store.attempts().slice(0, 10) });
            } else if (req.method === 'POST' && url.pathname === '/api/simulation') {
                // Local control only. No CORS and no cross-origin browser writes.
                if ((req.headers.origin && req.headers.origin !== 'http://' + req.headers.host)
                    || !req.headers['content-type']?.startsWith('application/json')) {
                    json(res, 403, { error: 'Same-origin JSON request required' });
                    return;
                }
                const body = await requestBody(req);
                if (typeof body.paused !== 'boolean' || !Number.isInteger(body.delayMs) || body.delayMs < 0 || body.delayMs > 10000) {
                    json(res, 400, { error: 'Invalid simulation settings' });
                    return;
                }
                Object.assign(simulation, { paused: body.paused, delayMs: body.delayMs });
                json(res, 200, { simulation });
            } else if (req.method === 'POST' && url.pathname === '/reservations') {
                const result = await receiveReservation({ body: await requestBody(req), headers: req.headers, store, simulation, failFirst, requestNumber: ++requests });
                json(res, result.status, result.body);
            } else {
                json(res, 404, { error: 'Not found' });
            }
        } catch (error) {
            json(res, error.status || (error instanceof SyntaxError ? 400 : 500), { error: error.status === 413 ? 'Request too large' : 'PMS request failed' });
        }
    });
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
    const { values } = parseArgs({ options: { 'fail-first': { type: 'string', default: '0' }, 'delay-ms': { type: 'string', default: '0' } } });
    const failFirst = Number(values['fail-first']);
    const delayMs = Number(values['delay-ms']);
    if (!Number.isInteger(failFirst) || failFirst < 0 || !Number.isFinite(delayMs) || delayMs < 0) throw new Error('fail-first and delay-ms must be non-negative numbers.');
    const server = await pmsServer({ failFirst, delayMs });
    const port = Number(process.env.PMS_PORT || 8095);
    if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('PMS_PORT must be between 1 and 65535.');
    server.on('error', error => {
        console.error(`PMS could not start: ${error.code}. Check port ${port}.`);
        process.exitCode = 1;
    });
    server.listen(port, process.env.PMS_HOST || '127.0.0.1', () => console.log(`PMS mock & dashboard: http://localhost:${port}`));
    for (const signal of ['SIGTERM', 'SIGINT']) process.on(signal, () => server.close(() => process.exit(0)));
}
