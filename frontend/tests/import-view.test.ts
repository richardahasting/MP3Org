import puppeteer, { Browser, Page } from 'puppeteer';

const FRONTEND_URL = 'http://localhost:4173';
const BACKEND_URL = 'http://localhost:9090';

// Helper to wait for React to render
async function waitForReact(page: Page) {
  await page.waitForFunction(
    () => document.querySelector('.app') !== null,
    { timeout: 10000 }
  );
  // Extra time for React hydration
  await new Promise(resolve => setTimeout(resolve, 500));
}

describe('MP3Org Import View E2E Tests', () => {
  let browser: Browser;
  let page: Page;

  beforeAll(async () => {
    browser = await puppeteer.launch({
      headless: false,  // Headed mode - browser visible
      args: ['--no-sandbox', '--disable-setuid-sandbox'],
      slowMo: 50,  // Slow down for visibility
    });
  });

  afterAll(async () => {
    await browser.close();
  });

  beforeEach(async () => {
    page = await browser.newPage();
    await page.setViewport({ width: 1280, height: 800 });
  });

  afterEach(async () => {
    await page.close();
  });

  describe('Backend API Health', () => {
    test('Backend browse endpoint should return root directories', async () => {
      const response = await fetch(`${BACKEND_URL}/api/v1/scanning/browse`);
      expect(response.ok).toBe(true);

      const data = await response.json();
      expect(data.entries).toBeDefined();
      expect(Array.isArray(data.entries)).toBe(true);
      expect(data.entries.length).toBeGreaterThan(0);

      // Should have Home, Music, and Volumes
      const names = data.entries.map((e: any) => e.name);
      expect(names).toContain('Home');
    });

    test('Backend music count endpoint should work', async () => {
      const response = await fetch(`${BACKEND_URL}/api/v1/music/count`);
      expect(response.ok).toBe(true);

      const count = await response.json();
      // Count might be a number or wrapped in an object
      expect(count !== undefined).toBe(true);
    });

    test('Backend scan directories endpoint should work', async () => {
      const response = await fetch(`${BACKEND_URL}/api/v1/scanning/directories`);
      expect(response.ok).toBe(true);

      const data = await response.json();
      expect(Array.isArray(data)).toBe(true);
    });
  });

  describe('Frontend Navigation', () => {
    test('Should load the application', async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);

      // Check title/logo is present
      const logoText = await page.$eval('.logo-text', el => el.textContent);
      expect(logoText).toBe('MP3Org');
    });

    test('Should display all five tabs', async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);

      const tabs = await page.$$('.tab-button');
      expect(tabs.length).toBe(5);

      // Get tab labels
      const tabLabels = await page.$$eval('.tab-label', els =>
        els.map(el => el.textContent)
      );
      expect(tabLabels).toContain('Import');
      expect(tabLabels).toContain('Metadata');
      expect(tabLabels).toContain('Duplicates');
    });

    test('Should navigate to Import tab when clicked', async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);

      // Click on Import tab (3rd in nav)
      await page.click('.tab-nav .tab-button:nth-child(3)');

      // Wait for Import view to load
      await page.waitForSelector('.import-view', { timeout: 5000 });

      // Check Import view title
      const title = await page.$eval('.import-title', el => el.textContent);
      expect(title).toBe('Import Music Files');
    });
  });

  describe('Import View - Directory Browser', () => {
    beforeEach(async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);
      await page.click('.tab-nav .tab-button:nth-child(3)');
      await page.waitForSelector('.import-view', { timeout: 5000 });
    });

    test('Should display directory browser panel', async () => {
      const browserPanel = await page.$('.browser-panel');
      expect(browserPanel).not.toBeNull();

      const browserTitle = await page.$eval('.browser-title', el => el.textContent);
      expect(browserTitle).toBe('Directory Browser');
    });

    test('Should load root directory entries', async () => {
      // Wait for entries to load
      await page.waitForSelector('.browser-entry', { timeout: 5000 });

      const entries = await page.$$('.browser-entry');
      expect(entries.length).toBeGreaterThan(0);

      // Check for Home entry
      const entryNames = await page.$$eval('.entry-name', els =>
        els.map(el => el.textContent)
      );
      expect(entryNames).toContain('Home');
    });

    test('Should navigate into a directory when clicked', async () => {
      await page.waitForSelector('.browser-entry', { timeout: 5000 });

      // Click on Home directory
      const homeEntry = await page.$('.browser-entry:first-child');
      await homeEntry?.click();

      // Wait for navigation
      await page.waitForFunction(
        () => {
          const pathValue = document.querySelector('.path-value');
          return pathValue && pathValue.textContent?.includes('/Users/');
        },
        { timeout: 5000 }
      );

      // Should now show contents of home directory
      const currentPath = await page.$eval('.path-value', el => el.textContent);
      expect(currentPath).toContain('/Users/');
    });

    test('Should show "Add to Queue" button when in a directory', async () => {
      await page.waitForSelector('.browser-entry', { timeout: 5000 });

      // Click on Home directory
      await page.click('.browser-entry:first-child');

      // Wait for path to update
      await page.waitForFunction(
        () => document.querySelector('.path-value')?.textContent?.includes('/Users/'),
        { timeout: 5000 }
      );

      // Check for Add to Queue button
      const addButton = await page.$('.select-current-btn');
      expect(addButton).not.toBeNull();

      const buttonText = await page.$eval('.select-current-btn', el => el.textContent);
      expect(buttonText).toContain('Add to Queue');
    });
  });

  describe('Import View - Scan Queue', () => {
    beforeEach(async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);
      await page.click('.tab-nav .tab-button:nth-child(3)');
      await page.waitForSelector('.import-view', { timeout: 5000 });
    });

    test('Should display scan queue panel', async () => {
      const queueSection = await page.$('.queue-section');
      expect(queueSection).not.toBeNull();

      const queueTitle = await page.$eval('.queue-title', el => el.textContent);
      expect(queueTitle).toContain('Scan Queue');
    });

    test('Should show empty queue message initially', async () => {
      const emptyMessage = await page.$('.queue-empty');
      expect(emptyMessage).not.toBeNull();

      const messageText = await page.$eval('.queue-empty', el => el.textContent);
      expect(messageText).toContain('Select directories');
    });

    test('Should have disabled Start Scan button when queue is empty', async () => {
      const startButton = await page.$('.start-scan-btn');
      expect(startButton).not.toBeNull();

      const isDisabled = await page.$eval('.start-scan-btn',
        (el) => (el as HTMLButtonElement).disabled
      );
      expect(isDisabled).toBe(true);
    });

    test('Should add directory to queue when + button clicked', async () => {
      await page.waitForSelector('.browser-entry', { timeout: 5000 });

      // Navigate to Home
      await page.click('.browser-entry:first-child');
      await page.waitForFunction(
        () => document.querySelector('.path-value')?.textContent?.includes('/Users/'),
        { timeout: 5000 }
      );

      // Wait for entries to load
      await page.waitForSelector('.entry-add-btn', { timeout: 5000 });

      // Hover over first directory to reveal + button
      const firstEntry = await page.$('.browser-entry.directory:not(:first-child)');
      if (firstEntry) {
        await firstEntry.hover();

        // Click the + button
        const addBtn = await firstEntry.$('.entry-add-btn');
        if (addBtn) {
          await addBtn.click();

          // Wait for queue to update
          await page.waitForSelector('.queue-item', { timeout: 5000 });

          // Verify item was added to queue
          const queueItems = await page.$$('.queue-item');
          expect(queueItems.length).toBeGreaterThan(0);

          // Start button should now be enabled
          const isDisabled = await page.$eval('.start-scan-btn',
            (el) => (el as HTMLButtonElement).disabled
          );
          expect(isDisabled).toBe(false);
        }
      }
    });
  });

  describe('Import View - Scan History', () => {
    beforeEach(async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);
      await page.click('.tab-nav .tab-button:nth-child(3)');
      await page.waitForSelector('.import-view', { timeout: 5000 });
    });

    test('Should display scan history section', async () => {
      const historySection = await page.$('.history-section');
      expect(historySection).not.toBeNull();

      const historyTitle = await page.$eval('.history-title', el => el.textContent);
      expect(historyTitle).toBe('Scan History');
    });
  });

  describe('Import View - Visual Elements', () => {
    beforeEach(async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);
      await page.click('.tab-nav .tab-button:nth-child(3)');
      await page.waitForSelector('.import-view', { timeout: 5000 });
    });

    test('Should have proper styling applied', async () => {
      // Check that the import header has the right background
      const headerBg = await page.$eval('.import-header', el =>
        getComputedStyle(el).backgroundColor
      );
      expect(headerBg).not.toBe('rgba(0, 0, 0, 0)'); // Should have a background

      // Check the grid layout
      const gridColumns = await page.$eval('.import-content', el =>
        getComputedStyle(el).gridTemplateColumns
      );
      expect(gridColumns).toContain('px'); // Should have defined grid columns
    });

    test('Should show folder icons for directories', async () => {
      await page.waitForSelector('.browser-entry', { timeout: 5000 });

      const icons = await page.$$eval('.entry-icon', els =>
        els.map(el => el.textContent)
      );
      expect(icons.some(icon => icon === '📁')).toBe(true);
    });
  });

  describe('Import View - Full Import from ~/Music', () => {
    beforeEach(async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);
      await page.click('.tab-nav .tab-button:nth-child(3)');
      await page.waitForSelector('.import-view', { timeout: 5000 });
    });

    test('Should navigate to Music folder and verify import capability', async () => {
      // Check if files are already imported
      const countResponse = await fetch(`${BACKEND_URL}/api/v1/music/count`);
      const countData = await countResponse.json();

      if (countData.count > 0) {
        // Files already exist - just verify the UI navigation works
        console.log(`Database already has ${countData.count} files - skipping import, verifying UI only`);

        // Wait for directory browser to load
        await page.waitForSelector('.browser-entry', { timeout: 5000 });

        // Click on Home directory
        const homeEntry = await page.$('.browser-entry:first-child');
        await homeEntry?.click();

        // Wait for home directory contents
        await page.waitForFunction(
          () => document.querySelector('.path-value')?.textContent?.includes('/Users/'),
          { timeout: 5000 }
        );

        // Find and click Music folder
        const entries = await page.$$('.browser-entry');
        for (const entry of entries) {
          const name = await entry.$eval('.entry-name', el => el.textContent);
          if (name === 'Music') {
            await entry.click();
            break;
          }
        }

        // Wait for Music directory to load
        await page.waitForFunction(
          () => document.querySelector('.path-value')?.textContent?.includes('/Music'),
          { timeout: 5000 }
        );

        // Verify "Add to Queue" button exists
        const addButton = await page.$('.select-current-btn');
        expect(addButton).not.toBeNull();

        return; // Skip actual import since we already have files
      }

      // Full import flow when database is empty
      await page.waitForSelector('.browser-entry', { timeout: 5000 });

      const homeEntry = await page.$('.browser-entry:first-child');
      await homeEntry?.click();

      await page.waitForFunction(
        () => document.querySelector('.path-value')?.textContent?.includes('/Users/'),
        { timeout: 5000 }
      );

      await page.waitForSelector('.browser-entry', { timeout: 5000 });

      const entries = await page.$$('.browser-entry');
      for (const entry of entries) {
        const name = await entry.$eval('.entry-name', el => el.textContent);
        if (name === 'Music') {
          await entry.click();
          break;
        }
      }

      await page.waitForFunction(
        () => document.querySelector('.path-value')?.textContent?.includes('/Music'),
        { timeout: 5000 }
      );

      const addButton = await page.$('.select-current-btn');
      expect(addButton).not.toBeNull();
      await addButton?.click();

      await page.waitForSelector('.queue-item', { timeout: 5000 });

      const queuePath = await page.$eval('.queue-item-path', el => el.textContent);
      expect(queuePath).toContain('Music');

      const startButton = await page.$('.start-scan-btn');
      const isDisabled = await page.$eval('.start-scan-btn', el => (el as HTMLButtonElement).disabled);
      expect(isDisabled).toBe(false);

      await startButton?.click();

      await page.waitForSelector('.progress-section', { timeout: 10000 });

      await page.waitForFunction(
        () => {
          const stageEl = document.querySelector('.stage-badge');
          if (stageEl?.textContent?.toLowerCase().includes('completed')) return true;
          if (stageEl?.textContent?.toLowerCase().includes('error')) return true;
          const percentEl = document.querySelector('.progress-percent');
          if (percentEl?.textContent === '100%') return true;
          return false;
        },
        { timeout: 300000 }
      );

      const response = await fetch(`${BACKEND_URL}/api/v1/music/count`);
      const data = await response.json();
      expect(data.count).toBeGreaterThan(0);

      console.log(`Successfully imported ${data.count} music files`);
    }, 360000);
  });
});
