/**
 * 只截登录页，用来确认「布局3 的 .dh-shell 外壳是否把整屏分屏挤歪」
 * 用法：NSET NODE_PATH=...  node scripts/shot_login.cjs
 */
const path = require('path')
const fs = require('fs')
const { chromium } = require('playwright-core')

const HOME = process.env.USERPROFILE || process.env.HOME
const EXE = path.join(HOME, 'AppData', 'Local', 'ms-playwright', 'chromium-1243', 'chrome-win64', 'chrome.exe')
const OUT = 'D:/开发项目/s1/docs/login-check'

;(async () => {
  fs.mkdirSync(OUT, { recursive: true })
  const browser = await chromium.launch({ headless: true, executablePath: EXE })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  const bad = []
  page.on('response', (r) => { if (r.status() >= 400) bad.push(`${r.status()} ${r.url()}`) })

  for (const theme of ['dark', 'light']) {
    await page.goto('http://localhost:3000/#/login', { waitUntil: 'domcontentloaded' })
    await page.evaluate((t) => localStorage.setItem('th_theme', t), theme)
    await page.reload({ waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(2200)

    const info = await page.evaluate(() => {
      const shell = document.querySelector('.dh-shell')
      const auth = document.querySelector('.auth')
      const art = document.querySelector('.auth__art')
      const box = document.querySelector('.auth__box')
      const r = (el) => (el ? { x: Math.round(el.getBoundingClientRect().x), w: Math.round(el.getBoundingClientRect().width), h: Math.round(el.getBoundingClientRect().height) } : null)
      return {
        theme: document.documentElement.getAttribute('data-theme'),
        shellCols: shell ? getComputedStyle(shell).gridTemplateColumns : null,
        shellHasSidenav: !!document.querySelector('.dh-sidenav'),
        auth: r(auth),
        art: r(art),
        box: r(box),
        docScrollW: document.documentElement.scrollWidth,
        clientW: document.documentElement.clientWidth,
      }
    })
    console.log(theme, JSON.stringify(info, null, 0))
    await page.screenshot({ path: `${OUT}/login-${theme}.png` })
  }

  console.log('4xx/5xx:', bad.length ? bad.join(' | ') : '无')

  // 注册页共用 AuthLayout，同一处修复需一并确认
  await page.goto('http://localhost:3000/#/register', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1800)
  const reg = await page.evaluate(() => {
    const a = document.querySelector('.auth')
    const art = document.querySelector('.auth__art')
    return { authW: Math.round(a.getBoundingClientRect().width), artW: Math.round(art.getBoundingClientRect().width) }
  })
  console.log('register', JSON.stringify(reg))
  await page.screenshot({ path: `${OUT}/register-dark.png` })

  // 移动端：980px 以下应自动叠成「上图下表单」
  await page.setViewportSize({ width: 390, height: 860 })
  await page.goto('http://localhost:3000/#/login', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1800)
  const mob = await page.evaluate(() => {
    const art = document.querySelector('.auth__art')
    const box = document.querySelector('.auth__box')
    const over = document.documentElement.scrollWidth - document.documentElement.clientWidth
    return {
      artH: Math.round(art.getBoundingClientRect().height),
      boxW: Math.round(box.getBoundingClientRect().width),
      overflow: over,
    }
  })
  console.log('mobile', JSON.stringify(mob))
  await page.screenshot({ path: `${OUT}/login-mobile.png` })

  await browser.close()
})()
