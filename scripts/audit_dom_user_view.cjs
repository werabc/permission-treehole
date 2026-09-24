/**
 * 用户视角 DOM 巡检（C 端）
 *
 * 与 verify_treehole_ui.cjs 的区别：
 *   那个脚本验证「功能与接口」，本脚本只回答一个问题——
 *   **作为一个用户打开每个页面，能不能看懂、能不能点、有没有坏掉的地方。**
 *
 * 每个页面检查五件事：
 *   1. 有真实内容渲染（不是空壳 / 白屏）
 *   2. 关键文案存在（用户知道自己在哪）
 *   3. 主要按钮可见且可点（不是 disabled / 被遮挡）
 *   4. 没有破图（naturalWidth === 0）
 *   5. 没有横向溢出、没有 JS 报错
 */
const { chromium } = require('playwright-core')
const CHROME = process.env.LOCALAPPDATA + '\\ms-playwright\\chromium-1243\\chrome-win64\\chrome.exe'
const BASE = 'http://localhost:3000'

let pass = 0, fail = 0
const problems = []
function check(page, name, ok, extra = '') {
  if (ok) { pass++; console.log(`  [OK]   ${name}${extra ? ' :: ' + extra : ''}`) }
  else { fail++; problems.push(`${page} → ${name}${extra ? ' :: ' + extra : ''}`); console.log(`  [坏]   ${name}${extra ? ' :: ' + extra : ''}`) }
}

/** 通用页面体检：返回结构化事实，交给调用方判断 */
async function inspect(page) {
  return await page.evaluate(() => {
    const visible = el => {
      const r = el.getBoundingClientRect()
      const s = getComputedStyle(el)
      return r.width > 0 && r.height > 0 && s.visibility !== 'hidden' && s.display !== 'none' && s.opacity !== '0'
    }
    const all = [...document.querySelectorAll('*')]
    const buttons = [...document.querySelectorAll('button, a[href], [role=button]')].filter(visible)
    const brokenImgs = [...document.querySelectorAll('img')]
      .filter(i => i.complete && i.naturalWidth === 0)
      .map(i => i.getAttribute('src') || '(no src)')
    const main = document.querySelector('.dh-main, main, #app > *') 
    return {
      bodyText: (document.body.innerText || '').replace(/\s+/g, ' ').trim().length,
      visibleButtons: buttons.length,
      buttonTexts: buttons.map(b => (b.innerText || b.textContent || '').trim()).filter(Boolean).slice(0, 20),
      brokenImgs,
      overflowX: document.documentElement.scrollWidth - document.documentElement.clientWidth,
      mainVisible: !!main && visible(main),
      rootChildren: document.getElementById('app')?.children.length || 0,
      h: document.body.scrollHeight,
    }
  })
}

async function pageCheck(page, path, name, opts = {}) {
  console.log(`\n--- ${name} (${path}) ---`)
  const errs = []
  const onErr = e => errs.push(e.message)
  page.on('pageerror', onErr)
  await page.goto(BASE + '/' + path, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(opts.wait || 1800)
  const r = await inspect(page)
  page.off('pageerror', onErr)

  check(name, '有内容渲染（非白屏）', r.bodyText > 60, `正文字符=${r.bodyText}`)
  check(name, '主区域可见', r.mainVisible || r.rootChildren > 0, `#app子节点=${r.rootChildren}`)
  check(name, '存在可交互按钮/链接', r.visibleButtons > 0, `数量=${r.visibleButtons}`)
  check(name, '无横向溢出', r.overflowX <= 2, `溢出=${r.overflowX}px`)
  check(name, '无破图', r.brokenImgs.length === 0, r.brokenImgs.join(', ') || '0 张')
  check(name, '无 JS 运行时报错', errs.length === 0, errs.slice(0, 2).join(' | ') || '0')
  if (opts.mustText) {
    // 取页面真实文本再匹配（inspect 返回的 bodyText 是字符数，不是字符串）
    const text = await page.evaluate(() => (document.body.innerText || '').replace(/\s+/g, ' '))
    for (const t of opts.mustText) {
      check(name, `包含关键文案「${t}」`, text.includes(t))
    }
  }
  return { r, errs }
}

;(async () => {
  const browser = await chromium.launch({ executablePath: CHROME, headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const page = await ctx.newPage()
  const consoleErrs = []
  page.on('console', m => { if (m.type() === 'error') consoleErrs.push(m.text()) })

  // ---------- 未登录：访客可见页面 ----------
  console.log('\n########## 访客视角（未登录） ##########')
  await pageCheck(page, '#/login', '登录页', {
    mustText: ['登录树洞', '用户名', '密码'],
  })
  await pageCheck(page, '#/register', '注册页', {
    mustText: ['注册'],
  })
  await pageCheck(page, '#/', '首页（未登录）', { mustText: [] })

  // ---------- 登录 ----------
  console.log('\n########## 登录 ##########')
  const user = 'dom_' + (Date.now() % 1000000)
  const pwd = 'Browser@123'
  await page.goto(BASE + '/#/register', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1500)
  // 注册页真实结构：用户名 placeholder="3-20 个字符"、按钮文案"注 册"（中间有空格）
  const inputs = page.locator('input')
  const n = await inputs.count()
  for (let i = 0; i < n; i++) {
    const ph = (await inputs.nth(i).getAttribute('placeholder')) || ''
    if (/个字符/.test(ph)) await inputs.nth(i).fill(user)
    else if (/至少 6 位/.test(ph)) await inputs.nth(i).fill(pwd)
    else if (/再次输入/.test(ph)) await inputs.nth(i).fill(pwd)
  }
  // 按钮文案含全角/半角空格，用宽松匹配
  await page.locator('button').filter({ hasText: /注\s*册/ }).first().click()
  await page.waitForTimeout(3000)
  const afterReg = page.url()
  const regMsg = await page.locator('.el-message, .dh-toast').last().innerText().catch(() => '')
  check('注册流程：能完成注册', afterReg.includes('login') || /成功/.test(regMsg), `url=${afterReg.split('#')[1]} msg=${regMsg}`)

  await page.goto(BASE + '/#/login', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1500)
  const li = page.locator('input')
  const ln = await li.count()
  for (let i = 0; i < ln; i++) {
    const ph = (await li.nth(i).getAttribute('placeholder')) || ''
    if (/用户名/.test(ph)) await li.nth(i).fill(user)
    else if (/密码/.test(ph)) await li.nth(i).fill(pwd)
  }
  await page.locator('button').filter({ hasText: /登\s*录/ }).first().click()
  await page.waitForTimeout(3000)
  check('登录流程：能登入系统', !page.url().includes('login'), page.url().split('#')[1])

  // ---------- 登录后逐页巡检 ----------
  console.log('\n########## 登录后：全站页面巡检 ##########')
  await pageCheck(page, '#/', '首页/信息流', {
    mustText: ['树洞'], wait: 2500,
  })
  await pageCheck(page, '#/square', '广场', { wait: 2200 })
  await pageCheck(page, '#/notifications', '消息中心', { wait: 2200 })
  await pageCheck(page, '#/profile', '我的主页', { wait: 2200 })
  await pageCheck(page, '#/settings', '设置', { mustText: ['头像'], wait: 2200 })
  await pageCheck(page, '#/collections', '我的收藏', { wait: 2200 })
  await pageCheck(page, '#/post/1', '帖子详情', { wait: 2500 })

  // ---------- 深色/浅色都看一眼首页 ----------
  console.log('\n########## 主题切换 ##########')
  await page.goto(BASE + '/#/', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1800)
  const themeBtn = page.locator('button[title*="主题"], button[aria-label*="主题"], .dh-theme-toggle, button:has(svg)')
  const darkTextLen = (await page.evaluate(() => document.body.innerText.replace(/\s+/g, '').length))
  await page.evaluate(() => document.documentElement.setAttribute('data-theme', 'light'))
  await page.waitForTimeout(900)
  const lightR = await inspect(page)
  check('浅色主题', '内容正常', lightR.bodyText > 60, `字符=${lightR.bodyText}`)
  check('浅色主题', '无破图', lightR.brokenImgs.length === 0, lightR.brokenImgs.join(', ') || '0 张')
  await page.evaluate(() => document.documentElement.setAttribute('data-theme', 'dark'))
  await page.waitForTimeout(700)

  // ---------- 关键交互：按钮能不能真点 ----------
  console.log('\n########## 关键交互可点性 ##########')
  await page.goto(BASE + '/#/', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(2200)
  // 点赞按钮
  const likeBtn = page.locator('.post button').filter({ hasText: /^\d+$/ }).first()
  if (await likeBtn.count() > 0) {
    const before = (await likeBtn.innerText()).trim()
    await likeBtn.click()
    await page.waitForTimeout(1200)
    const after = (await likeBtn.innerText()).trim()
    check('卡片点赞可点且数字变化', before !== after, `${before} → ${after}`)
  } else {
    check('卡片点赞按钮存在', false, '未找到')
  }
  // 分类筛选
  const cat = page.locator('.dh-nav button, aside button').first()
  if (await cat.count() > 0) {
    await cat.click()
    await page.waitForTimeout(1200)
    check('左侧分类可点', true, (await cat.innerText()).trim().slice(0, 8))
  } else {
    check('左侧分类存在', false, '未找到')
  }
  // 进入详情
  const card = page.locator('.post').first()
  if (await card.count() > 0) {
    await card.click()
    await page.waitForTimeout(2000)
    check('点击卡片进入详情', page.url().includes('/post/'), page.url().split('#')[1])
  } else {
    check('帖子卡片存在', false)
  }

  console.log('\n========================================')
  console.log(`通过 ${pass} / ${pass + fail}`)
  console.log(`控制台错误：${consoleErrs.length === 0 ? '无' : consoleErrs.length + ' 条'}`)
  if (consoleErrs.length) console.log(consoleErrs.slice(0, 5).join('\n'))
  if (problems.length) {
    console.log('\n---- 需要注意的项 ----')
    problems.forEach(p => console.log('  · ' + p))
  }
  await browser.close()
})().catch(e => { console.error('脚本异常:', e.message, e.stack?.split('\n')[1]); process.exit(2) })
