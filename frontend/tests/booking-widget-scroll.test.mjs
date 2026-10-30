import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import ts from 'typescript';
function moduleUrl(source) {
    const { outputText } = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022 } });
    return 'data:text/javascript;base64,' + Buffer.from(outputText).toString('base64');
}
const root = '../src/app/shared/utils/';
const geometry = moduleUrl(await readFile(new URL(root + 'booking-frame-message.ts', import.meta.url), 'utf8'));
const source = (await readFile(new URL(root + 'booking-widget-scroll.ts', import.meta.url), 'utf8'))
    .replace("from './booking-frame-message'", `from '${geometry}'`);
const { bookingWidgetOffset, scrollToBookingWidget } = await import(moduleUrl(source));
test('booking links target the actual iframe, not the introductory section heading', () => {
    const scrolls = [];
    const frame = { getBoundingClientRect: () => ({ top: 1800, height: 640 }) };
    const widget = { querySelector: selector => selector === 'iframe' ? frame : null };
    const document = { querySelector: selector => selector.includes('showcase-booking-embed') ? widget : { getBoundingClientRect: () => ({ height: 80 }) } };
    const window = {
        scrollY: 300, innerHeight: 1000, matchMedia: () => ({ matches: false }), scrollTo: value => scrolls.push(value)
    };
    scrollToBookingWidget(document, window);
    assert.equal(scrolls[0].top, 1880);
    assert.equal(scrolls[0].behavior, 'smooth');
    assert.equal(bookingWidgetOffset(1000, 80, 640), 220);
    window.matchMedia = () => ({ matches: true });
    scrollToBookingWidget(document, window);
    assert.equal(scrolls[1].behavior, 'instant');
});
test('a tall widget keeps the beginning visible below the menu and a missing widget does nothing', () => {
    assert.equal(bookingWidgetOffset(844, 70, 1400), 94);
    let scrolled = false;
    scrollToBookingWidget({ querySelector: () => null }, { scrollTo: () => {
            scrolled = true;
        } });
    assert.equal(scrolled, false);
});
