/**
 * 用户视角 DOM 巡检（管理端 permission-ui）
 *
 * 回答同一个问题：作为一名管理员，逐页打开管理后台，
 * 页面是否正常渲染、关键模块是否出现、操作按钮是否可见可点、有没有坏图与报错。
 *
 * 前置：后端 8081 + permission-ui（dev 或 preview）5173
 * 验证码：拦截登录页自己请求的 captchaKey，再从 Redis 取答案
 */
const { chromium } = require('playwright-core')
const { execFileSync } = require('child_process')

const ADMIN = 'http://localhost:5173'   // hash 路由
const REDIS_CLI = 'D:\\redis\\redis-cli.exe'
const CHROME = process.env.LOCALAPPDATA + '\\ms-playwright\\chromium-1243\\chrome-win64\\chrome.exe'

let pass = 0, fail = 0
const problems = []
function check(scope, name, ok, extra = '') {
  if (ok) { pass++; console.log(`  [OK]   ${name}${extra ? ' :: ' + extra : ''}`) }
  else { fail++; problems.push(`${scope} → ${name}${extra ? ' :: ' + extra : ''}`); console.log(`  [坏]   ${name}${extra ? ' :: ' + extra : ''}`) }
}
function section(t) { console.log(`\n########## ${t} ##########`) }

function redisGet(key) {
  try {
    return execFileSync(REDIS_CLI, ['-h', '127.0.0.1', '-p', '6379', '-a', 'redis123456', 'GET', key],
      { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim().replace(/^"|"$/g, '')
  } catch { return '' }
}

async function inspect(page) {
  return await page.evaluate(() => {
    const visible = el => {
      const r = el.getBoundingClientRect()
      const s = getComputedStyle(el)
      return r.width > 0 && r.height > 0 && s.visibility !== 'hidden' && s.display !== 'none' && s.opacity !== '0'
    }
    const btns = [...document.querySelectorAll('button, a[href], [role=button]')].filter(visible)
    return {
      text: (document.body.innerText || '').replace(/\s+/g, ' ').trim(),
      btnCount: btns.length,
      btnTexts: btns.map(b => (b.innerText || '').trim()).filter(Boolean).slice(0, 30),
      brokenImgs: [...document.querySelectorAll('img')].filter(i => i.complete && i.naturalWidth === 0)
        .map(i => i.getAttribute('src') || '(no src)'),
      overflowX: document.documentElement.scrollWidth - document.documentElement.clientWidth,
      hasTable: !!document.querySelector('.el-table'),
      hasEmptyText: /暂无数据|暂无|No Data/.test(document.body.innerText || ''),
    }
  })
}

async function pageCheck(page, hash, name, opts = {}) {
  console.log(`\n--- ${name} (${hash}) ---`)
  const errs = []
  const onErr = e => errs.push(e.message)
  page.on('pageerror', onErr)
  await page.goto(ADMIN + '/' + hash, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(opts.wait || 2200)
  const r = await inspect(page)
  page.off('pageerror', onErr)

  check(name, '有内容渲染（非白屏）', r.text.length > 40, `正文字符=${r.text.length}`)
  check(name, '存在可交互按钮/链接', r.btnCount > 0, `数量=${r.btnCount}`)
  check(name, '无横向溢出', r.overflowX <= 2, `溢出=${r.overflowX}px`)
  check(name, '无破图', r.brokenImgs.length === 0, r.brokenImgs.join(', ') || '0 张')
  check(name, '无 JS 运行时报错', errs.length === 0, errs.slice(0, 2).join(' | ') || '0')
  for (const t of opts.mustText || []) {
    check(name, `包含「${t}」`, r.text.includes(t))
  }
  for (const b of opts.mustBtn || []) {
    check(name, `按钮「${b}」在位`, r.btnTexts.some(x => x.includes(b)), r.btnTexts.slice(0, 8).join('/'))
  }
  return r
}

;(async () => {
  const browser = await chromium.launch({ executablePath: CHROME, headless: true })
  const page = await browser.newPage({ viewport: { width: 1600, height: 950 } })
  const consoleErrs = []
  page.on('console', m => { if (m.type() === 'error') consoleErrs.push(m.text()) })

  // ---------- 登录页 ----------
  section('访客视角')
  let captchaKey = ''
  page.on('response', async r => {
    if (r.url().includes('/api/auth/captcha') && !captchaKey) {
      try { const j = await r.json(); if (j?.data?.captchaKey) captchaKey = j.data.captchaKey } catch {}
    }
  })
  await pageCheck(page, '#/login', '登录页', {
    mustText: ['权限管理系统'], mustBtn: ['登'], wait: 1800,
  })

  // ---------- 登录 ----------
  section('登录')
  await page.getByPlaceholder('请输入用户名').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@1234')
  await page.waitForTimeout(900)
  const code = redisGet('captcha:' + captchaKey)
  await page.getByPlaceholder('验证码').fill(code)
  await page.getByRole('button', { name: /登\s*录/ }).first().click()
  await page.waitForTimeout(3000)
  check('登录', '成功进入系统', page.url().includes('dashboard'), page.url().split('#')[1])

  // ---------- 逐页巡检 ----------
  section('管理后台逐页巡检')
  await pageCheck(page, '#/dashboard', '首页看板', {
    mustText: ['系统', '用户'], wait: 3000,
  })
  await pageCheck(page, '#/system/user', '用户管理', {
    mustText: ['用户'], mustBtn: ['新增', '搜索'],
  })
  await pageCheck(page, '#/system/role', '角色管理', {
    mustText: ['角色'], mustBtn: ['新增角色', '搜索'],
  })
  await pageCheck(page, '#/system/menu', '菜单管理', { mustText: ['菜单'], wait: 2500 })
  await pageCheck(page, '#/system/dept', '部门管理', { mustText: ['部门'], wait: 2500 })
  await pageCheck(page, '#/log/operation', '操作日志', { wait: 2500 })
  await pageCheck(page, '#/log/login', '登录日志', { wait: 2500 })
  await pageCheck(page, '#/profile', '个人信息', { wait: 2200 })

  section('树洞管理逐页巡检')
  await pageCheck(page, '#/admin/th/user', '树洞用户', { mustText: ['用户'], mustBtn: ['搜索'], wait: 2500 })
  await pageCheck(page, '#/admin/th/post', '帖子管理', { wait: 2500 })
  await pageCheck(page, '#/admin/th/comment', '评论管理', { wait: 2500 })
  await pageCheck(page, '#/admin/th/report', '举报管理', { wait: 2500 })
  await pageCheck(page, '#/admin/th/moderation', '内容审核', { wait: 2500 })
  await pageCheck(page, '#/admin/th/sensitive', '敏感词管理', { wait: 2500 })
  await pageCheck(page, '#/admin/th/category', '分类管理', { wait: 2500 })
  await pageCheck(page, '#/admin/th/announcement', '公告管理', { wait: 2500 })
  await pageCheck(page, '#/admin/th/analytics', '数据分析', { wait: 3000 })
  await pageCheck(page, '#/admin/th/settings', '站点配置', { wait: 2500 })
  await pageCheck(page, '#/admin/th/online', '在线用户', { wait: 2500 })
  await pageCheck(page, '#/admin/th/logs', '树洞操作日志', { wait: 2500 })

  console.log('\n========================================')
  console.log(`通过 ${pass} / ${pass + fail}`)
  console.log(`控制台错误：${consoleErrs.length === 0 ? '无' : consoleErrs.length + ' 条'}`)
  if (consoleErrs.length) console.log(consoleErrs.slice(0, 6).join('\n'))
  if (problems.length) {
    console.log('\n---- 需要注意的项 ----')
    problems.forEach(p => console.log('  · ' + p))
  }
  await browser.close()
})().catch(e => { console.error('脚本异常:', e.message); process.exit(2) })
