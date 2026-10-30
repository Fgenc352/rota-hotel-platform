import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { stripTypeScriptTypes } from 'node:module';
const path = new URL('../src/app/shared/utils/display-date.ts', import.meta.url);
const source = stripTypeScriptTypes(await readFile(path, 'utf8'), { sourceUrl: path.href });
const { formatCalendarDate, formatHotelTimestamp, stayNights } = await import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'));
test('date-only values keep their calendar day across client timezones and have readable month names', () => {
    const before = process.env.TZ;
    try {
        process.env.TZ = 'Pacific/Honolulu';
        const west = formatCalendarDate('2027-02-10');
        process.env.TZ = 'Pacific/Auckland';
        assert.equal(formatCalendarDate('2027-02-10'), west);
        assert.match(west, /10.*Şub.*2027/);
        assert.match(formatCalendarDate('2027-02-10', 'en-GB'), /10.*Feb.*2027/);
    }
    finally {
        if (before === undefined)
            delete process.env.TZ;
        else
            process.env.TZ = before;
    }
});
test('invalid or missing dates are not rendered as misleading dates', () => {
    for (const value of [
        undefined, null, '', '2027-02-30', '2027-13-01', 'not-a-date'
    ]) {
        assert.equal(formatCalendarDate(value), '—');
    }
    assert.match(formatCalendarDate('2028-02-29'), /29/);
});
test('hotel timestamps and stay length use the intended timezone and date-only boundaries', () => {
    const value = formatHotelTimestamp('2027-02-10T22:30:00Z');
    assert.match(value, /11.*Şub.*2027/);
    assert.match(value, /01:30/);
    assert.equal(stayNights('2027-03-27', '2027-03-29'), 2);
    assert.equal(stayNights('2027-03-29', '2027-03-27'), 0);
    assert.equal(formatHotelTimestamp(undefined), '—');
});
