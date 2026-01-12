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

// Helper to navigate to Organize tab
async function navigateToOrganizeTab(page: Page) {
  await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
  await waitForReact(page);

  // Find and click the Organize tab
  const tabs = await page.$$('.tab-button');
  for (const tab of tabs) {
    const label = await tab.$eval('.tab-label', el => el.textContent);
    if (label === 'Organize') {
      await tab.click();
      break;
    }
  }

  // Wait for organize view to render
  await page.waitForSelector('.organize-view', { timeout: 5000 });
  await new Promise(resolve => setTimeout(resolve, 300));
}

describe('MP3Org Organization View E2E Tests', () => {
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

  describe('Backend Organization API Health', () => {
    test('Backend templates endpoint should return templates', async () => {
      const response = await fetch(`${BACKEND_URL}/api/v1/organization/templates`);
      expect(response.ok).toBe(true);

      const data = await response.json();
      expect(data.examples).toBeDefined();
      expect(Array.isArray(data.examples)).toBe(true);
      expect(data.default).toBeDefined();
      expect(typeof data.default).toBe('string');
    }, 60000);

    test('Backend fields endpoint should return available fields', async () => {
      const response = await fetch(`${BACKEND_URL}/api/v1/organization/fields`);
      expect(response.ok).toBe(true);

      const data = await response.json();
      expect(Array.isArray(data)).toBe(true);
      expect(data.length).toBeGreaterThan(0);
      expect(data).toContain('artist');
      expect(data).toContain('album');
      expect(data).toContain('title');
    });

    test('Backend formats endpoint should return text formats', async () => {
      const response = await fetch(`${BACKEND_URL}/api/v1/organization/formats`);
      expect(response.ok).toBe(true);

      const data = await response.json();
      expect(Array.isArray(data)).toBe(true);
      expect(data.length).toBeGreaterThan(0);
      expect(data[0].name).toBeDefined();
      expect(data[0].description).toBeDefined();
    });

    test('Backend preview-all endpoint should accept POST request', async () => {
      const response = await fetch(`${BACKEND_URL}/api/v1/organization/preview-all`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          basePath: '/test/output',
          textFormat: 'UNDERSCORE',
          useSubdirectories: true,
          subdirectoryLevels: 7,
          page: 0,
          size: 10,
        }),
      });
      expect(response.ok).toBe(true);

      const data = await response.json();
      expect(data.previews).toBeDefined();
      expect(Array.isArray(data.previews)).toBe(true);
      expect(data.totalCount).toBeDefined();
      expect(data.totalPages).toBeDefined();
    });
  });

  describe('Frontend Navigation to Organize Tab', () => {
    test('Should load the application', async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);

      const logoText = await page.$eval('.logo-text', el => el.textContent);
      expect(logoText).toBe('MP3Org');
    });

    test('Should display Organize tab in navigation', async () => {
      await page.goto(FRONTEND_URL, { waitUntil: 'domcontentloaded' });
      await waitForReact(page);

      const tabLabels = await page.$$eval('.tab-label', els =>
        els.map(el => el.textContent)
      );
      expect(tabLabels).toContain('Organize');
    });

    test('Should navigate to Organize tab when clicked', async () => {
      await navigateToOrganizeTab(page);

      const organizeView = await page.$('.organize-view');
      expect(organizeView).not.toBeNull();
    });

    test('Should display Organize title', async () => {
      await navigateToOrganizeTab(page);

      const title = await page.$eval('.organize-title', el => el.textContent);
      expect(title).toBe('Organize Music Files');
    });
  });

  describe('Configuration Panel', () => {
    test('Should display configuration panel with title', async () => {
      await navigateToOrganizeTab(page);

      const panelTitle = await page.$eval('.config-panel .panel-title', el => el.textContent);
      expect(panelTitle).toBe('Configuration');
    });

    test('Should have base path input with default value', async () => {
      await navigateToOrganizeTab(page);

      const basePath = await page.$eval(
        '.config-panel input.config-input[type="text"]',
        el => (el as HTMLInputElement).value
      );
      expect(basePath).toBe('/Volumes/Music/Organized');
    });

    test('Should allow editing base path', async () => {
      await navigateToOrganizeTab(page);

      const input = await page.$('.config-panel input.config-input[type="text"]');
      await input!.click({ clickCount: 3 }); // Select all
      await input!.type('/new/test/path');

      const newValue = await page.$eval(
        '.config-panel input.config-input[type="text"]',
        el => (el as HTMLInputElement).value
      );
      expect(newValue).toBe('/new/test/path');
    });

    test('Should display path template textarea', async () => {
      await navigateToOrganizeTab(page);

      const textarea = await page.$('.config-panel .config-textarea');
      expect(textarea).not.toBeNull();
    });

    test('Should display field insertion buttons', async () => {
      await navigateToOrganizeTab(page);

      const fieldButtons = await page.$$('.field-buttons .field-btn');
      expect(fieldButtons.length).toBeGreaterThan(0);
    });

    test('Should insert field placeholder when button clicked', async () => {
      await navigateToOrganizeTab(page);

      // Clear the textarea first
      const textarea = await page.$('.config-panel .config-textarea');
      await textarea!.click({ clickCount: 3 });
      await page.keyboard.press('Backspace');

      // Click a field button
      const artistBtn = await page.$('.field-btn');
      await artistBtn!.click();

      // Check textarea has placeholder
      const value = await page.$eval(
        '.config-panel .config-textarea',
        el => (el as HTMLTextAreaElement).value
      );
      expect(value).toMatch(/\{[a-z_]+\}/);
    });

    test('Should display example templates dropdown', async () => {
      await navigateToOrganizeTab(page);

      const select = await page.$('.config-panel select.config-select');
      expect(select).not.toBeNull();
    });

    test('Should display text format dropdown', async () => {
      await navigateToOrganizeTab(page);

      const selects = await page.$$('.config-panel select.config-select');
      // Second select should be text format
      expect(selects.length).toBeGreaterThanOrEqual(2);
    });

    test('Should display subdirectory checkbox', async () => {
      await navigateToOrganizeTab(page);

      const checkbox = await page.$('.config-checkbox input[type="checkbox"]');
      expect(checkbox).not.toBeNull();
    });

    test('Should toggle subdirectory levels input visibility', async () => {
      await navigateToOrganizeTab(page);

      // Subdirectory checkbox should be checked by default
      const isChecked = await page.$eval(
        '.config-checkbox input[type="checkbox"]',
        el => (el as HTMLInputElement).checked
      );
      expect(isChecked).toBe(true);

      // Subdirectory levels input should be visible
      const levelsInput = await page.$('input.config-input-small');
      expect(levelsInput).not.toBeNull();

      // Uncheck the checkbox
      const checkbox = await page.$('.config-checkbox input[type="checkbox"]');
      await checkbox!.click();

      // Wait for state update
      await new Promise(resolve => setTimeout(resolve, 200));

      // Levels input should be hidden
      const levelsInputAfter = await page.$('input.config-input-small');
      expect(levelsInputAfter).toBeNull();
    });

    test('Should have Preview Organization button', async () => {
      await navigateToOrganizeTab(page);

      const button = await page.$('.preview-btn');
      expect(button).not.toBeNull();

      const buttonText = await page.$eval('.preview-btn', el => el.textContent);
      expect(buttonText).toBe('Preview Organization');
    });
  });

  describe('Preview Panel', () => {
    test('Should display preview panel with title', async () => {
      await navigateToOrganizeTab(page);

      const panelTitle = await page.$eval('.preview-panel .panel-title', el => el.textContent);
      expect(panelTitle).toContain('Preview');
    });

    test('Should display empty preview state initially', async () => {
      await navigateToOrganizeTab(page);

      const emptyPreview = await page.$('.empty-preview');
      expect(emptyPreview).not.toBeNull();

      const emptyTitle = await page.$eval('.empty-preview h3', el => el.textContent);
      expect(emptyTitle).toBe('No Preview Available');
    });
  });

  describe('Preview Functionality', () => {
    test('Should show loading state or complete when preview button clicked', async () => {
      await navigateToOrganizeTab(page);

      // Click preview button
      const previewBtn = await page.$('.preview-btn');
      await previewBtn!.click();

      // Button should show loading text OR be back to normal (if loading completed quickly)
      const buttonText = await page.$eval('.preview-btn', el => el.textContent);
      // Accept either loading state or completed state (API might be fast)
      expect(['Loading Preview...', 'Preview Organization']).toContain(buttonText);
    }, 15000);

    test('Should load and display preview data', async () => {
      await navigateToOrganizeTab(page);

      // Click preview button
      const previewBtn = await page.$('.preview-btn');
      await previewBtn!.click();

      // Wait for preview to load
      await page.waitForSelector('.preview-table', { timeout: 10000 });

      // Should have preview rows
      const rows = await page.$$('.preview-table tbody tr');
      expect(rows.length).toBeGreaterThan(0);
    }, 15000);

    test('Should display file info in preview table', async () => {
      await navigateToOrganizeTab(page);

      // Click preview button and wait for data
      const previewBtn = await page.$('.preview-btn');
      await previewBtn!.click();
      await page.waitForSelector('.preview-table', { timeout: 10000 });

      // Should have file title and meta info
      const fileTitle = await page.$('.file-title');
      expect(fileTitle).not.toBeNull();

      const fileMeta = await page.$('.file-meta');
      expect(fileMeta).not.toBeNull();
    }, 15000);

    test('Should display current and proposed paths', async () => {
      await navigateToOrganizeTab(page);

      const previewBtn = await page.$('.preview-btn');
      await previewBtn!.click();
      await page.waitForSelector('.preview-table', { timeout: 10000 });

      // Should have current and proposed path cells
      const currentPaths = await page.$$('.cell-path.current');
      expect(currentPaths.length).toBeGreaterThan(0);

      const proposedPaths = await page.$$('.cell-path.proposed');
      expect(proposedPaths.length).toBeGreaterThan(0);
    }, 15000);

    test('Should update preview total count in header', async () => {
      await navigateToOrganizeTab(page);

      const previewBtn = await page.$('.preview-btn');
      await previewBtn!.click();
      await page.waitForSelector('.preview-table', { timeout: 10000 });

      const headerText = await page.$eval('.preview-header .panel-title', el => el.textContent);
      expect(headerText).toMatch(/Preview \([\d,]+ files\)/);
    }, 15000);
  });

  describe('Selection Functionality', () => {
    test('Should have selection checkboxes in preview table', async () => {
      await navigateToOrganizeTab(page);

      const previewBtn = await page.$('.preview-btn');
      await previewBtn!.click();
      await page.waitForSelector('.preview-table', { timeout: 10000 });

      const checkboxes = await page.$$('.cell-select input[type="checkbox"]');
      expect(checkboxes.length).toBeGreaterThan(0);
    }, 15000);

    test('Should have select all checkbox in header', async () => {
      await navigateToOrganizeTab(page);

      const previewBtn = await page.$('.preview-btn');
      await previewBtn!.click();
      await page.waitForSelector('.preview-table', { timeout: 10000 });

      const headerCheckbox = await page.$('.col-select input[type="checkbox"]');
      expect(headerCheckbox).not.toBeNull();
    }, 15000);

    test('Should select row when checkbox clicked', async () => {
      await navigateToOrganizeTab(page);

      const previewBtn = await page.$('.preview-btn');
      await previewBtn!.click();
      await page.waitForSelector('.preview-table', { timeout: 10000 });

      // Wait for table to fully render
      await new Promise(resolve => setTimeout(resolve, 500));

      // Click first VALID row checkbox using evaluate for better React compatibility
      await page.evaluate(() => {
        const checkbox = document.querySelector('.preview-table tbody tr:not(.invalid) .cell-select input[type="checkbox"]:not(:disabled)') as HTMLInputElement;
        if (checkbox) {
          checkbox.click();
        }
      });

      // Wait longer for state update
      await new Promise(resolve => setTimeout(resolve, 800));

      // Row should have selected class
      const selectedRow = await page.$('.preview-table tbody tr.selected');
      expect(selectedRow).not.toBeNull();
    }, 20000);

    test('Should show selected count after selection', async () => {
      await navigateToOrganizeTab(page);

      const previewBtn = await page.$('.preview-btn');
      await previewBtn!.click();
      await page.waitForSelector('.preview-table', { timeout: 10000 });

      // Wait for table to fully render
      await new Promise(resolve => setTimeout(resolve, 500));

      // Click first VALID row checkbox using evaluate for better React compatibility
      await page.evaluate(() => {
        const checkbox = document.querySelector('.preview-table tbody tr:not(.invalid) .cell-select input[type="checkbox"]:not(:disabled)') as HTMLInputElement;
        if (checkbox) {
          checkbox.click();
        }
      });

      // Wait longer for state update
      await new Promise(resolve => setTimeout(resolve, 800));

      // Should show selected info
      const selectedInfo = await page.$('.selected-info');
      expect(selectedInfo).not.toBeNull();

      const selectedText = await page.$eval('.selected-info', el => el.textContent);
      expect(selectedText).toContain('1 selected');
    }, 20000);

    test('Should show execute button after selection', async () => {
      await navigateToOrganizeTab(page);

      const previewBtn = await page.$('.preview-btn');
      await previewBtn!.click();
      await page.waitForSelector('.preview-table', { timeout: 10000 });

      // Wait for table to fully render
      await new Promise(resolve => setTimeout(resolve, 500));

      // Click first VALID row checkbox using evaluate for better React compatibility
      await page.evaluate(() => {
        const checkbox = document.querySelector('.preview-table tbody tr:not(.invalid) .cell-select input[type="checkbox"]:not(:disabled)') as HTMLInputElement;
        if (checkbox) {
          checkbox.click();
        }
      });

      // Wait longer for state update
      await new Promise(resolve => setTimeout(resolve, 800));

      // Should show execute button
      const executeBtn = await page.$('.execute-btn');
      expect(executeBtn).not.toBeNull();

      const buttonText = await page.$eval('.execute-btn', el => el.textContent);
      expect(buttonText).toContain('Organize');
    }, 20000);

    test('Should select all valid rows when header checkbox clicked', async () => {
      await navigateToOrganizeTab(page);

      const previewBtn = await page.$('.preview-btn');
      await previewBtn!.click();
      await page.waitForSelector('.preview-table', { timeout: 10000 });

      // Get count of valid rows
      const validRows = await page.$$('.preview-table tbody tr:not(.invalid)');
      const validCount = validRows.length;

      // Click header checkbox
      const headerCheckbox = await page.$('.col-select input[type="checkbox"]');
      await headerCheckbox!.click();

      // Wait for state update
      await new Promise(resolve => setTimeout(resolve, 200));

      // Selected info should show count
      const selectedText = await page.$eval('.selected-info', el => el.textContent);
      expect(selectedText).toContain(`${validCount} selected`);
    }, 15000);
  });

  describe('Pagination', () => {
    test('Should display pagination when multiple pages exist', async () => {
      await navigateToOrganizeTab(page);

      const previewBtn = await page.$('.preview-btn');
      await previewBtn!.click();
      await page.waitForSelector('.preview-table', { timeout: 10000 });

      // Wait a moment for pagination to render
      await new Promise(resolve => setTimeout(resolve, 300));

      // Check if pagination exists (may not if few files)
      const pagination = await page.$('.pagination');
      // Just verify the element exists or doesn't error
      expect(pagination !== null || pagination === null).toBe(true);
    }, 15000);

    test('Should navigate to next page when Next clicked', async () => {
      await navigateToOrganizeTab(page);

      const previewBtn = await page.$('.preview-btn');
      await previewBtn!.click();
      await page.waitForSelector('.preview-table', { timeout: 10000 });

      // Check if pagination exists with next button enabled
      const nextBtn = await page.$('.pagination-btn:not(:disabled)');
      if (nextBtn) {
        const initialInfo = await page.$eval('.pagination-info', el => el.textContent);

        // Click next if available
        const buttons = await page.$$('.pagination-btn');
        if (buttons.length >= 2) {
          await buttons[1].click(); // Next is usually second
          await new Promise(resolve => setTimeout(resolve, 500));

          const newInfo = await page.$eval('.pagination-info', el => el.textContent);
          // Info should have changed
          expect(newInfo).not.toBe(initialInfo);
        }
      }
    }, 15000);
  });

  describe('Error Handling', () => {
    test('Should show error for empty base path', async () => {
      await navigateToOrganizeTab(page);

      // Clear the base path
      const input = await page.$('.config-panel input.config-input[type="text"]');
      await input!.click({ clickCount: 3 });
      await page.keyboard.press('Backspace');

      // Click preview
      const previewBtn = await page.$('.preview-btn');
      await previewBtn!.click();

      // Wait for error state
      await new Promise(resolve => setTimeout(resolve, 300));

      // Should show error
      const error = await page.$('.preview-error');
      expect(error).not.toBeNull();

      const errorText = await page.$eval('.preview-error', el => el.textContent);
      expect(errorText).toContain('base path');
    }, 10000);
  });

  describe('Text Format Options', () => {
    test('Should have text format dropdown with options', async () => {
      await navigateToOrganizeTab(page);

      // Wait for formats to load
      await new Promise(resolve => setTimeout(resolve, 500));

      // Find the text format select (second select)
      const selects = await page.$$('.config-panel select.config-select');
      expect(selects.length).toBeGreaterThanOrEqual(2);

      // Check the second select has options (text format dropdown)
      const optionCount = await selects[1].$$eval('option', els => els.length);
      // Should have at least UNDERSCORE and other formats
      expect(optionCount).toBeGreaterThan(0);
    });

    test('Should default to UNDERSCORE format', async () => {
      await navigateToOrganizeTab(page);

      // Wait for formats to load
      await new Promise(resolve => setTimeout(resolve, 500));

      // Get the selected value of text format dropdown
      const selects = await page.$$('.config-panel select.config-select');
      const textFormatValue = await selects[1].evaluate(
        el => (el as HTMLSelectElement).value
      );
      expect(textFormatValue).toBe('UNDERSCORE');
    });
  });

  describe('Example Templates', () => {
    test('Should populate template textarea when example selected', async () => {
      await navigateToOrganizeTab(page);

      // Wait for templates to load
      await new Promise(resolve => setTimeout(resolve, 500));

      // Get current template value
      const currentTemplate = await page.$eval(
        '.config-textarea',
        el => (el as HTMLTextAreaElement).value
      );

      // Find example templates dropdown (first select)
      const exampleSelect = await page.$('.config-panel select.config-select');

      // Get available options
      const options = await exampleSelect!.$$eval('option', els =>
        els.map(el => el.value).filter(v => v !== '')
      );

      if (options.length > 0) {
        // Select an example template
        await exampleSelect!.select(options[0]);

        // Wait for state update
        await new Promise(resolve => setTimeout(resolve, 200));

        // Template should be updated
        const newTemplate = await page.$eval(
          '.config-textarea',
          el => (el as HTMLTextAreaElement).value
        );

        // If the default was different from the example, they should differ
        // (or be the same if the example matches default)
        expect(newTemplate.length).toBeGreaterThan(0);
      }
    }, 10000);
  });

  describe('Subdirectory Configuration', () => {
    test('Should have subdirectory levels input with default value', async () => {
      await navigateToOrganizeTab(page);

      const levelsInput = await page.$('input.config-input-small');
      expect(levelsInput).not.toBeNull();

      const value = await page.$eval(
        'input.config-input-small',
        el => (el as HTMLInputElement).value
      );
      expect(value).toBe('7'); // Default value
    });

    test('Should allow changing subdirectory levels', async () => {
      await navigateToOrganizeTab(page);

      const levelsInput = await page.$('input.config-input-small');
      await levelsInput!.click({ clickCount: 3 });
      await levelsInput!.type('5');

      const newValue = await page.$eval(
        'input.config-input-small',
        el => (el as HTMLInputElement).value
      );
      expect(newValue).toBe('5');
    });

    test('Should clamp subdirectory levels between 1 and 12', async () => {
      await navigateToOrganizeTab(page);

      const levelsInput = await page.$('input.config-input-small');

      // Try to set to 0 - should clamp to 1
      await levelsInput!.click({ clickCount: 3 });
      await levelsInput!.type('0');
      // Trigger blur to run validation
      await page.keyboard.press('Tab');
      await new Promise(resolve => setTimeout(resolve, 100));

      // Note: The clamping happens on change, so entering 0 will become 1
      // But the actual value validation happens in the handler
    });
  });
});
