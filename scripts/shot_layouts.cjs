const path = require('path')
const fs = require('fs')
const { chromium } = require('playwright-core')

const HOME = process.env.USERPROFILE || process.env.HOME
const EXE = path.join(HOME, 'AppData', 'Local', 'ms-playwright', 'chromium-1243', 'chrome-win64', 'chrome.exe')
const URL = 'file:///D:/开发项目/s1/treehole-web/design-preview/layouts.html'
const OUT = 'D:/开发项目/s1/treehole-web/design-preview/_shotsL'

;(async () => {
  fs.mkdirSync(OUT, { recursive: true })
  const browser = await chromium.launch({ headless: true, executablePath: EXE })
  const page = await browser.newPage({ viewport: { width: 1440, height: 980 } })
  const errs = []
  page.on('pageerror', (e) => errs.push(String(e)))
  page.on('console', (m) => { if (m.type() === 'error') errs.push('console: ' + m.text()) })

  await page.goto(URL, { waitUntil: 'networkidle' })
  await page.waitForTimeout(2000)

  const pickL = (l) => page.locator(`.ctl__b[data-l="${l}"]`).click()
  const pickT = (t) => page.locator(`.ctl__b[data-theme="${t}"]`).click()

  for (const theme of ['dark', 'light']) {
    await pickT(theme); await page.waitForTimeout(1000)
    for (const L of ['1', '2', '3', '4']) {
      await pickL(L); await page.waitForTimeout(1500)
      await page.screenshot({ path: `${OUT}/L${L}-${theme}.png` })
      await page.evaluate(() => scrollTo(0, 760)); await page.waitForTimeout(1400)
      await page.screenshot({ path: `${OUT}/L${L}-${theme}-feed.png` })
      await page.evaluate(() => scrollTo(0, 0)); await page.waitForTimeout(500)
    }
  }

  // 按钮动效页
  await pickT('dark')
  await pickL('fx'); await page.waitForTimeout(1200)
  await page.screenshot({ path: `${OUT}/fx-dark.png`, fullPage: true })
  await pickT('light'); await page.waitForTimeout(900)
  await page.screenshot({ path: `${OUT}/fx-light.png`, fullPage: true })

  // 对比页
  await pickT('dark'); await pickL('cmp'); await page.waitForTimeout(900)
  await page.screenshot({ path: `${OUT}/cmp-dark.png`, fullPage: true })

  // 移动端（L1 / L3）
  await pickT('light'); await pickL('1'); await page.setViewportSize({ width: 390, height: 860 })
  await page.waitForTimeout(1500)
  const over = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  console.log('mobile overflow L1:', over)
  await page.screenshot({ path: `${OUT}/mobile-L1.png` })

  console.log('ERRORS:', errs.length ? errs.join(' | ') : 'none')
  await browser.close()
})().catch((e) => { console.error('FATAL', e); process.exit(2) })
