const { chromium } = require('playwright-core')
const CHROME = process.env.LOCALAPPDATA + '\\ms-playwright\\chromium-1243\\chrome-win64\\chrome.exe'
;(async () => {
  const browser = await chromium.launch({ executablePath: CHROME, headless: true })
  const page = await browser.newPage({ viewport: { width: 1440, height: 860 } })
  await page.goto('http://localhost:3000/#/login', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1500)
  await page.evaluate(() => { document.documentElement.setAttribute('data-theme', 'light'); localStorage.setItem('theme', 'light') })
  await page.waitForTimeout(800)
  await page.screenshot({ path: 'docs/login-check/login-light-v2.jpg', quality: 82, type: 'jpeg' })
  console.log('ok')
  await browser.close()
})()
