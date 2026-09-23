const path = require('path')
const fs = require('fs')
const { chromium } = require('playwright-core')

const HOME = process.env.USERPROFILE || process.env.HOME
const EXE = path.join(HOME, 'AppData', 'Local', 'ms-playwright', 'chromium-1243', 'chrome-win64', 'chrome.exe')
const URL = 'file:///D:/开发项目/s1/treehole-web/design-preview/v3.html'
const OUT = 'D:/开发项目/s1/treehole-web/design-preview/_shots3'

async function pick(page, group, label) {
  await page.locator(`.ctl__group:has-text("${group}") .ctl__btn`, { hasText: label }).first().click()
  await page.waitForTimeout(900)
}
async function view(page, label) {
  await page.locator(`.ctl__btn[data-view]`, { hasText: label }).first().click()
  await page.waitForTimeout(1000)
}

;(async () => {
  fs.mkdirSync(OUT, { recursive: true })
  const browser = await chromium.launch({ headless: true, executablePath: EXE })
  const page = await browser.newPage({ viewport: { width: 1440, height: 980 } })
  const errs = []
  page.on('pageerror', (e) => errs.push(String(e)))
  page.on('console', (m) => { if (m.type() === 'error') errs.push('console: ' + m.text()) })

  await page.goto(URL, { waitUntil: 'networkidle' })
  await page.waitForTimeout(2000)

  // 深色 + 布局A
  await page.screenshot({ path: `${OUT}/01-home-dark-A.png` })
  await page.evaluate(() => scrollTo(0, 620)); await page.waitForTimeout(1500)
  await page.screenshot({ path: `${OUT}/02-home-dark-A-feed.png` })
  await page.evaluate(() => scrollTo(0, 0)); await page.waitForTimeout(600)

  // 深色 + 布局B
  await pick(page, '布局', 'B 横幅+双列')
  await page.evaluate(() => scrollTo(0, 540)); await page.waitForTimeout(1400)
  await page.screenshot({ path: `${OUT}/03-home-dark-B-feed.png` })
  await page.evaluate(() => scrollTo(0, 0)); await page.waitForTimeout(600)

  // 浅色 + 布局A
  await pick(page, '布局', 'A 内容+侧栏')
  await pick(page, '主题', '白天')
  await page.waitForTimeout(1400)
  await page.screenshot({ path: `${OUT}/04-home-light-A.png` })
  await page.evaluate(() => scrollTo(0, 640)); await page.waitForTimeout(1500)
  await page.screenshot({ path: `${OUT}/05-home-light-A-feed.png` })
  await page.evaluate(() => scrollTo(0, 0)); await page.waitForTimeout(600)

  // 浅色 + 布局B
  await pick(page, '布局', 'B 横幅+双列')
  await page.evaluate(() => scrollTo(0, 560)); await page.waitForTimeout(1400)
  await page.screenshot({ path: `${OUT}/06-home-light-B-feed.png` })
  await page.evaluate(() => scrollTo(0, 0)); await page.waitForTimeout(600)

  // 其它页面（浅色）
  await view(page, '帖子详情'); await page.screenshot({ path: `${OUT}/07-detail-light.png` })
  await view(page, '消息中心'); await page.screenshot({ path: `${OUT}/08-notify-light.png` })
  await view(page, '个人主页'); await page.screenshot({ path: `${OUT}/09-profile-light.png` })
  await view(page, '改动说明'); await page.waitForTimeout(600)
  await page.screenshot({ path: `${OUT}/10-spec-light.png`, fullPage: true })

  // 深色说明页
  await pick(page, '主题', '深夜')
  await page.waitForTimeout(900)
  await page.screenshot({ path: `${OUT}/11-spec-dark.png`, fullPage: true })

  // 移动端（浅色 + 深色）
  await pick(page, '主题', '白天')
  await view(page, '首页')
  await pick(page, '布局', 'A 内容+侧栏')
  await page.setViewportSize({ width: 390, height: 860 })
  await page.waitForTimeout(1600)
  const over = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  console.log('mobile overflow:', over)
  await page.screenshot({ path: `${OUT}/12-mobile-light.png` })
  await page.evaluate(() => scrollTo(0, 700)); await page.waitForTimeout(1400)
  await page.screenshot({ path: `${OUT}/12b-mobile-light-feed.png` })
  await pick(page, '主题', '深夜')
  await page.evaluate(() => scrollTo(0, 520)); await page.waitForTimeout(1400)
  await page.screenshot({ path: `${OUT}/13-mobile-dark-feed.png` })

  console.log('ERRORS:', errs.length ? errs.join(' | ') : 'none')
  await browser.close()
})().catch((e) => { console.error('FATAL', e); process.exit(2) })
