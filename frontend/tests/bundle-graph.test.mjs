import test from 'node:test';
import assert from 'node:assert/strict';
import { initialCodeFiles, entryOutput } from '../scripts/bundle-graph.mjs';
test('widget first-load measurement follows eager imports once and excludes lazy CRM routes', () => {
    const outputs = {
        'main.js': { entryPoint: 'src/main.ts', imports: [{ path: 'core.js', kind: 'import-statement' }, { path: 'crm.js', kind: 'dynamic-import' }] },
        'widget.js': { entryPoint: 'widget-page.ts', imports: [{ path: 'core.js', kind: 'import-statement' }, { path: 'gallery.js', kind: 'dynamic-import' }] },
        'core.js': { imports: [{ path: 'main.js', kind: 'import-statement' }] },
        'styles.css': { imports: [{ path: 'font.woff2', kind: 'url-token' }] },
        'crm.js': {}, 'gallery.js': {}, 'font.woff2': {}
    };
    assert.equal(entryOutput(outputs, 'widget-page.ts'), 'widget.js');
    assert.deepEqual(initialCodeFiles(outputs, ['main.js', 'widget.js', 'styles.css']), [
        'core.js', 'main.js', 'styles.css', 'widget.js'
    ]);
});
test('an unknown entry or missing root cannot silently produce an undersized report', () => {
    assert.throws(() => entryOutput({}, 'missing.ts'), /missing/);
    assert.throws(() => initialCodeFiles({}, ['missing.js']), /missing/);
});
