import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import ts from 'typescript';
const source = await readFile(new URL('../src/app/features/booking/presentation/weekend-rate-summary.ts', import.meta.url), 'utf8');
const { outputText } = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022 } });
const { weekendRateSummary, groupedStayRates } = await import('data:text/javascript;base64,' + Buffer.from(outputText).toString('base64'));
const offer = {
    baseNightlyPrice: 3200, weekendMultiplier: 1.15, total: 10560,
    nights: [
        { date: '2026-11-06', price: 3200 },
        { date: '2026-11-07', price: 3680 },
        { date: '2026-11-08', price: 3680 }
    ]
};
test('explains the server quoted weekend portion without changing the quote', () => {
    assert.deepEqual(weekendRateSummary(offer), {
        percent: 15, nights: 2, surcharge: 960
    });
    assert.equal(offer.total, 10560);
});
test('weekday-only stays explicitly have no surcharge', () => {
    assert.deepEqual(weekendRateSummary({ ...offer, nights: [offer.nights[0]] }), {
        percent: 15, nights: 0, surcharge: 0
    });
});
test('does not invent tariff information for an older server or a room without a markup', () => {
    assert.equal(weekendRateSummary({ nights: offer.nights }), null);
    assert.equal(weekendRateSummary({ ...offer, weekendMultiplier: 1 }), null);
});
test('uses rounded nightly quotes for surcharge cents', () => {
    assert.deepEqual(weekendRateSummary({
        ...offer, baseNightlyPrice: 100.01, weekendMultiplier: 1.5,
        nights: [{ date: '2026-11-07', price: 150.02 }, { date: '2026-11-08', price: 150.02 }]
    }), {
        percent: 50, nights: 2, surcharge: 100.02
    });
});
test('weekend detection uses hotel calendar dates independently of the browser timezone', () => {
    const previous = process.env.TZ;
    try {
        for (const timezone of ['America/Los_Angeles', 'Pacific/Auckland', 'Europe/Istanbul']) {
            process.env.TZ = timezone;
            assert.deepEqual(weekendRateSummary(offer), {
                percent: 15, nights: 2, surcharge: 960
            });
        }
    }
    finally {
        if (previous === undefined)
            delete process.env.TZ;
        else
            process.env.TZ = previous;
    }
});
test('groups a mixed stay into readable quantity times nightly price lines', () => {
    assert.deepEqual(groupedStayRates(offer), [
        {
            kind: 'weekday', price: 3200, nights: 1, total: 3200
        },
        {
            kind: 'weekend', price: 3680, nights: 2, total: 7360
        }
    ]);
});
test('one hundred nights use two tariff groups instead of one hundred rows', () => {
    const nights = Array.from({ length: 100 }, (_, index) => {
        const date = new Date(Date.UTC(2026, 10, 2 + index));
        const weekend = [0, 6].includes(date.getUTCDay());
        return { date: date.toISOString().slice(0, 10), price: weekend ? 3680 : 3200 };
    });
    const groups = groupedStayRates({ ...offer, nights });
    assert.equal(groups.length, 2);
    assert.equal(groups.reduce((sum, group) => sum + group.nights, 0), 100);
    assert.equal(groups.reduce((sum, group) => sum + group.total, 0), nights.reduce((sum, night) => sum + night.price, 0));
});
test('distinct quoted prices are preserved and zero markup does not invent a surcharge', () => {
    const groups = groupedStayRates({
        ...offer, weekendMultiplier: 1, nights: [
            { date: '2026-11-02', price: 100.01 }, { date: '2026-11-03', price: 100.01 },
            { date: '2026-11-04', price: 110 }, { date: '2026-11-07', price: 100.01 }
        ]
    });
    assert.deepEqual(groups, [
        {
            kind: 'weekday', price: 100.01, nights: 2, total: 200.02
        },
        {
            kind: 'weekday', price: 110, nights: 1, total: 110
        },
        {
            kind: 'weekend', price: 100.01, nights: 1, total: 100.01
        }
    ]);
});
