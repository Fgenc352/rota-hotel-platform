import test from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import { readFile } from 'node:fs/promises';
const source = await readFile(new URL('../public/widget.js', import.meta.url), 'utf8');
function run(key, sourceUrl = 'https://booking.example/widget.js') {
    class HTMLScriptElement {
    }
    const script = Object.assign(new HTMLScriptElement(), {
        dataset: { hotelKey: key },
        src: sourceUrl,
        insertAdjacentElement(_where, element) {
            this.frame = element;
        }
    });
    const errors = [];
    const document = {
        currentScript: script,
        createElement() {
            return {
                style: {},
                attributes: {},
                setAttribute(k, v) {
                    this.attributes[k] = v;
                }
            };
        }
    };
    vm.runInNewContext(source, {
        document,
        window: { addEventListener() {
            } },
        HTMLScriptElement,
        URL,
        encodeURIComponent,
        console: { error: e => errors.push(e) }
    });
    return {
        script,
        errors
    };
}
test('embeds hotel-scoped widget on booking origin, independent of host page', () => {
    const { script } = run('peri-stone');
    assert.equal(script.frame.src, 'https://booking.example/widget?hotel=peri-stone');
    assert.match(script.frame.attributes.sandbox, /allow-forms/);
    assert.equal(script.frame.referrerPolicy, 'strict-origin-when-cross-origin');
});
test('rejects invalid or missing hotel key without creating a frame', () => {
    for (const key of [
        undefined, '', '../crm', 'x\" onload=alert(1)', 'a'.repeat(81)
    ]) {
        const { script, errors } = run(key);
        assert.equal(script.frame, undefined);
        assert.equal(errors.length, 1);
    }
});
