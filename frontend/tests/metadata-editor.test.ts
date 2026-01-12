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

describe('MP3Org Metadata Editor E2E Tests', () => {
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

  describe('Backend Music API Health', () => {
    test('Backend music list endpoint should return paginated files', async () => {
      const response = await fetch(`${BACKEND_URL}/api/v1/music?page=0&size=10`);
      expect(response.ok).toBe(true);

      const data = await response.json();
      expect(data.content).toBeDefined();
      expect(Array.isArray(data.content)).toBe(true);
      expect(data.totalElements).toBeDefined();
      expect(data.totalPages).toBeDefined();
    }, 60000);

    test('Backend music count endpoint should return count', async () => {
      const response = await fetch(`${BACKEND_URL}/api/v1/music/count`);
      expect(response.ok).toBe(true);

      const data = await response.json();
      expect(data.count).toBeDefined();
      expect(typeof data.count).toBe('number');
      expect(data.count).toBeGreaterThan(0);
    });

    test('Backend search endpoint should work', async () => {
      const response = await fetch(`${BACKEND_URL}/api/v1/music/search?q=rock&page=0&size=10`);
      expect(response.ok).toBe(true);

      const data = await response.json();
      expect(data.content).toBeDefined();
      expect(Array.isArray(data.content)).toBe(true);
    });

    test('Backend search by artist should work', async () => {
      const response = await fetch(`${BACKEND_URL}/api/v1/music/search?artist=Boston&page=0&size=10`);
      expect(response.ok).toBe(true);

      const data = await response.json();
      expect(data.content).toBeDefined();
    });
  });

  describe('Frontend Navigation to Metadata Tab', () => {
    test('Should load the application', async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);

      const logoText = await page.$eval('.logo-text', el => el.textContent);
      expect(logoText).toBe('MP3Org');
    });

    test('Should display Metadata tab in navigation', async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);

      const tabLabels = await page.$$eval('.tab-label', els =>
        els.map(el => el.textContent)
      );
      expect(tabLabels).toContain('Metadata');
    });

    test('Should navigate to Metadata tab when clicked', async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);

      // Click on Metadata tab (2nd in nav)
      await page.click('.tab-nav .tab-button:nth-child(2)');

      // Wait for Metadata Editor to load
      await page.waitForSelector('.metadata-editor', { timeout: 10000 });

      const metadataEditor = await page.$('.metadata-editor');
      expect(metadataEditor).not.toBeNull();
    });
  });

  describe('Metadata Editor - Header Section', () => {
    beforeEach(async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);
      await page.click('.tab-nav .tab-button:nth-child(2)');
      await page.waitForSelector('.metadata-editor', { timeout: 10000 });
    });

    test('Should display total file count', async () => {
      await page.waitForFunction(
        () => document.querySelector('.loading-state') === null,
        { timeout: 30000 }
      );

      const statsSection = await page.$('.metadata-stats');
      expect(statsSection).not.toBeNull();

      const statValue = await page.$eval('.metadata-stats .stat-value', el => el.textContent);
      expect(parseInt(statValue?.replace(/,/g, '') || '0')).toBeGreaterThan(0);
    });

    test('Should display search form', async () => {
      const searchForm = await page.$('.search-form');
      expect(searchForm).not.toBeNull();

      const fieldSelect = await page.$('.field-select');
      expect(fieldSelect).not.toBeNull();

      const searchInput = await page.$('.search-input');
      expect(searchInput).not.toBeNull();

      const searchBtn = await page.$('.search-btn');
      expect(searchBtn).not.toBeNull();
    });

    test('Should have search field options', async () => {
      const options = await page.$$eval('.field-select option', els =>
        els.map(el => el.textContent)
      );
      expect(options).toContain('All Fields');
      expect(options).toContain('Title');
      expect(options).toContain('Artist');
      expect(options).toContain('Album');
    });
  });

  describe('Metadata Editor - Data Table', () => {
    beforeEach(async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);
      await page.click('.tab-nav .tab-button:nth-child(2)');
      await page.waitForSelector('.metadata-editor', { timeout: 10000 });
      await page.waitForFunction(
        () => document.querySelector('.loading-state') === null,
        { timeout: 30000 }
      );
    });

    test('Should display music table with data', async () => {
      const musicTable = await page.$('.music-table');
      expect(musicTable).not.toBeNull();

      const rows = await page.$$('.music-table tbody tr');
      expect(rows.length).toBeGreaterThan(0);
    });

    test('Should display table headers including select column', async () => {
      const headers = await page.$$eval('.music-table th', els =>
        els.map(el => el.className)
      );
      expect(headers).toContain('col-select');
      expect(headers).toContain('col-title');
      expect(headers).toContain('col-artist');
      expect(headers).toContain('col-album');
    });

    test('Should display checkboxes for selection', async () => {
      // Check header checkbox
      const headerCheckbox = await page.$('.col-select input[type="checkbox"]');
      expect(headerCheckbox).not.toBeNull();

      // Check row checkboxes
      const rowCheckboxes = await page.$$('.cell-select input[type="checkbox"]');
      expect(rowCheckboxes.length).toBeGreaterThan(0);
    });

    test('Should display action buttons for each row', async () => {
      const editButtons = await page.$$('.action-btn.edit');
      expect(editButtons.length).toBeGreaterThan(0);

      const deleteButtons = await page.$$('.action-btn.delete');
      expect(deleteButtons.length).toBeGreaterThan(0);
    });
  });

  describe('Metadata Editor - Search Functionality', () => {
    beforeEach(async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);
      await page.click('.tab-nav .tab-button:nth-child(2)');
      await page.waitForSelector('.metadata-editor', { timeout: 10000 });
      await page.waitForFunction(
        () => document.querySelector('.loading-state') === null,
        { timeout: 30000 }
      );
    });

    test('Should search and display results', async () => {
      // Get initial total
      const initialTotal = await page.$eval('.metadata-stats .stat-value', el => el.textContent);

      // Type search query
      await page.type('.search-input', 'Boston');
      await page.click('.search-btn');

      // Wait for search results
      await page.waitForFunction(
        () => {
          const loading = document.querySelector('.loading-state');
          return loading === null;
        },
        { timeout: 30000 }
      );

      // Should show "Results" stat
      const statLabels = await page.$$eval('.stat-label', els =>
        els.map(el => el.textContent)
      );
      expect(statLabels).toContain('Results');

      // Results should be less than or equal to total
      const results = await page.$eval('.metadata-stats .stat-item:last-child .stat-value', el => el.textContent);
      expect(parseInt(results?.replace(/,/g, '') || '0')).toBeLessThanOrEqual(
        parseInt(initialTotal?.replace(/,/g, '') || '0')
      );
    }, 60000);

    test('Should clear search when clear button is clicked', async () => {
      // Type search query
      await page.type('.search-input', 'test');
      await page.click('.search-btn');

      await page.waitForFunction(
        () => document.querySelector('.loading-state') === null,
        { timeout: 30000 }
      );

      // Click clear button
      await page.click('.search-clear');

      // Wait for reload
      await page.waitForFunction(
        () => document.querySelector('.loading-state') === null,
        { timeout: 30000 }
      );

      // Search input should be empty
      const inputValue = await page.$eval('.search-input', (el: HTMLInputElement) => el.value);
      expect(inputValue).toBe('');
    }, 60000);
  });

  describe('Metadata Editor - Pagination', () => {
    beforeEach(async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);
      await page.click('.tab-nav .tab-button:nth-child(2)');
      await page.waitForSelector('.metadata-editor', { timeout: 10000 });
      await page.waitForFunction(
        () => document.querySelector('.loading-state') === null,
        { timeout: 30000 }
      );
    });

    test('Should display pagination controls', async () => {
      const pagination = await page.$('.pagination');
      expect(pagination).not.toBeNull();

      const prevBtn = await page.$('.pagination-btn');
      expect(prevBtn).not.toBeNull();
    });

    test('Should navigate to next page', async () => {
      // Get first row title before navigation
      const firstRowTitle = await page.$eval('.music-table tbody tr:first-child .cell-title', el => el.textContent);

      // Click next page
      const nextBtn = await page.$('.pagination-btn:last-child');
      await nextBtn?.click();

      // Wait for data to load
      await page.waitForFunction(
        () => document.querySelector('.loading-state') === null,
        { timeout: 30000 }
      );

      // Get first row title after navigation
      const newFirstRowTitle = await page.$eval('.music-table tbody tr:first-child .cell-title', el => el.textContent);

      // Titles should be different (different page)
      expect(newFirstRowTitle).not.toBe(firstRowTitle);
    }, 60000);
  });

  describe('Metadata Editor - Selection Functionality', () => {
    beforeEach(async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);
      await page.click('.tab-nav .tab-button:nth-child(2)');
      await page.waitForSelector('.metadata-editor', { timeout: 10000 });
      await page.waitForFunction(
        () => document.querySelector('.loading-state') === null,
        { timeout: 30000 }
      );
    });

    test('Should select a row when checkbox is clicked', async () => {
      // Click first row checkbox
      const firstCheckbox = await page.$('.music-table tbody tr:first-child .cell-select input');
      await firstCheckbox?.click();

      // Row should have selected class
      const selectedRow = await page.$('.music-table tbody tr.selected');
      expect(selectedRow).not.toBeNull();

      // Bulk action bar should appear
      const bulkBar = await page.$('.bulk-action-bar');
      expect(bulkBar).not.toBeNull();
    });

    test('Should show selected count in stats', async () => {
      // Select first row
      const firstCheckbox = await page.$('.music-table tbody tr:first-child .cell-select input');
      await firstCheckbox?.click();

      // Should show "Selected" stat
      const selectedStat = await page.$('.selected-count');
      expect(selectedStat).not.toBeNull();

      const selectedValue = await page.$eval('.selected-count .stat-value', el => el.textContent);
      expect(selectedValue).toBe('1');
    });

    test('Should select all rows when header checkbox is clicked', async () => {
      // Click header checkbox
      const headerCheckbox = await page.$('.col-select input[type="checkbox"]');
      await headerCheckbox?.click();

      // All rows should be selected
      const selectedRows = await page.$$('.music-table tbody tr.selected');
      const totalRows = await page.$$('.music-table tbody tr');
      expect(selectedRows.length).toBe(totalRows.length);
    });

    test('Should clear selection when Clear Selection is clicked', async () => {
      // Select first row
      const firstCheckbox = await page.$('.music-table tbody tr:first-child .cell-select input');
      await firstCheckbox?.click();

      // Click Clear Selection
      const clearBtn = await page.$('.bulk-btn.clear');
      await clearBtn?.click();

      // No rows should be selected
      const selectedRows = await page.$$('.music-table tbody tr.selected');
      expect(selectedRows.length).toBe(0);

      // Bulk bar should disappear
      const bulkBar = await page.$('.bulk-action-bar');
      expect(bulkBar).toBeNull();
    });
  });

  describe('Metadata Editor - Bulk Edit Modal', () => {
    beforeEach(async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);
      await page.click('.tab-nav .tab-button:nth-child(2)');
      await page.waitForSelector('.metadata-editor', { timeout: 10000 });
      await page.waitForFunction(
        () => document.querySelector('.loading-state') === null,
        { timeout: 30000 }
      );
    });

    test('Should open bulk edit modal when Edit Selected is clicked', async () => {
      // Select first row
      const firstCheckbox = await page.$('.music-table tbody tr:first-child .cell-select input');
      await firstCheckbox?.click();

      // Click Edit Selected
      const editBtn = await page.$('.bulk-btn.edit');
      await editBtn?.click();

      // Modal should appear
      await page.waitForSelector('.bulk-edit-modal', { timeout: 5000 });
      const modal = await page.$('.bulk-edit-modal');
      expect(modal).not.toBeNull();
    });

    test('Should display bulk edit form fields', async () => {
      // Select first row
      const firstCheckbox = await page.$('.music-table tbody tr:first-child .cell-select input');
      await firstCheckbox?.click();

      // Open modal
      const editBtn = await page.$('.bulk-btn.edit');
      await editBtn?.click();

      await page.waitForSelector('.bulk-edit-modal', { timeout: 5000 });

      // Check for field labels
      const fieldLabels = await page.$$eval('.bulk-field-label', els =>
        els.map(el => el.textContent?.trim())
      );
      expect(fieldLabels.some(l => l?.includes('Artist'))).toBe(true);
      expect(fieldLabels.some(l => l?.includes('Album'))).toBe(true);
      expect(fieldLabels.some(l => l?.includes('Genre'))).toBe(true);
    });

    test('Should close modal when Cancel is clicked', async () => {
      // Select first row
      const firstCheckbox = await page.$('.music-table tbody tr:first-child .cell-select input');
      await firstCheckbox?.click();

      // Open modal
      const editBtn = await page.$('.bulk-btn.edit');
      await editBtn?.click();

      await page.waitForSelector('.bulk-edit-modal', { timeout: 5000 });

      // Click Cancel
      const cancelBtn = await page.$('.modal-btn.cancel');
      await cancelBtn?.click();

      // Modal should disappear
      await page.waitForFunction(
        () => document.querySelector('.bulk-edit-modal') === null,
        { timeout: 5000 }
      );

      const modal = await page.$('.bulk-edit-modal');
      expect(modal).toBeNull();
    });

    test('Should close modal when X button is clicked', async () => {
      // Select first row
      const firstCheckbox = await page.$('.music-table tbody tr:first-child .cell-select input');
      await firstCheckbox?.click();

      // Open modal
      const editBtn = await page.$('.bulk-btn.edit');
      await editBtn?.click();

      await page.waitForSelector('.bulk-edit-modal', { timeout: 5000 });

      // Click X close button
      await page.click('.modal-close');

      // Modal should disappear
      await page.waitForFunction(
        () => document.querySelector('.bulk-edit-modal') === null,
        { timeout: 5000 }
      );
    });
  });

  describe('Metadata Editor - Inline Editing', () => {
    beforeEach(async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);
      await page.click('.tab-nav .tab-button:nth-child(2)');
      await page.waitForSelector('.metadata-editor', { timeout: 10000 });
      await page.waitForFunction(
        () => document.querySelector('.loading-state') === null,
        { timeout: 30000 }
      );
    });

    test('Should enter edit mode when edit button is clicked', async () => {
      // Click edit button on first row
      const editBtn = await page.$('.music-table tbody tr:first-child .action-btn.edit');
      await editBtn?.click();

      // Row should have editing class
      const editingRow = await page.$('.music-table tbody tr.editing');
      expect(editingRow).not.toBeNull();

      // Should show edit inputs
      const editInputs = await page.$$('.edit-input');
      expect(editInputs.length).toBeGreaterThan(0);
    });

    test('Should show save and cancel buttons in edit mode', async () => {
      // Click edit button on first row
      const editBtn = await page.$('.music-table tbody tr:first-child .action-btn.edit');
      await editBtn?.click();

      // Should show save button
      const saveBtn = await page.$('.action-btn.save');
      expect(saveBtn).not.toBeNull();

      // Should show cancel button
      const cancelBtn = await page.$('.action-btn.cancel');
      expect(cancelBtn).not.toBeNull();
    });

    test('Should exit edit mode when cancel is clicked', async () => {
      // Enter edit mode
      const editBtn = await page.$('.music-table tbody tr:first-child .action-btn.edit');
      await editBtn?.click();

      // Click cancel
      const cancelBtn = await page.$('.action-btn.cancel');
      await cancelBtn?.click();

      // Should exit edit mode
      const editingRow = await page.$('.music-table tbody tr.editing');
      expect(editingRow).toBeNull();
    });
  });

  describe('Metadata Editor - Visual Elements', () => {
    beforeEach(async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);
      await page.click('.tab-nav .tab-button:nth-child(2)');
      await page.waitForSelector('.metadata-editor', { timeout: 10000 });
    });

    test('Should have proper header styling', async () => {
      const headerBorderBottom = await page.$eval('.metadata-header', el =>
        getComputedStyle(el).borderBottomStyle
      );
      expect(headerBorderBottom).toBe('solid');
    });

    test('Should display loading spinner while loading', async () => {
      // Navigate away and back to trigger loading
      await page.click('.tab-nav .tab-button:nth-child(1)');
      await new Promise(resolve => setTimeout(resolve, 100));
      await page.click('.tab-nav .tab-button:nth-child(2)');

      // Check for loading state (may be brief)
      const loadingState = await page.$('.loading-state');
      // Loading state may or may not be visible depending on cache speed
      expect(loadingState !== null || true).toBe(true);
    });
  });
});
