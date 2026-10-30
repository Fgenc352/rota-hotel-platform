import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { stripTypeScriptTypes } from 'node:module';

const path = new URL('../src/app/features/crm/utils/note-deletion.ts', import.meta.url);
const source = stripTypeScriptTypes(await readFile(path, 'utf8'), { sourceUrl: path.href });
const { noteDeletionState } = await import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'));
const created = Date.parse('2026-10-30T12:00:00Z');
const note = { id: 'note', body: 'Note', author: 'lagos', created_at: new Date(created).toISOString() };
const day = 24 * 60 * 60 * 1000;

test('the author can delete until the millisecond before the twenty-four-hour deadline', () => {
    assert.equal(noteDeletionState(note, 'lagos', created), 'available');
    assert.equal(noteDeletionState(note, 'lagos', created + day - 1), 'available');
});

test('at and after twenty-four hours deletion is unavailable', () => {
    assert.equal(noteDeletionState(note, 'lagos', created + day), 'expired');
    assert.equal(noteDeletionState(note, 'lagos', created + day + 1), 'expired');
});

test('another author, a missing identity or invalid/future creation time cannot enable deletion', () => {
    assert.equal(noteDeletionState(note, 'peri', created + 1000), 'not-author');
    assert.equal(noteDeletionState(note, '', created + 1000), 'not-author');
    assert.equal(noteDeletionState({ ...note, created_at: 'invalid' }, 'lagos', created), 'expired');
    assert.equal(noteDeletionState(note, 'lagos', created - 1), 'expired');
});

test('UTC offsets do not turn a twenty-four-hour window into a calendar-day rule', () => {
    const offsetNote = { ...note, created_at: '2026-10-30T15:00:00+03:00' };
    assert.equal(noteDeletionState(offsetNote, 'lagos', created + day - 1), 'available');
    assert.equal(noteDeletionState(offsetNote, 'lagos', created + day), 'expired');
});
