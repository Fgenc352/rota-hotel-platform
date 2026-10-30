import { readdir, readFile, writeFile } from 'node:fs/promises';
import { gzipSync } from 'node:zlib';
import { initialCodeFiles, entryOutput } from './bundle-graph.mjs';
const root = new URL('../dist/rota/browser/', import.meta.url);
const { outputs } = JSON.parse(await readFile(new URL('../dist/rota/stats.json', import.meta.url), 'utf8'));
const names = (await readdir(root)).filter(name => /\.(js|css)$/.test(name));
const files = await Promise.all(names.map(async (name) => {
    const data = await readFile(new URL(name, root));
    return {
        name, bytes: data.length, gzipBytes: gzipSync(data).length
    };
}));
const metrics = new Map(files.map(file => [file.name, file]));
const summarize = selected => ({
    bytes: selected.reduce((sum, name) => sum + metrics.get(name).bytes, 0),
    gzipBytes: selected.reduce((sum, name) => sum + metrics.get(name).gzipBytes, 0)
});
// Angular stats also contain virtual component CSS that is already inlined in JS.
// Measure only physical output files, avoiding both missing files and double counting.
const emitted = Object.fromEntries(Object.entries(outputs).filter(([name]) => metrics.has(name)));
const boot = [entryOutput(emitted, 'src/main.ts'), ...Object.keys(emitted).filter(name => /polyfills.*\.js$/.test(name) || emitted[name].entryPoint?.startsWith('angular:styles/'))];
const editions = [
    ['Classic', 'widget.js', 'src/app/features/booking/pages/booking-page/booking-page.component.ts'],
    ['Premium', 'widget-premium.js', 'src/app/features/showcase/pages/premium-booking/premium-booking.component.ts'],
    ['Signature', 'widget-signature.js', 'src/app/features/showcase/pages/signature-booking/signature-booking.component.ts'],
    ['Atelier', 'widget-atelier.js', 'src/app/features/showcase/pages/atelier-booking/atelier-booking.component.ts'],
    ['Maison', 'widget-maison.js', 'src/app/features/showcase/pages/collection-booking/collection-booking.component.ts'],
    ['Quiet', 'widget-quiet.js', 'src/app/features/showcase/pages/collection-booking/collection-booking.component.ts']
];
const widgets = editions.map(([edition, loader, entry]) => {
    const selected = [...new Set([loader, ...initialCodeFiles(emitted, [...boot, entryOutput(emitted, entry)])])];
    return {
        edition, files: selected, ...summarize(selected)
    };
});
const report = {
    measuredAt: new Date().toISOString(),
    files,
    allEmittedCode: summarize(names),
    widgets,
    note: 'Cold-cache code only: loader + Angular bootstrap + selected widget eager imports + global CSS. Dynamic CRM/hotel/gallery routes excluded. HTML, photos, fonts, API responses and HTTP headers excluded. Gzip measured per file at Node default compression; this is not a captured browser transfer.'
};
await writeFile(new URL('bundle-report.json', root), JSON.stringify(report, null, 2) + '\n');
console.table(widgets.map(widget => ({
    widget: widget.edition, bytes: widget.bytes, gzipBytes: widget.gzipBytes, files: widget.files.length
})));
console.log(report.note);
if (process.argv.includes('--update-readme')) {
    const readme = new URL('../../README.md', import.meta.url);
    const text = await readFile(readme, 'utf8');
    const start = '<!-- widget-bundle:start -->';
    const end = '<!-- widget-bundle:end -->';
    if (!text.includes(start) || !text.includes(end))
        throw new Error('README bundle markers are missing.');
    const rows = widgets.map(widget => `| ${widget.edition} | ${widget.bytes} | ${widget.gzipBytes} |`).join('\n');
    const section = `${start}\nÖlçüm: ${report.measuredAt}. Değerler byte cinsindedir.\n\n| Widget | İlk JS + CSS + loader | Gzip |\n| --- | ---: | ---: |\n${rows}\n${end}`;
    await writeFile(readme, text.slice(0, text.indexOf(start)) + section + text.slice(text.indexOf(end) + end.length));
}
