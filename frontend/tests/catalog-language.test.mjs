import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import ts from 'typescript';
const source = await readFile(new URL('../src/app/features/booking/presentation/catalog-translations.ts', import.meta.url), 'utf8');
const { outputText } = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022 } });
const { catalogText, serviceText } = await import('data:text/javascript;base64,' + Buffer.from(outputText).toString('base64'));
test('all five Lagos and Peri room names switch to English and back without mutating canonical data', () => {
    for (const [name, translated] of [
        ['Standart', 'Standard Room'], ['Deluxe Deniz Manzaralı', 'Sea View Deluxe'],
        ['Aile Suiti', 'Family Suite'], ['Mağara Oda', 'Cave Room'], ['Teras Süit', 'Terrace Suite']
    ]) {
        const room = Object.freeze({ name });
        assert.equal(catalogText(room.name, 'en'), translated);
        assert.equal(catalogText(room.name, 'tr'), name);
        assert.equal(room.name, name);
    }
    assert.equal(catalogText('1 çift kişilik yatak + 2 tek kişilik yatak', 'en'), '1 double bed + 2 single beds');
    assert.equal(catalogText('Özel teras', 'en'), 'Private terrace');
    assert.equal(catalogText('Custom suite', 'en'), 'Custom suite');
});
test('service names and descriptions use stable codes and switch back to original Turkish', () => {
    for (const [code, name] of [
        ['BREAKFAST', 'Breakfast'], ['LUNCH', 'Lunch'], ['DINNER', 'Dinner'],
        ['VALET', 'Valet and parking'], ['TRANSFER', 'Airport transfer'], ['WIFI', 'Wi-Fi']
    ]) {
        assert.equal(serviceText(code, 'name', 'Orijinal isim', 'en'), name);
        assert.notEqual(serviceText(code, 'description', 'Türkçe açıklama', 'en'), 'Türkçe açıklama');
        assert.equal(serviceText(code, 'name', 'Orijinal isim', 'tr'), 'Orijinal isim');
        assert.equal(serviceText(code, 'description', 'Türkçe açıklama', 'tr'), 'Türkçe açıklama');
    }
    assert.equal(serviceText('CUSTOM', 'name', 'Custom spa', 'en'), 'Custom spa');
});
