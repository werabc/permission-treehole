/**
 * 树洞前端改版 —— 真实浏览器验收脚本
 *
 * 覆盖：首页(深/浅主题)、搜索、注册、登录、发布、消息中心、个人主页、帖子详情
 * 断言：无 console/page 错误、关键元素渲染、图标为真 SVG、背景层生效、动效类挂上
 *
 * 运行：export NODE_PATH=... && node scripts/verify_treehole_ui.cjs
 */
const path = require('path')
const fs = require('fs')
const { chromium } = require('playwright-core')

const HOME = process.env.USERPROFILE || process.env.HOME
const EXE = path.join(HOME, 'AppData', 'Local', 'ms-playwright', 'chromium-1243', 'chrome-win64', 'chrome.exe')
const TREE = 'http://localhost:3000'
const OUT = 'D:/开发项目/s1/docs/ui-shots'
const U1 = 'br_u1'
const PWD = 'Browser@123'

  const errors = []
  const badResponses = []
  const results = []
let pass = 0
let fail = 0

function check(name, ok, extra = '') {
  results.push({ name, ok, extra })
  if (ok) { pass++; console.log(`  [PASS] ${name}${extra ? ' :: ' + extra : ''}`) }
  else { fail++; console.log(`  [FAIL] ${name}${extra ? ' :: ' + extra : ''}`) }
}

async function shot(page, name, full = false) {
  await page.screenshot({ path: `${OUT}/${name}.png`, fullPage: full })
}

/** 清掉上一页残留的 ElMessage，避免断言读到旧提示 */
async function clearMessages(page) {
  await page.evaluate(() => document.querySelectorAll('.el-message').forEach((n) => n.remove()))
}

/** 登录页/首页都不需要处理遮罩，这里只做导航 + 等网络空闲 */
async function go(page, url) {
  await page.goto(url, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1200)
  await clearMessages(page)
}

async function msgText(page, timeout = 8000) {
  try {
    await page.waitForSelector('.el-message', { timeout })
    return ((await page.locator('.el-message').last().textContent()) || '').trim()
  } catch {
    return ''
  }
}

;(async () => {
  fs.mkdirSync(OUT, { recursive: true })
  const browser = await chromium.launch({ headless: true, executablePath: EXE })
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 940 } })
  const page = await ctx.newPage()

  page.on('pageerror', (e) => errors.push('pageerror: ' + e.message))
  page.on('console', (m) => {
    if (m.type() === 'error') {
      const t = m.text()
      // 忽略无意义的 favicon/字体类噪声
      if (!/favicon|fonts\.g|net::ERR_/i.test(t)) errors.push('console: ' + t)
    }
  })
  page.on('requestfailed', (r) => {
    const u = r.url()
    if (!/favicon|fonts\.g/i.test(u)) errors.push('reqfail: ' + u + ' ' + (r.failure()?.errorText || ''))
  })
  // 记录所有 4xx/5xx 及其 URL —— 出问题时能直接定位到接口
  page.on('response', (r) => {
    if (r.status() >= 400) badResponses.push(`${r.status()} ${r.request().method()} ${r.url()}`)
  })

  // ==================== 1. 首页（深色） ====================
  console.log('\n=== 1. 首页（默认深色） ===')
  await go(page, `${TREE}/#/`)
  await page.waitForTimeout(1500)

  check('主题默认为 dark', (await page.getAttribute('html', 'data-theme')) === 'dark')
  check('Hero 主标题渲染', (await page.locator('h1').first().innerText()).includes('树洞'))
  const artOpacity = await page.evaluate(() => getComputedStyle(document.querySelector('.dh-bg__art')).opacity)
  check('极光背景图已加载并可见', parseFloat(artOpacity) > 0.1, `opacity=${artOpacity}`)
  const canvasBox = await page.locator('canvas.dh-bg__stars').boundingBox()
  check('星点 canvas 已铺满视口', !!canvasBox && canvasBox.width > 1000 && canvasBox.height > 500,
    canvasBox ? `${canvasBox.width}x${canvasBox.height}` : 'null')

  const cardCount = await page.locator('.post').count()
  check('帖子卡片渲染', cardCount > 0, `数量=${cardCount}`)

  const iconBox = await page.locator('.post').first().locator('svg use').first().boundingBox()
  check('图标为真实 SVG（替代 emoji）', !!iconBox && iconBox.width > 0,
    iconBox ? `${iconBox.width}x${iconBox.height}` : 'null')
  const emojiLeft = await page.evaluate(() =>
    /[\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}]/u.test(document.body.innerText))
  check('页面文本不再含 emoji 图标', !emojiLeft)

  const revealed = await page.locator('.dh-reveal.is-in').count()
  check('滚动入场动效已触发', revealed > 0, `已入场=${revealed}`)

  const tickerText = (await page.locator('.ticker__item').innerText().catch(() => '')) || ''
  check('公告轮播有内容', tickerText.length > 0, tickerText.slice(0, 22))
  await shot(page, '01-home-dark')

  // ==================== 2. 浅色主题 ====================
  console.log('\n=== 2. 浅色主题切换 ===')
  await page.locator('.dh-hdr__acts .dh-icon-btn').nth(1).click()
  await page.waitForTimeout(900)
  check('主题切到 light', (await page.getAttribute('html', 'data-theme')) === 'light')
  await shot(page, '02-home-light')
  await page.locator('.dh-hdr__acts .dh-icon-btn').nth(1).click()
  await page.waitForTimeout(700)
  check('主题切回 dark', (await page.getAttribute('html', 'data-theme')) === 'dark')

  // ==================== 3. 搜索 ====================
  console.log('\n=== 3. 搜索 ===')
  await go(page, `${TREE}/#/search?keyword=${encodeURIComponent('妈妈')}`)
  await page.waitForTimeout(1500)
  const searchCards = await page.locator('.post').count()
  check('搜索关键词命中结果', searchCards > 0, `命中=${searchCards}`)
  check('搜索页展示命中总数', (await page.locator('.hint').innerText()).includes('共找到'))
  await shot(page, '03-search')

  // ==================== 4. 帖子详情 ====================
  console.log('\n=== 4. 帖子详情 ===')
  await go(page, `${TREE}/#/post/2`)
  await page.waitForTimeout(1600)
  check('详情页正文渲染', (await page.locator('.detail__body').innerText()).includes('妈妈'))
  check('详情页动作栏存在', (await page.locator('.actbar .act').count()) >= 4)
  check('回响列表渲染', (await page.locator('.cmt').count()) > 0, `回响=${await page.locator('.cmt').count()}`)
  await shot(page, '04-post-detail')

  // 未登录时点赞应被引导到登录页，而不是白跑一次 401
  await page.locator('.actbar .act').first().click()
  await page.waitForTimeout(700)
  check('未登录点赞被引导登录（不产生 401 登出）', /#\/login/.test(page.url()), page.url())

  // ==================== 5. 注册（真实流程） ====================
  console.log('\n=== 5. 注册 ===')
  await go(page, `${TREE}/#/register`)
  check('注册页为分屏布局（左侧画面 + 右侧表单）', (await page.locator('.auth__art img').count()) === 1)
  const newUser = 'ui_' + (Date.now() % 1000000)

  // 先验前端校验：两次密码不一致
  await page.getByPlaceholder('3-20 个字符').fill('ui_mismatch')
  await page.getByPlaceholder('至少 6 位').fill('UiCheck@123')
  await page.getByPlaceholder('再次输入密码').fill('UiCheck@999')
  await page.getByRole('button', { name: /注\s*册/ }).click()
  await page.waitForTimeout(600)
  check('两次密码不一致被拦截', (await page.locator('.dh-error').count()) > 0,
    await page.locator('.dh-error').first().innerText().catch(() => ''))

  // 再验正常注册
  await page.getByPlaceholder('3-20 个字符').fill(newUser)
  await page.getByPlaceholder('至少 6 位').fill('UiCheck@123')
  await page.getByPlaceholder('再次输入密码').fill('UiCheck@123')
  await shot(page, '05-register')
  await page.getByRole('button', { name: /注\s*册/ }).click()
  const regMsg = await msgText(page)
  check('注册提交成功', /成功/.test(regMsg), regMsg)
  await page.waitForTimeout(1500)
  check('注册成功跳转登录页', /#\/login/.test(page.url()), page.url())

  // ==================== 6. 登录 ====================
  console.log('\n=== 6. 登录 ===')
  await go(page, `${TREE}/#/login`)
  check('登录页为分屏布局', (await page.locator('.auth__art img').count()) === 1)
  await page.getByPlaceholder('请输入用户名').fill(U1)
  await page.getByPlaceholder('请输入密码').fill(PWD)
  await shot(page, '06-login')

  // 空值校验
  await page.getByPlaceholder('请输入用户名').fill('')
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForTimeout(500)
  check('空用户名被前端拦截', (await page.locator('.dh-error').count()) > 0)

  await page.getByPlaceholder('请输入用户名').fill(U1)
  await page.getByPlaceholder('请输入密码').fill(PWD)
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await page.waitForTimeout(2500)
  check('登录成功并跳转首页', !/#\/login/.test(page.url()), page.url())
  const chip = (await page.locator('.dh-user-chip b').innerText().catch(() => '')) || ''
  check('顶栏显示登录用户昵称', chip.length > 0, chip)
  const badge = (await page.locator('.dh-badge').innerText().catch(() => '')) || ''
  check('顶栏未读消息徽标显示', badge.length > 0, `未读=${badge}`)

  // ==================== 6.5 已登录：点赞 / 收藏 ====================
  console.log('\n=== 6.5 已登录交互（点赞 / 收藏） ===')
  await go(page, `${TREE}/#/post/3`)
  await page.waitForTimeout(1600)
  // 脚本可重复运行：先读初始态，点击后断言取反，避免"重复点击反而取关"的假失败
  const likeBtn = page.locator('.actbar .act').first()
  const likeBefore = await likeBtn.evaluate((el) => el.classList.contains('on'))
  await likeBtn.click()
  await page.waitForTimeout(1300)
  check('点赞切换生效', (await likeBtn.evaluate((el) => el.classList.contains('on'))) === !likeBefore,
    `${likeBefore} → ${!likeBefore}`)

  const collectBtn = page.locator('.actbar .act').nth(1)
  const collectBefore = await collectBtn.evaluate((el) => el.classList.contains('on'))
  await collectBtn.click()
  await page.waitForTimeout(1300)
  check('收藏切换生效', (await collectBtn.evaluate((el) => el.classList.contains('on'))) === !collectBefore,
    `${collectBefore} → ${!collectBefore}`)
  await shot(page, '06b-post-detail-logged-in')

  // 发表一条回响，验证评论链路
  await clearMessages(page)
  await page.locator('textarea.composer__in').fill('这是一条来自浏览器验收脚本的回响。')
  await page.locator('.composer__foot .dh-btn--primary').click()
  await page.waitForTimeout(1800)
  const cmtMsg = await msgText(page)
  check('发表回响成功', /成功|回响/.test(cmtMsg), cmtMsg)
  const cmtCount = await page.locator('.cmt').count()
  check('回响列表已刷新', cmtCount >= 3, `回响=${cmtCount}`)

  // ==================== 7. 发布 ====================
  console.log('\n=== 7. 发布 ===')
  await go(page, `${TREE}/#/publish`)
  const body = `视觉验收用内容 ${Date.now()}`
  await page.locator('textarea.ta').fill(body)
  await page.waitForTimeout(400)
  const meter = (await page.locator('.meter span').first().innerText()) || ''
  check('字数计量随输入更新', /\d+ \/ 5000/.test(meter) && !meter.startsWith('0 /'), meter)
  check('匿名/实名分段控件存在', (await page.locator('.dh-seg button').count()) === 2)
  await shot(page, '07-publish')
  await page.getByRole('button', { name: /匿名发布|实名发布/ }).last().click()
  const pubMsg = await msgText(page)
  check('发布成功并跳转详情', /成功/.test(pubMsg), pubMsg)

  // ==================== 8. 消息中心 ====================
  console.log('\n=== 8. 消息中心 ===')
  await go(page, `${TREE}/#/notifications`)
  await page.waitForTimeout(1500)
  const noteCount = await page.locator('.note').count()
  check('消息列表渲染', noteCount > 0, `数量=${noteCount}`)
  check('未读消息高亮', (await page.locator('.note.unread').count()) > 0,
    `未读=${await page.locator('.note.unread').count()}`)
  check('消息页签 4 个', (await page.locator('.head-tabs button').count()) === 4)
  await shot(page, '08-notifications')

  // ==================== 9. 个人主页 ====================
  console.log('\n=== 9. 个人主页 ===')
  await go(page, `${TREE}/#/profile`)
  await page.waitForTimeout(1600)
  check('个人主页昵称渲染', (await page.locator('.prof__info h2').innerText()).length > 0,
    await page.locator('.prof__info h2').innerText())
  check('数据宫格 4 格', (await page.locator('.bento__cell').count()) === 4)
  check('我的心事列表渲染', (await page.locator('.item').count()) > 0, `数量=${await page.locator('.item').count()}`)
  await shot(page, '09-profile')

  // 删除按钮存在（作者自管理）
  check('帖子带删除入口', (await page.locator('.mini--danger').count()) > 0)

  // ==================== 10. 移动端 ====================
  console.log('\n=== 10. 移动端 (390x844) ===')
  await page.setViewportSize({ width: 390, height: 844 })
  await go(page, `${TREE}/#/`)
  await page.waitForTimeout(1600)
  const overflow = await page.evaluate(() =>
    document.documentElement.scrollWidth - document.documentElement.clientWidth)
  check('移动端无横向溢出', overflow <= 1, `溢出=${overflow}px`)
  await shot(page, '10-mobile-home')
  await page.setViewportSize({ width: 1440, height: 940 })

  // ==================== 11. 非法路由参数 ====================
  console.log('\n=== 11. 非法路由参数（防 /api/th/post/NaN → 500） ===')
  await go(page, `${TREE}/#/post/abc`)
  await page.waitForTimeout(1500)
  check('非法帖子 ID 给出友好提示', (await page.locator('.dh-state').count()) > 0,
    (await page.locator('.dh-state').first().innerText().catch(() => '')))
  await go(page, `${TREE}/#/user/abc`)
  await page.waitForTimeout(1500)
  check('非法用户 ID 给出友好提示', (await page.locator('.dh-state').count()) > 0,
    (await page.locator('.dh-state').first().innerText().catch(() => '')))

  // ==================== 汇总 ====================
  console.log('\n========================================')
  console.log(`通过 ${pass} / ${pass + fail}`)
  console.log('控制台错误：', errors.length ? '\n  - ' + errors.join('\n  - ') : '无')
  console.log('4xx/5xx 响应：', badResponses.length ? '\n  - ' + badResponses.join('\n  - ') : '无')
  const failed = results.filter((r) => !r.ok)
  if (failed.length) {
    console.log('\n失败项：')
    failed.forEach((f) => console.log(`  - ${f.name} ${f.extra}`))
  }
  fs.writeFileSync(`${OUT}/_result.json`, JSON.stringify({ pass, fail, errors, badResponses, results }, null, 2))
  console.log('========================================')

  await browser.close()
  process.exit(fail > 0 || errors.length > 0 || badResponses.length > 0 ? 1 : 0)
})().catch((e) => {
  console.error('FATAL', e)
  process.exit(2)
})
