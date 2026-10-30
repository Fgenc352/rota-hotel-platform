import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import ts from 'typescript';
const source = await readFile(new URL('../src/app/features/booking/presentation/service-visuals.ts', import.meta.url), 'utf8');
const { outputText } = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022 } });
const { serviceImage } = await import('data:text/javascript;base64,' + Buffer.from(outputText).toString('base64'));
test('each hotel has its own five distinct local service photographs', async () => {
    const hashes = new Set();
    for (const key of ['lagos-beach', 'peri-stone']) {
        for (const code of [
            'BREAKFAST', 'LUNCH', 'DINNER', 'VALET', 'TRANSFER'
        ]) {
            const path = serviceImage(key, code);
            const data = await readFile(new URL('../public' + path, import.meta.url));
            assert.equal(data.toString('ascii', 0, 4), 'RIFF');
            assert.equal(data.toString('ascii', 8, 12), 'WEBP');
            assert.ok(data.length < 150000, `${path} should be a compact lazy-loaded card asset`);
            hashes.add(createHash('sha256').update(data).digest('hex'));
        }
    }
    assert.equal(hashes.size, 10);
});
test('unknown hotel or service falls back to a text benefit instead of requesting a missing photo', () => {
    assert.equal(serviceImage('lagos-beach', 'WIFI'), null);
    assert.equal(serviceImage('unknown', 'VALET'), null);
    assert.equal(serviceImage('lagos-beach', '../private'), null);
});
