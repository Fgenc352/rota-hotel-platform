import test from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import { readFile } from 'node:fs/promises';
import ts from 'typescript';
const source = await readFile(new URL('../src/app/shared/utils/room-gallery-message.ts', import.meta.url), 'utf8');
const { outputText } = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022 } });
const { readRoomGalleryRequest } = await import('data:text/javascript;base64,' + Buffer.from(outputText).toString('base64'));
const hostSource = await readFile(new URL('../public/widget-gallery.js', import.meta.url), 'utf8');
const request = {
    type: 'rota:gallery:open', roomId: '20000000-0000-0000-0000-000000000004', currency: 'EUR', language: 'en', selectable: true
};
test('gallery requests accept bounded identifiers and preferences, never HTML or prices', () => {
    assert.deepEqual(readRoomGalleryRequest({
        ...request, total: 1, html: '<script>'
    }), {
        roomId: request.roomId, currency: 'EUR', language: 'en', selectable: true
    });
    for (const fields of [
        { roomId: '../crm' }, { roomId: '<script>' }, { currency: 'USD' }, { language: 'javascript:' }, { selectable: 'true' }
    ]) {
        assert.equal(readRoomGalleryRequest({ ...request, ...fields }), null);
    }
});
function setup() {
    const messages = [];
    const scrolls = [];
    const listeners = new Map();
    function element(tag) {
        return {
            tag, style: {}, children: [], events: {}, contentWindow: {}, attributes: {},
            appendChild(child) {
                child.parent = this;
                this.children.push(child);
            },
            setAttribute(key, value) {
                this.attributes[key] = value;
            },
            addEventListener(key, callback) {
                this.events[key] = callback;
            },
            showModal() {
                this.open = true;
            }, close() {
                this.open = false;
            },
            remove() {
                this.parent.children.splice(this.parent.children.indexOf(this), 1);
            }
        };
    }
    const document = {
        documentElement: { style: { overflow: 'auto', scrollbarGutter: 'auto' } },
        head: element('head'), body: element('body'), createElement: element
    };
    const window = {
        scrollY: 1520, scrollTo: value => scrolls.push(value),
        addEventListener: (key, callback) => listeners.set(key, callback),
        removeEventListener: (key, callback) => {
            if (listeners.get(key) === callback)
                listeners.delete(key);
        }
    };
    vm.runInNewContext(hostSource, {
        window, document, URL, URLSearchParams
    });
    const frame = { contentWindow: { postMessage: (data, origin) => messages.push({ data, origin }) } };
    const open = (selectable = true) => window[Symbol.for('rota.roomGalleryHost')]({
        origin: 'https://booking.example', hotelKey: 'peri-stone', frame, request: { ...request, selectable }
    });
    return {
        document, window, messages, scrolls, listeners, frame, open
    };
}
test('host uses a separate viewport dialog, rejects foreign messages and preserves the booking frame', () => {
    const { document, messages, scrolls, listeners, frame, open } = setup();
    open();
    const dialog = document.body.children[0];
    const viewer = dialog.children[0];
    assert.equal(dialog.open, true);
    assert.equal(dialog.className, 'rota-room-gallery-host');
    assert.match(viewer.src, /^https:\/\/booking.example\/room-gallery\?hotel=peri-stone&room=/);
    assert.equal(document.documentElement.style.overflow, 'hidden');
    assert.equal(messages[0].data.type, 'rota:gallery:hosted');
    const event = {
        origin: 'https://booking.example', source: viewer.contentWindow, data: { type: 'rota:gallery:select', roomId: request.roomId }
    };
    listeners.get('message')({ ...event, origin: 'https://attacker.example' });
    listeners.get('message')({ ...event, source: frame.contentWindow });
    listeners.get('message')({ ...event, data: { ...event.data, roomId: 'other-room' } });
    assert.equal(document.body.children.length, 1);
    assert.equal(messages.length, 1);
    listeners.get('message')(event);
    assert.equal(document.body.children.length, 0);
    assert.equal(document.head.children.length, 0);
    assert.equal(document.documentElement.style.overflow, 'auto');
    assert.equal(document.documentElement.style.scrollbarGutter, 'auto');
    assert.equal(scrolls[0].top, 1520);
    assert.equal(messages[1].data.selected, true);
    assert.equal(messages[1].origin, 'https://booking.example');
    assert.equal(listeners.has('message'), false);
});
test('read-only gallery ignores selection and Escape closes without changing the booking', () => {
    const { document, messages, listeners, open } = setup();
    open(false);
    const dialog = document.body.children[0];
    listeners.get('message')({
        origin: 'https://booking.example', source: dialog.children[0].contentWindow,
        data: { type: 'rota:gallery:select', roomId: request.roomId }
    });
    assert.equal(messages.length, 1);
    let prevented = false;
    dialog.events.cancel({ preventDefault() {
            prevented = true;
        } });
    assert.equal(prevented, true);
    assert.equal(messages[1].data.selected, false);
    assert.equal(document.body.children.length, 0);
});
