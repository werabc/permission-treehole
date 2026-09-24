const { chromium } = require('playwright-core')
const { execFileSync } = require('child_process')
const ADMIN = 'http://localhost:5173'
const REDIS_CLI = 'D://redis//redis-cli.exe'
const CHROME = process.env.LOCALAPPDATA + '\\ms-playwright\\chromium-1243\\chrome-win64\\chrome.exe'
;(async () => {
  const browser = await chromium.launch({ executablePath: CHROME, headless: true })
  const page = await browser.newPage({ viewport: { width: 1600, height: 900 } })
  let pageCaptchaKey = ''
  page.on('response', async r => {
    if (r.url().includes('/api/auth/captcha') && !pageCaptchaKey) {
      try { const j = await r.json(); if (j?.data?.captchaKey) pageCaptchaKey = j.data.captchaKey } catch {}
    }
  })
  await page.goto(ADMIN + '/login', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1500)
  await page.getByPlaceholder('请输入用户名').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@1234')
  await page.waitForTimeout(800)
  const out = execFileSync(REDIS_CLI, ['-h','127.0.0.1','-p','6379','-a','redis123456','GET','captcha:'+pageCaptchaKey], {encoding:'utf8',stdio:['ignore','pipe','ignore']}).trim().replace(/^"|"$/g,'')
  await page.getByPlaceholder('验证码').fill(out)
  await page.getByRole('button', { name: /登\s*录|登录/ }).click()
  await page.waitForTimeout(2500)
  console.log('登录后URL:', page.url())
  await page.goto(ADMIN + '/system/role', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(2500)
  console.log('goto后URL:', page.url())
  console.log('页面标题:', await page.title())
  const btns = await page.locator('button').allInnerTexts()
  console.log('按钮:', btns.filter(b => b.trim()).slice(0, 12).join(' | '))
  await page.screenshot({ path: 'docs/login-check/admin-role-dbg.png' })
  const bodyText = (await page.locator('body').innerText()).slice(0, 300).replace(/\n+/g, ' | ')
  console.log('正文片段:', bodyText)
  await browser.close()
})()
