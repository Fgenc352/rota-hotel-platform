import { test, expect } from '@playwright/test';
import { mockBooking, roomFor } from './fixtures/showcase-api.mjs';
for (const edition of [
    'showcase', 'signature', 'atelier', 'maison', 'quiet'
]) {
    for (const key of ['lagos-beach', 'peri-stone']) {
        test(`${edition}/${key}: public links resolve to real hotel sections and room cards select their own photos`, async ({ page }) => {
            await mockBooking(page);
            const first = roomFor(key);
            const second = {
                ...first, id: '20000000-0000-0000-0000-000000000099', name: 'Test Aile Süiti', capacity: 4,
                areaSquareMeters: 55, images: [...first.images].reverse()
            };
            await page.route('**/api/public/hotels/' + key + '/rooms', route => route.fulfill({ json: [first, second] }));
            const path = '/' + edition + '/' + key;
            await page.goto(path + '?source=link-check');
            await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
            const audit = await page.locator('a[href]').evaluateAll(links => links.map(link => {
                const url = new URL(link.href);
                const samePage = url.pathname === location.pathname;
                return {
                    href: url.href,
                    staff: /^\/(login|crm)(\/|$)/.test(url.pathname),
                    fragment: samePage && url.hash ? decodeURIComponent(url.hash.slice(1)) : null,
                    targetExists: !samePage || !url.hash || !!document.getElementById(decodeURIComponent(url.hash.slice(1))),
                    hasQuery: !samePage || url.searchParams.get('source') === 'link-check'
                };
            }));
            expect(audit.filter(link => link.staff)).toEqual([]);
            expect(audit.filter(link => !link.targetExists)).toEqual([]);
            expect(audit.filter(link => link.fragment && !link.hasQuery)).toEqual([]);
            if (edition !== 'showcase') {
                const tabs = page.getByRole('tablist', { name: 'Oda tipleri' });
                await expect(tabs.getByRole('tab')).toHaveCount(2);
                const next = tabs.getByRole('tab', { name: second.name, exact: true });
                await expect(next.locator('img')).toHaveAttribute('src', second.images[0]);
                await next.click();
                await expect(next).toHaveAttribute('aria-selected', 'true');
                const panel = page.getByRole('tabpanel');
                await expect(panel).toHaveAttribute('id', 'room-panel-' + second.id);
                await expect(panel.locator('.gallery-trigger img, .room-gallery-trigger img')).toHaveAttribute('src', second.images[0]);
                await next.press('Home');
                await expect(tabs.getByRole('tab', { name: first.name, exact: true })).toBeFocused();
                await expect(panel).toHaveAttribute('id', 'room-panel-' + first.id);
                await tabs.getByRole('tab', { name: first.name, exact: true }).press('ArrowLeft');
                await expect(next).toHaveAttribute('aria-selected', 'true');
            }
            else {
                await expect(page.locator('showcase-room-collection .room-card')).toHaveCount(2);
            }
            await page.setViewportSize({ width: 390, height: 844 });
            expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
        });
    }
}
