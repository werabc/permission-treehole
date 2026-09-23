const path = require('path')
const fs = require('fs')
const { chromium } = require('playwright-core')

const HOME = process.env.USERPROFILE || process.env.HOME
const EXE = path.join(HOME, 'AppData', 'Local', 'ms-playwright', 'chromium-1243', 'chrome-win64', 'chrome.exe')

;(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: EXE })
  const page = await browser.newPage({ viewport: { width: 1440, height: 940 } })
  const errs = []
  page.on('pageerror', e => errs.push(String(e)))
  page.on('console', m => { if (m.type() === 'error') errs.push('console: ' + m.text()) })

  await page.goto('file:///D:/开发项目/s1/treehole-web/design-preview/index.html', { waitUntil: 'networkidle' })
  await page.waitForTimeout(2500)

  const out = 'D:/开发项目/s1/treehole-web/design-preview/_shots'
  fs.mkdirSync(out, { recursive: true })

  for (const v of ['home', 'detail', 'publish', 'auth', 'notify', 'profile', 'spec']) {
    await page.click(`.tab[data-view="${v}"]`)
    await page.waitForTimeout(1500)
    await page.screenshot({ path: `${out}/${v}.png`, fullPage: v === 'spec' })
  }
  console.log('ERRORS:', errs.length ? errs.join(' | ') : 'none')
  await browser.close()
})()
