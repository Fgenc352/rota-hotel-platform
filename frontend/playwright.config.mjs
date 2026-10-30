import { defineConfig } from '@playwright/test';

const baseURL = process.env.E2E_BASE_URL || 'http://localhost:4205';

export default defineConfig({
    testDir: './e2e',
    timeout: 45000,
    use: {
        baseURL,
        trace: 'retain-on-failure'
    },
    webServer: {
        command: 'npm start -- --host 127.0.0.1',
        url: baseURL,
        reuseExistingServer: true,
        timeout: 120000
    },
    reporter: [['list'], ['html', { open: 'never' }]]
});
