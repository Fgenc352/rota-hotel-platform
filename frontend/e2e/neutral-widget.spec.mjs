import { test, expect } from '@playwright/test';
import { mockBooking } from './fixtures/showcase-api.mjs';
for (const key of ['lagos-beach', 'peri-stone']) {
    for (const variant of [
        'widget', 'widget-premium', 'widget-signature', 'widget-atelier', 'widget-maison', 'widget-quiet'
    ]) {
        test(`${variant}/${key}: a colored host keeps its own background and the widget stays neutral`, async ({ page }) => {
            await page.setViewportSize({ width: 1440, height: 1000 });
            await mockBooking(page);
            const loader = variant === 'widget' ? 'widget.js' : variant + '.js';
            await page.route('**/neutral-widget-host', route => route.fulfill({
                contentType: 'text/html', body: `<!doctype html><html><head><style>
                    body{margin:0;background:#b91c1c;color:white;font-family:Arial,sans-serif;}
                    main{max-width:1050px;margin:40px auto;padding:24px;}
                    </style></head><body><main><h1>Host website</h1>
                    <script src="/${loader}" data-hotel-key="${key}"></script></main></body></html>`
            }));
            await page.goto('/neutral-widget-host');
            const widget = page.frameLocator('main > iframe');
            const root = widget.locator('.widget-shell, .premium-booking, .signature-widget, .atelier-widget, .collection-widget');
            await expect(widget.getByLabel('Giriş tarihi', { exact: true })).toHaveValue(/^\d{4}-\d{2}-\d{2}$/);
            await expect(page.locator('body')).toHaveCSS('background-color', 'rgb(185, 28, 28)');
            await expect(widget.locator('html')).toHaveCSS('background-color', 'rgba(0, 0, 0, 0)');
            await expect(widget.locator('body')).toHaveCSS('background-color', 'rgba(0, 0, 0, 0)');
            await expect(root).toHaveCSS('background-color', 'rgb(255, 255, 255)');
            const name = key === 'peri-stone' ? 'Mağara Oda' : 'Standart';
            await widget.getByRole('button', { name: 'Oda ara', exact: true }).click();
            await expect(widget.getByRole('heading', { name, exact: true })).toBeVisible();
            if (variant !== 'widget') {
                await widget.locator('.room-photo').first().click();
                const gallery = page.frameLocator('.rota-room-gallery-host iframe');
                const dialog = gallery.getByRole('dialog');
                await expect(dialog).toHaveClass(/neutral-gallery/);
                await expect(dialog).not.toHaveClass(/cave-gallery/);
                await expect(dialog).toHaveCSS('background-color', 'rgb(255, 255, 255)');
                await dialog.getByRole('button', { name: 'Oda detayını kapat', exact: true }).click();
                await expect(page.locator('.rota-room-gallery-host')).toHaveCount(0);
            }
            await widget.getByRole('button', { name: 'Odayı seç', exact: true }).first().click();
            const services = widget.locator('.service-options');
            await expect(services).not.toHaveClass(/cave-services/);
            await expect(services).toHaveCSS('background-color', 'rgb(255, 255, 255)');
            await page.evaluate(() => {
                document.body.style.background = '#111';
            });
            await expect(root).toHaveCSS('background-color', 'rgb(255, 255, 255)');
            await expect(page.locator('body')).toHaveCSS('background-color', 'rgb(17, 17, 17)');
        });
    }
}
