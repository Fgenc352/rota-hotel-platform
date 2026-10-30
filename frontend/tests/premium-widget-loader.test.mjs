import test from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import { readFile } from 'node:fs/promises';
const source = await readFile(new URL('../public/widget-premium.js', import.meta.url), 'utf8');
const legacySource = await readFile(new URL('../public/widget.js', import.meta.url), 'utf8');
const atelierSource = await readFile(new URL('../public/widget-atelier.js', import.meta.url), 'utf8');
const collectionSources = Object.fromEntries(await Promise.all(['maison', 'quiet'].map(async (edition) => [edition, await readFile(new URL('../public/widget-' + edition + '.js', import.meta.url), 'utf8')])));
const signatureSource = await readFile(new URL('../public/widget-signature.js', import.meta.url), 'utf8');
function embed(key, signature = false, legacy = false, reducedMotion = false, atelier = false, collection = null) {
    class HTMLScriptElement {
    }
    const changes = [];
    const listeners = {};
    const errors = [];
    const scrolls = [];
    const script = Object.assign(new HTMLScriptElement(), {
        dataset: { hotelKey: key }, src: 'https://booking.example/' + (collection ? 'widget-' + collection + '.js' : atelier ? 'widget-atelier.js' : signature ? 'widget-signature.js' : 'widget-premium.js'),
        insertAdjacentElement(_position, frame) {
            this.frame = frame;
        }
    });
    const frame = {
        contentWindow: {}, attributes: {},
        getBoundingClientRect: () => ({ top: -400 }),
        style: { setProperty: (...args) => changes.push(args) },
        setAttribute(name, value) {
            this.attributes[name] = value;
        }
    };
    const browserWindow = {
        addEventListener: (name, callback) => {
            listeners[name] = callback;
        },
        requestAnimationFrame: callback => callback(), scrollY: 1800, innerHeight: 1000,
        matchMedia: () => ({ matches: reducedMotion }), scrollTo: options => scrolls.push(options)
    };
    vm.runInNewContext(collection ? collectionSources[collection] : atelier ? atelierSource : legacy ? legacySource : signature ? signatureSource : source, {
        document: { currentScript: script, createElement: () => frame },
        window: browserWindow,
        HTMLScriptElement, URL, encodeURIComponent, console: { error: value => errors.push(value) }
    });
    return {
        frame, script, changes, listeners, errors, scrolls, browserWindow
    };
}
test('premium loader embeds the selected hotel while keeping the original widget route separate', () => {
    const { script, frame } = embed('peri-stone');
    assert.equal(script.frame, frame);
    assert.equal(frame.src, 'https://booking.example/widget-premium?hotel=peri-stone');
    assert.match(frame.attributes.sandbox, /allow-forms/);
});
test('resize only accepts bounded heights from its own frame and booking origin', () => {
    const { listeners, frame, changes } = embed('lagos-beach');
    const event = {
        origin: 'https://booking.example', source: frame.contentWindow,
        data: { type: 'rota:premium-widget:resize', height: 900 }
    };
    listeners.message({ ...event, origin: 'https://attacker.example' });
    listeners.message({ ...event, source: {} });
    for (const height of [
        NaN, Infinity, -1, 0, 10001, '900'
    ]) {
        listeners.message({ ...event, data: { ...event.data, height } });
    }
    assert.equal(changes.length, 0);
    listeners.message(event);
    assert.deepEqual(changes[0], ['height', '904px', 'important']);
});
test('invalid hotel keys do not insert a frame', () => {
    for (const key of [
        undefined, '', '../crm', '<script>', 'a'.repeat(81)
    ]) {
        const { script, errors } = embed(key);
        assert.equal(script.frame, undefined);
        assert.equal(errors.length, 1);
    }
});
test('signature loader uses its own route and rejects resize messages from a foreign origin', () => {
    const { frame, listeners, changes } = embed('peri-stone', true);
    assert.equal(frame.src, 'https://booking.example/widget-signature?hotel=peri-stone');
    const data = { type: 'rota:premium-widget:resize', height: 900 };
    listeners.message({
        origin: 'https://attacker.example', source: frame.contentWindow, data
    });
    assert.equal(changes.length, 0);
    listeners.message({
        origin: 'https://booking.example', source: frame.contentWindow, data
    });
    assert.deepEqual(changes[0], ['height', '904px', 'important']);
});
for (const [variant, signature, legacy, atelier, collection] of [
    [
        'premium', false, false, false
    ], [
        'signature', true, false, false
    ], [
        'legacy', false, true, false
    ], [
        'atelier', false, false, true
    ], [
        'maison', false, false, false, 'maison'
    ], [
        'quiet', false, false, false, 'quiet'
    ]
]) {
    test(`${variant} confirmation reveals the card and rejects foreign or invalid messages`, () => {
        const { frame, changes, listeners, scrolls } = embed('peri-stone', signature, legacy, false, atelier, collection);
        const event = {
            origin: 'https://booking.example', source: frame.contentWindow,
            data: {
                type: 'rota:booking:confirmed', height: 900, top: 250, cardHeight: 400
            }
        };
        listeners.message({ ...event, origin: 'https://attacker.example' });
        listeners.message({ ...event, source: {} });
        for (const top of [
            -1, 900, NaN, Infinity, '250'
        ]) {
            listeners.message({ ...event, data: { ...event.data, top } });
        }
        assert.equal(scrolls.length, 0);
        assert.equal(changes.length, 0);
        listeners.message(event);
        assert.deepEqual(changes[0], ['height', '904px', 'important']);
        assert.equal(scrolls[0].top, 1350);
        assert.equal(scrolls[0].behavior, 'smooth');
        listeners.message({ ...event, data: { type: 'rota:premium-widget:resize', height: 850 } });
        assert.equal(scrolls.length, 1);
    });
}
test('reduced-motion preference reveals the confirmation without smooth animation', () => {
    const { frame, listeners, scrolls } = embed('lagos-beach', true, false, true);
    listeners.message({
        origin: 'https://booking.example', source: frame.contentWindow,
        data: {
            type: 'rota:booking:confirmed', height: 900, top: 250, cardHeight: 400
        }
    });
    assert.equal(scrolls[0].behavior, 'instant');
});
for (const [variant, signature, legacy, atelier, collection] of [
    [
        'premium', false, false, false
    ], [
        'signature', true, false, false
    ], [
        'legacy', false, true, false
    ], [
        'atelier', false, false, true
    ], [
        'maison', false, false, false, 'maison'
    ], [
        'quiet', false, false, false, 'quiet'
    ]
]) {
    test(`${variant} opens only a validated room gallery from its own booking frame`, () => {
        const { frame, listeners, browserWindow, changes } = embed('peri-stone', signature, legacy, false, atelier, collection);
        const calls = [];
        browserWindow[Symbol.for('rota.roomGalleryHost')] = value => calls.push(value);
        const event = {
            origin: 'https://booking.example', source: frame.contentWindow,
            data: {
                type: 'rota:gallery:open', roomId: '20000000-0000-0000-0000-000000000004', currency: 'TRY', language: 'tr', selectable: true
            }
        };
        listeners.message({ ...event, origin: 'https://attacker.example' });
        listeners.message({ ...event, source: {} });
        listeners.message({ ...event, data: { ...event.data, roomId: '../crm' } });
        listeners.message({ ...event, data: { ...event.data, currency: 'USD' } });
        listeners.message({ ...event, data: { ...event.data, selectable: 'true' } });
        assert.equal(calls.length, 0);
        listeners.message(event);
        assert.equal(calls.length, 1);
        assert.equal(calls[0].frame, frame);
        assert.equal(calls[0].hotelKey, 'peri-stone');
        assert.equal(calls[0].origin, 'https://booking.example');
        assert.equal(changes.length, 0);
    });
}
test('atelier loader has its own route and does not replace the signature widget', () => {
    const { frame, script } = embed('lagos-beach', false, false, false, true);
    assert.equal(script.frame, frame);
    assert.equal(frame.src, 'https://booking.example/widget-atelier?hotel=lagos-beach');
    assert.match(frame.attributes.sandbox, /allow-forms/);
});
for (const edition of ['maison', 'quiet']) {
    test(`${edition} loader uses its own route and rejects invalid keys`, () => {
        for (const key of ['lagos-beach', 'peri-stone']) {
            const { frame } = embed(key, false, false, false, false, edition);
            assert.equal(frame.src, 'https://booking.example/widget-' + edition + '?hotel=' + key);
        }
        for (const key of ['', '../crm', '<script>']) {
            const { script, errors } = embed(key, false, false, false, false, edition);
            assert.equal(script.frame, undefined);
            assert.equal(errors.length, 1);
        }
    });
}
