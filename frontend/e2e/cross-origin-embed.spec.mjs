import { test, expect } from '@playwright/test';
import { readFile } from 'node:fs/promises';
// Only the unrelated host HTML is intercepted. Widget code and hotel APIs run for real.
for (const [index, hotel] of ['lagos-beach', 'peri-stone'].entries()) {
    test(`${hotel}: a foreign plain-HTML host books through the real script and API`, async ({ page, baseURL }) => {
        const html = await readFile(new URL('./fixtures/widget-host.html', import.meta.url), 'utf8').then(html => html.replaceAll('http://localhost:4200', baseURL));
        await page.route('http://127.0.0.1:8081/**', route => route.fulfill({ contentType: 'text/html', body: html }));
        const apiRequests = [];
        page.on('request', request => {
            if (new URL(request.url()).pathname.startsWith('/api/public/'))
                apiRequests.push(request.url());
        });
        await page.goto('http://127.0.0.1:8081/');
        await expect(page.locator('main iframe')).toHaveCount(2);
        const container = page.locator('main iframe').nth(index);
        await container.scrollIntoViewIfNeeded();
        const widget = page.frameLocator('main iframe').nth(index);
        await expect(widget.getByLabel('Giriş tarihi', { exact: true })).toBeVisible();
        await expect(widget.getByLabel('Giriş tarihi', { exact: true })).toHaveValue(/^\d{4}-\d{2}-\d{2}$/);
        await expect(page.locator('body')).toHaveCSS('background-color', 'rgb(134, 38, 38)');
        await expect(page.getByRole('button', { name: 'Host sitenin düğmesi' })).toHaveCSS('background-color', 'rgb(255, 0, 255)');
        const start = new Date();
        start.setUTCDate(start.getUTCDate() + 90);
        const checkIn = start.toISOString().slice(0, 10);
        start.setUTCDate(start.getUTCDate() + 2);
        await widget.getByLabel('Giriş tarihi', { exact: true }).fill(checkIn);
        await widget.getByLabel('Çıkış tarihi', { exact: true }).fill(start.toISOString().slice(0, 10));
        await widget.getByRole('button', { name: 'Oda ara', exact: true }).click();
        await widget.getByRole('button', { name: 'Odayı seç', exact: true }).first().click();
        await widget.getByLabel('Ad', { exact: true }).fill('Deniz');
        await widget.getByLabel('Soyad', { exact: true }).fill('Embed');
        await widget.getByLabel('E-posta', { exact: true }).fill(`embed-${hotel}-${Date.now()}@example.com`);
        await widget.getByLabel('Telefon (ülke koduyla)', { exact: true }).fill('+905551111118');
        await widget.getByRole('checkbox', { name: /KVKK/ }).check();
        await widget.getByRole('button', { name: 'Rezervasyonu tamamla', exact: true }).click();
        await expect(widget.getByRole('region', { name: 'Rezervasyon onayı' })).toContainText('RT-');
        expect(apiRequests.length).toBeGreaterThan(0);
        expect(apiRequests.every(url => new URL(url).origin === new URL(baseURL).origin)).toBe(true);
        await expect(page).toHaveURL('http://127.0.0.1:8081/');
    });
}
