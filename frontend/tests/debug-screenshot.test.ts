import puppeteer from 'puppeteer';

const FRONTEND_URL = 'http://localhost:4173';

describe('Debug Screenshot Test', () => {
  test('Take screenshot of frontend', async () => {
    const browser = await puppeteer.launch({
      headless: 'new',
      args: ['--no-sandbox', '--disable-setuid-sandbox'],
    });

    const page = await browser.newPage();
    await page.setViewport({ width: 1280, height: 800 });

    console.log('Navigating to:', FRONTEND_URL);
    const response = await page.goto(FRONTEND_URL, {
      waitUntil: 'load',
      timeout: 30000
    });
    console.log('Response status:', response?.status());

    // Wait a bit for JavaScript to execute
    await new Promise(resolve => setTimeout(resolve, 3000));

    // Get page content
    const html = await page.content();
    console.log('Page HTML length:', html.length);
    console.log('First 500 chars:', html.substring(0, 500));

    // Take screenshot
    await page.screenshot({
      path: '/Users/richard/projects/MP3Org/frontend/tests/debug-screenshot.png',
      fullPage: true
    });
    console.log('Screenshot saved to debug-screenshot.png');

    // Check if we can find specific elements
    const body = await page.$('body');
    console.log('Body found:', body !== null);

    const rootDiv = await page.$('#root');
    console.log('Root div found:', rootDiv !== null);

    const appDiv = await page.$('.app');
    console.log('App div found:', appDiv !== null);

    // Evaluate in page context
    const reactRoot = await page.evaluate(() => {
      const root = document.getElementById('root');
      return root ? root.innerHTML.substring(0, 200) : 'Not found';
    });
    console.log('React root innerHTML:', reactRoot);

    await browser.close();

    expect(true).toBe(true);
  }, 60000);
});
