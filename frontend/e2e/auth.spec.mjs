import { test, expect } from '@playwright/test';
async function mockCrm(page, username = 'lagos') {
    let authenticated = false;
    const hotelName = username === 'peri' ? 'Peri Stone Cave Hotel' : 'Lagos Beach Hotel';
    await page.route('**/api/**', async (route) => {
        const request = route.request();
        const path = new URL(request.url()).pathname;
        if (path === '/api/auth/me' && request.headers().authorization) {
            authenticated = true;
        }
        if (!authenticated) {
            await route.fulfill({ status: 401, json: { code: 'UNAUTHENTICATED', detail: 'Giriş yapmanız gerekiyor.' } });
            return;
        }
        if (path === '/api/auth/logout') {
            expect(request.headers()['x-csrf-token']).toBe('test-csrf');
            authenticated = false;
            await route.fulfill({ status: 204 });
            return;
        }
        const body = path === '/api/auth/me'
            ? {
                username, hotelId: 'hotel-' + username, hotelKey: username === 'peri' ? 'peri-stone' : 'lagos-beach', hotelName, location: username === 'peri' ? 'Kapadokya' : 'Antalya'
            }
            : path === '/api/crm/segments' ? [] : {
                items: [], total: 0, page: 0, size: 20
            };
        await route.fulfill({
            status: 200, json: body, headers: { 'X-CSRF-TOKEN': 'test-csrf' }
        });
    });
    return () => {
        authenticated = false;
    };
}
async function submitLogin(page, username = 'lagos') {
    await page.getByLabel('Kullanıcı adı').fill(username);
    await page.getByLabel('Parola', { exact: true }).fill('DemoHotel2026!');
    await page.getByRole('button', { name: 'Giriş yap' }).click();
}
for (const destination of [
    '/crm/guests', '/crm/reservations', '/crm/operations', '/crm/pms'
]) {
    test(`anonymous access to ${destination} redirects before requesting CRM data`, async ({ page }) => {
        const crmRequests = [];
        page.on('request', request => {
            if (new URL(request.url()).pathname.startsWith('/api/crm/')) {
                crmRequests.push(request.url());
            }
        });
        await page.goto(destination);
        await expect(page).toHaveURL(url => url.pathname === '/login' && url.searchParams.get('returnUrl') === destination);
        await expect(page.getByRole('heading', { name: 'Tekrar hoş geldiniz.' })).toBeVisible();
        await expect(page.getByRole('navigation', { name: 'Ana menü' })).toHaveCount(0);
        expect(crmRequests).toEqual([]);
    });
}
test('incorrect credentials keep the user on the login route', async ({ page }) => {
    await page.route('**/api/auth/me', route => route.fulfill({
        status: 401,
        json: { code: 'UNAUTHENTICATED', detail: 'Giriş yapmanız gerekiyor.' }
    }));
    await page.goto('/crm/guests');
    await submitLogin(page);
    await expect(page.getByRole('alert')).toContainText('Giriş yapmanız gerekiyor.');
    await expect(page).toHaveURL(/\/login\?/);
    await expect(page.getByRole('navigation', { name: 'Ana menü' })).toHaveCount(0);
});
test('successful login restores the session after reload and direct URL navigation', async ({ page }) => {
    await mockCrm(page);
    await page.goto('/crm/reservations');
    await submitLogin(page);
    await expect(page).toHaveURL(/\/crm\/reservations$/);
    await expect(page.getByRole('heading', { name: 'Rezervasyonlar', exact: true })).toBeVisible();
    await page.getByRole('link', { name: 'Misafirler', exact: true }).click();
    await expect(page).toHaveURL(/\/crm\/guests$/);
    await expect(page.getByRole('heading', { name: 'Misafirler', exact: true })).toBeVisible();
    await page.reload();
    await expect(page).toHaveURL(/\/crm\/guests$/);
    await expect(page.getByRole('heading', { name: 'Misafirler', exact: true })).toBeVisible();
    await page.goto('/crm/reservations');
    await expect(page.getByRole('heading', { name: 'Rezervasyonlar', exact: true })).toBeVisible();
});
test('logout and browser back do not restore the protected CRM screen', async ({ page }) => {
    await mockCrm(page);
    await page.goto('/login');
    await submitLogin(page);
    await expect(page).toHaveURL(/\/crm\/guests$/);
    await page.getByRole('link', { name: 'Rezervasyonlar', exact: true }).click();
    await expect(page).toHaveURL(/\/crm\/reservations$/);
    await page.getByRole('button', { name: 'Çıkış yap' }).click();
    await expect(page).toHaveURL(/\/login$/);
    await page.goBack();
    await expect(page).toHaveURL(/\/login\?/);
    await expect(page.getByRole('navigation', { name: 'Ana menü' })).toHaveCount(0);
});
test('a CRM 401 clears the session and redirects to login', async ({ page }) => {
    const expire = await mockCrm(page);
    await page.goto('/login');
    await submitLogin(page);
    await expect(page.getByRole('heading', { name: 'Misafirler', exact: true })).toBeVisible();
    expire();
    await page.route('**/api/crm/guests?**', route => route.fulfill({
        status: 401,
        json: { code: 'UNAUTHENTICATED', detail: 'Giriş yapmanız gerekiyor.' }
    }));
    await page.getByRole('button', { name: '↻ Yenile' }).click();
    await expect(page).toHaveURL(/\/login\?/);
    await expect(page.getByRole('navigation', { name: 'Ana menü' })).toHaveCount(0);
});
for (const [username, hotelName] of [['lagos', 'Lagos Beach Hotel'], ['peri', 'Peri Stone Cave Hotel']]) {
    test(`${username} sees its own hotel name and browser title after reload`, async ({ page }) => {
        await mockCrm(page, username);
        await page.goto('/login');
        await submitLogin(page, username);
        await expect(page).toHaveTitle(hotelName + ' | Otel Yönetimi');
        await expect(page.locator('.page-top')).toContainText(hotelName);
        await page.reload();
        await expect(page).toHaveTitle(hotelName + ' | Otel Yönetimi');
        await expect(page.locator('.page-top')).toContainText(hotelName);
    });
}
test('a successful password check without a persisted session stays on login with a clear error', async ({ page }) => {
    await page.route('**/api/auth/me', async (route) => {
        const basic = route.request().headers().authorization;
        if (!basic) {
            await route.fulfill({ status: 401, json: { detail: 'Giriş yapmanız gerekiyor.' } });
            return;
        }
        await route.fulfill({
            status: 200,
            json: {
                username: 'lagos', hotelId: 'hotel-lagos', hotelKey: 'lagos-beach', hotelName: 'Lagos Beach Hotel', location: 'Antalya'
            },
            headers: { 'X-CSRF-TOKEN': 'test-csrf' }
        });
    });
    await page.goto('/login');
    await submitLogin(page);
    await expect(page.getByRole('alert')).toContainText('Giriş doğrulandı fakat oturum kurulamadı.');
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('navigation', { name: 'Ana menü' })).toHaveCount(0);
});
test('an outdated backend response tells the user to restart instead of opening CRM', async ({ page }) => {
    await page.route('**/api/auth/me', async (route) => {
        if (!route.request().headers().authorization) {
            await route.fulfill({ status: 401, json: { detail: 'Giriş yapmanız gerekiyor.' } });
            return;
        }
        await route.fulfill({ status: 200, json: { username: 'lagos', hotelId: 'hotel-lagos' } });
    });
    await page.goto('/login');
    await submitLogin(page);
    await expect(page.getByRole('alert')).toContainText('Backend oturum sürümü güncel değil.');
    await expect(page).toHaveURL(/\/login$/);
});
test('server unavailability is shown separately from an anonymous session', async ({ page }) => {
    await page.route('**/api/auth/me', route => route.fulfill({
        status: 503, json: { detail: 'Backend unavailable' }
    }));
    await page.goto('/login');
    await expect(page.getByRole('alert')).toContainText('Oturum doğrulanamadı.');
    await expect(page.getByRole('alert')).not.toContainText('parola yanlış');
    await expect(page).toHaveURL(/\/login$/);
});
test('login uses a shared brand presentation and never lists or prefills hotel accounts', async ({ page }) => {
    await page.route('**/api/auth/me', route => route.fulfill({
        status: 401, json: { code: 'UNAUTHENTICATED', detail: 'Giriş yapmanız gerekiyor.' }
    }));
    await page.goto('/login');
    const username = page.getByLabel('Kullanıcı adı', { exact: true });
    await expect(username).toHaveValue('');
    await expect(page.getByLabel('Parola', { exact: true })).toHaveValue('');
    await expect(page.getByRole('button', { name: 'Giriş yap', exact: true })).toBeDisabled();
    await expect(page.getByRole('button', { name: /Lagos Beach Hotel|Peri Stone Cave Hotel/ })).toHaveCount(0);
    await expect(page.getByText('Lagos Beach Hotel', { exact: true })).toHaveCount(0);
    await expect(page.getByText('Peri Stone Cave Hotel', { exact: true })).toHaveCount(0);
    const photo = page.locator('.login-image');
    const source = await photo.getAttribute('src');
    await username.fill('some.staff.member');
    await expect(photo).toHaveAttribute('src', source);
    await page.getByLabel('Parola', { exact: true }).fill('example-password');
    await page.getByRole('button', { name: 'Parolayı göster', exact: true }).click();
    await expect(page.getByLabel('Parola', { exact: true })).toHaveAttribute('type', 'text');
    await page.getByRole('button', { name: 'Parolayı gizle', exact: true }).click();
    await expect(page.getByLabel('Parola', { exact: true })).toHaveAttribute('type', 'password');
    await page.setViewportSize({ width: 390, height: 844 });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});
