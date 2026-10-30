import { test, expect } from '@playwright/test';
async function widgetFor(page, hotel = 'lagos-beach') {
    await page.goto('/widget?hotel=' + hotel);
    const widget = page;
    await expect(widget.getByRole('heading', { name: hotel === 'lagos-beach' ? 'Lagos Beach Hotel' : 'Peri Stone Cave Hotel' })).toBeVisible();
    const inDate = new Date(Date.now() + 86400000 * 10).toISOString().slice(0, 10);
    const outDate = new Date(Date.now() + 86400000 * 12).toISOString().slice(0, 10);
    await widget.getByLabel('Giriş tarihi').fill(inDate);
    await widget.getByLabel('Çıkış tarihi').fill(outDate);
    return { widget, inDate };
}
async function login(page, username) {
    await page.goto('/crm');
    const logout = page.getByRole('button', { name: 'Çıkış yap' });
    await expect(page.getByRole('heading', { name: /Misafirler|Tekrar hoş geldiniz/ })).toBeVisible();
    if (await logout.count()) {
        await logout.click();
    }
    await page.getByLabel('Kullanıcı adı').fill(username);
    await page.getByLabel('Parola', { exact: true }).fill('DemoHotel2026!');
    await page.getByRole('button', { name: 'Giriş yap' }).click();
    await expect(page.getByRole('heading', { name: 'Misafirler', exact: true })).toBeVisible();
}
test('happy path: booking appears in its hotel CRM and stays isolated from the other hotel', async ({ page }) => {
    const { widget } = await widgetFor(page);
    await widget.getByRole('button', { name: 'Oda ara' }).click();
    await widget.getByRole('button', { name: 'Odayı seç' }).first().click();
    const email = `guest-${Date.now()}@example.com`;
    await widget.getByLabel('Ad', { exact: true }).fill('Deniz');
    await widget.getByLabel('Soyad', { exact: true }).fill('Test');
    await widget.getByLabel('E-posta', { exact: true }).fill(email);
    await widget.getByLabel('Telefon (ülke koduyla)').fill('+90555' + String(Date.now()).slice(-7));
    await widget.getByLabel('KVKK aydınlatma metnini okudum.').check();
    await widget.getByRole('button', { name: 'Rezervasyonu tamamla' }).click();
    await expect(widget.locator('.booking-code')).toHaveText(/^RT-/);
    await login(page, 'lagos');
    await page.getByLabel('E-posta', { exact: true }).fill(email);
    await expect(async () => {
        await page.getByRole('button', { name: 'Filtrele', exact: true }).click();
        await expect(page.getByRole('cell').filter({ hasText: email })).toBeVisible();
    }).toPass({ timeout: 15000 });
    await page.getByRole('button', { name: 'Profili aç' }).click();
    await expect(page.getByRole('heading', { name: 'Deniz Test' })).toBeVisible();
    await login(page, 'peri');
    await page.getByLabel('E-posta', { exact: true }).fill(email);
    await page.getByRole('button', { name: 'Filtrele', exact: true }).click();
    await expect(page.getByText('Misafir bulunamadı.', { exact: true })).toBeVisible();
});
test('session cookie restores the staff session after page reload', async ({ page }) => {
    await login(page, 'lagos');
    await page.reload();
    await expect(page.getByRole('heading', { name: 'Misafirler', exact: true })).toBeVisible();
});
test('edge case: an equal checkout date is rejected before searching availability', async ({ page }) => {
    const { widget, inDate } = await widgetFor(page);
    await widget.getByLabel('Çıkış tarihi').fill(inDate);
    await widget.getByRole('button', { name: 'Oda ara' }).click();
    await expect(widget.getByRole('alert')).toContainText('Çıkış tarihi girişten sonra olmalıdır.');
    await expect(widget.getByRole('button', { name: 'Odayı seç' })).toHaveCount(0);
});
test('edge case: privacy consent is required before submitting a booking', async ({ page }) => {
    const { widget } = await widgetFor(page);
    await widget.getByRole('button', { name: 'Oda ara' }).click();
    await widget.getByRole('button', { name: 'Odayı seç' }).first().click();
    await widget.getByLabel('Ad', { exact: true }).fill('Deniz');
    await widget.getByLabel('Soyad', { exact: true }).fill('Test');
    await widget.getByLabel('E-posta', { exact: true }).fill(`no-consent-${Date.now()}@example.com`);
    await widget.getByLabel('Telefon (ülke koduyla)').fill('+905551234567');
    await widget.getByRole('button', { name: 'Rezervasyonu tamamla' }).click();
    await expect(widget.getByRole('alert')).toContainText('Tüm zorunlu alanları doldurun.');
    await expect(widget.locator('.booking-code')).toHaveCount(0);
});
test('both hotel pages are Angular routes with a hotel-scoped widget', async ({ page }) => {
    await widgetFor(page, 'lagos-beach');
    await page.getByRole('link', { name: 'Peri Stone Cave Hotel' }).click();
    await expect(page).toHaveURL(/\/hotels\/peri-stone$/);
    await expect(page).toHaveTitle('Peri Stone Cave Hotel | Rota');
    await expect(page.frameLocator('iframe').getByRole('heading', { name: 'Peri Stone Cave Hotel' })).toBeVisible();
});
