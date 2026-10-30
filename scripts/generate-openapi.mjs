import { writeFile } from 'node:fs/promises';

const baseUrl = process.argv[2] || 'http://localhost:8080';
const response = await fetch(new URL('/v3/api-docs', baseUrl), {
    signal: AbortSignal.timeout(15000)
});
if (!response.ok) {
    throw new Error(`OpenAPI export failed: HTTP ${response.status}`);
}
const document = await response.json();
const output = new URL('../doc/openapi.json', import.meta.url);
await writeFile(output, JSON.stringify(document, null, 2) + '\n');
console.log(`Exported doc/openapi.json with ${Object.keys(document.paths || {}).length} paths.`);
