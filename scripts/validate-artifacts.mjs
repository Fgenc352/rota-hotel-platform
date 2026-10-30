import { readFile } from 'node:fs/promises';

async function readJson(path) {
    return JSON.parse(await readFile(new URL('../' + path, import.meta.url), 'utf8'));
}

function validateReferences(value, document) {
    if (value === null || typeof value !== 'object') {
        return 0;
    }
    let count = 0;
    for (const [key, child] of Object.entries(value)) {
        if (key !== '$ref') {
            count += validateReferences(child, document);
            continue;
        }
        if (typeof child !== 'string' || !child.startsWith('#/')) {
            throw new Error('Expected a local OpenAPI reference: ' + child);
        }
        const target = child.slice(2).split('/').reduce((node, part) => {
            const name = part.replace(/~1/g, '/').replace(/~0/g, '~');
            return node?.[name];
        }, document);
        if (target === undefined) {
            throw new Error('Broken OpenAPI reference: ' + child);
        }
        count++;
    }
    return count;
}

const spec = await readJson('doc/openapi.json');
const references = validateReferences(spec, spec);
if (references === 0) {
    throw new Error('No OpenAPI schema references checked.');
}
console.log('OpenAPI JSON:', references, 'local references resolved.');

const manifest = await readJson('frontend/package.json');
const lock = await readJson('frontend/package-lock.json');
for (const group of ['dependencies', 'devDependencies']) {
    for (const [name, version] of Object.entries(manifest[group] || {})) {
        if (lock.packages[''][group]?.[name] !== version ||
            lock.packages['node_modules/' + name]?.version !== version) {
            throw new Error('Package lock version mismatch: ' + name);
        }
    }
}
const packages = Object.entries(lock.packages).filter(([name]) => name !== '');
for (const [name, entry] of packages) {
    if (!entry.integrity || !entry.resolved) {
        throw new Error('Incomplete package lock entry: ' + name);
    }
}
console.log('Package lock:', packages.length, 'resolved packages; root versions match.');

// Catch saved requests pointing at removed endpoints before anyone runs the collection.
const collection = await readJson('postman/Rota-local.postman_collection.json').catch(error => {
    if (error.code !== 'ENOENT') throw error;
    console.log('Postman collection: absent; optional saved-request validation not run.');
    return null;
});
const operations = Object.entries(spec.paths).flatMap(([path, methods]) =>
    Object.keys(methods).map(method => ({ method: method.toUpperCase(), segments: path.split('/') })));
let requestCount = 0;
function validateRequests(items) {
    for (const item of items) {
        if (item.item) validateRequests(item.item);
        if (!item.request) continue;
        const request = item.request;
        const raw = typeof request.url === 'string' ? request.url : request.url.raw;
        const path = raw.replace(/^\{\{baseUrl\}\}/, '').split('?')[0];
        const segments = path.split('/');
        const matched = operations.some(operation => operation.method === request.method &&
            operation.segments.length === segments.length && operation.segments.every((segment, index) =>
                /^\{[^}]+\}$/.test(segment) || segment === segments[index]));
        if (!matched) throw new Error(`Saved request has no OpenAPI operation: ${request.method} ${path}`);
        requestCount++;
    }
}
if (collection) {
    validateRequests(collection.item);
    console.log('Postman collection:', requestCount, 'requests match documented operations.');
}
