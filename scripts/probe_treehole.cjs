const path = require('path')
const { chromium } = require('playwright-core')

const HOME = process.env.USERPROFILE || process.env.HOME
const EXE = path.join(HOME, 'AppData', 'Local', 'ms-playwright', 'chromium-1243', 'chrome-win64', 'chrome.exe')
const TREE = 'http://localhost:3000'

;(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: EXE })
  const page = await browser.newPage({ viewport: { width: 1440, height: 940 } })

  const bad = []
  page.on('response', (r) => {
    if (r.status() >= 400) bad.push(`${r.status()} ${r.request().method()} ${r.url()}`)
  })

  // 登录
  await page.goto(`${TREE}/#/login`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1200)
  await page.getByPlaceholder('请输入用户名').fill('br_u1')
  await page.getByPlaceholder('请输入密码').fill('Browser@123')
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForTimeout(2500)

  console.log('--- A. 打开帖子详情 ---')
  await page.goto(`${TREE}/#/post/3`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(2000)
  console.log('URL:', page.url())

  console.log('--- B. 发表回响 ---')
  await page.locator('textarea.composer__in').fill('探针回响 ' + Date.now())
  await page.locator('.composer__foot .dh-btn--primary').click()
  await page.waitForTimeout(3000)
  console.log('评论后 4xx/5xx:', bad.length ? bad.join(' | ') : '无')

  console.log('--- C. 进入发布页并发布 ---')
  bad.length = 0
  await page.goto(`${TREE}/#/publish`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1500)
  await page.locator('textarea.ta').fill('探针帖 ' + Date.now())
  await page.getByRole('button', { name: /匿名发布|实名发布/ }).last().click()
  await page.waitForTimeout(3500)
  console.log('发布后 URL:', page.url())
  console.log('发布后 4xx/5xx:', bad.length ? bad.join(' | ') : '无')

  console.log('--- D. 移动端溢出元素定位 (390px) ---')
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto(`${TREE}/#/`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(2200)
  const over = await page.evaluate(() => {
    const dw = document.documentElement.clientWidth
    const out = []
    document.querySelectorAll('*').forEach((el) => {
      const r = el.getBoundingClientRect()
      if (r.width > 0 && r.right > dw + 1) {
        out.push({
          tag: el.tagName.toLowerCase(),
          cls: (el.className && typeof el.className === 'string' ? el.className : '').slice(0, 60),
          w: Math.round(r.width),
          right: Math.round(r.right),
        })
      }
    })
    return { dw, scrollW: document.documentElement.scrollWidth, out: out.slice(0, 14) }
  })
  console.log('clientWidth:', over.dw, 'scrollWidth:', over.scrollW)
  console.log(JSON.stringify(over.out, null, 2))

  await browser.close()
})().catch((e) => { console.error('FATAL', e); process.exit(2) })
