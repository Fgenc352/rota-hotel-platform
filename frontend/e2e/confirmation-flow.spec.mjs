import { test, expect } from '@playwright/test';
import { mockBooking } from './fixtures/showcase-api.mjs';
for (const route of [
    'signature/lagos-beach', 'signature/peri-stone', 'showcase/lagos-beach', 'showcase/peri-stone'
]) {
    for (const mobile of [false, true]) {
        test(`${route}: confirmation stays visible after the long service form shrinks (${mobile ? 'mobile' : 'desktop'})`, async ({ page }) => {
            await page.setViewportSize(mobile ? { width: 390, height: 844 } : { width: 1440, height: 1000 });
            await mockBooking(page);
            await page.goto('/' + route);
            const embed = page.locator('showcase-booking-embed iframe');
            await embed.scrollIntoViewIfNeeded();
            const frame = page.frameLocator('showcase-booking-embed iframe');
            await frame.getByRole('button', { name: 'Oda ara', exact: true }).click();
            await frame.getByRole('button', { name: /Odayı seç|Bu odayı seç/ }).first().click();
            await frame.getByLabel('Ad', { exact: true }).fill('Deniz');
            await frame.getByLabel('Soyad', { exact: true }).fill('Test');
            await frame.getByLabel('E-posta', { exact: true }).fill('confirmation@example.com');
            await frame.getByLabel('Telefon (ülke koduyla)', { exact: true }).fill('+905551234567');
            await frame.getByRole('checkbox', { name: 'Vale ve otopark', exact: true }).check();
            const image = frame.locator('.service-card').filter({ hasText: 'Vale ve otopark' }).locator('img');
            await expect(image).toHaveAttribute('src', `/images/services/${route.endsWith('peri-stone') ? 'peri' : 'lagos'}-valet.webp`);
            await expect.poll(() => image.evaluate(element => element.complete && element.naturalWidth > 0)).toBe(true);
            await frame.getByRole('checkbox', { name: /KVKK/ }).check();
            await frame.getByRole('button', { name: 'Rezervasyonu tamamla', exact: true }).click();
            await expect(frame.locator('.booking-code')).toHaveText('RT-SHOWCASE-01');
            // boundingBox uses the main frame viewport, so this catches host-page footer jumps.
            await expect.poll(async () => {
                const box = await frame.locator('.booking-code').boundingBox();
                return !!box && box.y >= 0 && box.y + box.height <= (mobile ? 844 : 1000);
            }).toBe(true);
            await expect(frame.getByRole('region', { name: 'Rezervasyon onayı' })).toBeFocused();
            await expect.poll(async () => {
                const card = await frame.getByRole('region', { name: 'Rezervasyon onayı' }).boundingBox();
                if (!card)
                    return Infinity;
                const viewport = mobile ? 844 : 1000;
                const header = await page.locator('.signature-header, .site-header, .peri-header').boundingBox();
                const headerHeight = header?.height || 0;
                const desiredCenter = headerHeight + (viewport - headerHeight) / 2;
                return Math.abs(card.y + card.height / 2 - desiredCenter);
            }).toBeLessThan(24);
            await expect.poll(() => embed.evaluate(element => Number.parseFloat(element.style.height))).toBeLessThan(1800);
        });
    }
}
test('standalone widget reveals its confirmation without a parent page', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await mockBooking(page);
    await page.goto('/widget-signature?hotel=peri-stone');
    await page.getByRole('button', { name: 'Oda ara', exact: true }).click();
    await page.getByRole('button', { name: /Odayı seç/ }).first().click();
    await page.getByLabel('Ad', { exact: true }).fill('Deniz');
    await page.getByLabel('Soyad', { exact: true }).fill('Test');
    await page.getByLabel('E-posta', { exact: true }).fill('standalone@example.com');
    await page.getByLabel('Telefon (ülke koduyla)', { exact: true }).fill('+905551234567');
    await page.getByRole('checkbox', { name: /KVKK/ }).check();
    await page.getByRole('button', { name: 'Rezervasyonu tamamla', exact: true }).click();
    await expect(page.locator('.booking-code')).toBeInViewport();
    await expect(page.getByRole('region', { name: 'Rezervasyon onayı' })).toBeFocused();
});
