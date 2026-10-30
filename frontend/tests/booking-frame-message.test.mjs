import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import ts from 'typescript';
const source = await readFile(new URL('../src/app/shared/utils/booking-frame-message.ts', import.meta.url), 'utf8');
const { outputText } = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022 } });
const { readBookingFrameMessage, confirmationScrollTop } = await import('data:text/javascript;base64,' + Buffer.from(outputText).toString('base64'));
test('confirmation coordinates are bounded and the parsed message contains no guest data', () => {
    assert.deepEqual(readBookingFrameMessage({
        type: 'rota:booking:confirmed', height: 900, top: 270, cardHeight: 400, email: 'private@example.com'
    }), {
        type: 'rota:booking:confirmed', height: 900, top: 270, cardHeight: 400
    });
    for (const top of [
        -1, 900, 10000, Infinity, NaN, '270'
    ]) {
        assert.equal(readBookingFrameMessage({
            type: 'rota:booking:confirmed', height: 900, top, cardHeight: 400
        }), null);
    }
    for (const height of [
        0, 199, 10001, Infinity, NaN, '900'
    ]) {
        assert.equal(readBookingFrameMessage({
            type: 'rota:booking:confirmed', height, top: 0, cardHeight: 100
        }), null);
    }
    for (const value of [
        null, {}, [], 'resize', { type: 'other', height: 900 }
    ])
        assert.equal(readBookingFrameMessage(value), null);
});
test('ordinary height updates do not request confirmation focus', () => {
    assert.deepEqual(readBookingFrameMessage({ type: 'rota:premium-widget:resize', height: 500 }), { type: 'rota:premium-widget:resize', height: 500 });
});
test('target uses the current resized frame position and the fixed header instead of document bottom', () => {
    assert.equal(confirmationScrollTop(-400, 1800, 250, 90, 1000, 400), 1305);
    assert.equal(confirmationScrollTop(-50, 100, 10, 90, 1000, 400), 0);
});
test('full-card center matches the usable viewport center below the fixed header', () => {
    for (const [viewportHeight, headerHeight, cardHeight] of [[1000, 90, 400], [844, 70, 450], [900, 0, 360]]) {
        const cardDocumentTop = 2400;
        const scroll = confirmationScrollTop(800, 1400, 200, headerHeight, viewportHeight, cardHeight);
        const center = cardDocumentTop - scroll + cardHeight / 2;
        assert.equal(center, headerHeight + (viewportHeight - headerHeight) / 2);
    }
    // A card taller than the screen keeps its heading visible instead of hiding its top.
    assert.equal(confirmationScrollTop(800, 1400, 200, 90, 800, 1200), 2286);
});
test('invalid card measurements cannot cause arbitrary centering requests', () => {
    for (const cardHeight of [
        undefined, null, 0, -1, NaN, Infinity, '400', 901
    ]) {
        assert.equal(readBookingFrameMessage({
            type: 'rota:booking:confirmed', height: 900, top: 270, cardHeight
        }), null);
    }
});
