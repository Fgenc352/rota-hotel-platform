import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';
const migration = await readFile(new URL('../../src/main/resources/db/changelog/changes/012-dedicated-room-galleries.sql', import.meta.url), 'utf8');
test('all five room galleries have five distinct existing images with no repeated file contents', async () => {
    const galleries = [...migration.matchAll(/images='([^']+)'::jsonb/g)].map(match => JSON.parse(match[1]));
    assert.equal(galleries.length, 5);
    galleries.forEach(gallery => assert.equal(gallery.length, 5));
    const paths = galleries.flat();
    assert.equal(new Set(paths).size, 25);
    assert.equal(paths.some(path => path.includes('-hero.')), false);
    const hashes = [];
    for (const path of paths) {
        assert.match(path, /^\/images\/showcase\/[a-z-]+\.webp$/);
        const data = await readFile(new URL('../public' + path, import.meta.url));
        assert.equal(data.toString('ascii', 8, 12), 'WEBP');
        hashes.push(createHash('sha256').update(data).digest('hex'));
    }
    assert.equal(new Set(hashes).size, 25);
});
