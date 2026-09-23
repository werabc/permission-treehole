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

  // 滚动入场：首页首屏被 Hero/精选占满，帖子在下面，必须先滚到列表再断言
  await page.evaluate(() => document.querySelector('.filters')?.scrollIntoView())
  await page.waitForTimeout(1400)
  const revealed = await page.locator('.dh-reveal.is-in').count()
  check('滚动入场动效已触发', revealed > 0, `已入场=${revealed}`)
  await page.evaluate(() => window.scrollTo(0, 0))
  await page.waitForTimeout(600)

  // ---- App Shell（布局 3）----
  const sidenavBox = await page.locator('.dh-sidenav__in').boundingBox().catch(() => null)
  check('宽屏左侧导航常驻（App Shell）', !!sidenavBox && sidenavBox.width > 150,
    sidenavBox ? `宽=${Math.round(sidenavBox.width)}px` : 'null')
  const navCatCount = await page.locator('.dh-sidenav__cat').count()
  check('左侧导航含分类且带帖子数', navCatCount >= 2, `分类项=${navCatCount}`)
  check('置顶帖抽成「精选」卡', (await page.locator('.dh-spot').count()) === 1)
  const feedCount = await page.locator('.feed .post').count()
  check('精选帖不再重复出现在流里', feedCount > 0 && feedCount <= 9, `流内=${feedCount}`)
  check('公告改为右栏卡片', (await page.locator('.dh-ann').count()) > 0)

  // 计数口径：th_category.post_count 历史上没人维护，后端已改成实时聚合，这里守住一致性
  const allPillText = await page.locator('.filters .dh-pill i').first().innerText()
  const totalText = await page.locator('.filters__sp').innerText()
  const totalNum = Number((totalText.match(/\d+/) || [0])[0])
  check('「全部」计数与「共 N 条」一致', Number(allPillText) === totalNum,
    `全部=${allPillText} 共=${totalNum}`)
  check('星云层渲染（深色）', (await page.locator('.dh-nebula').count()) === 3)
  check('旧网格已移除', (await page.locator('.dh-bg__grid').count()) === 0)
  check('银河带渲染（深色）', (await page.locator('.dh-bg__band').count()) === 1)

  await shot(page, '01-home-dark')

  // ==================== 2. 浅色主题 ====================
  console.log('\n=== 2. 浅色主题切换 ===')
  await page.locator('.dh-hdr__acts .dh-icon-btn').nth(1).click()
  await page.waitForTimeout(900)
  check('主题切到 light', (await page.getAttribute('html', 'data-theme')) === 'light')
  check('浅色下星云仍在（改 multiply 混色）', (await page.locator('.dh-nebula').count()) === 3)
  check('浅色下星空 canvas 隐藏', !(await page.locator('canvas.dh-bg__stars').isVisible().catch(() => false)))
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

  // 关键：必须校验「真实尺寸」而不是节点是否存在。
  // 之前只数 .auth__art img 的个数，即使左侧插画被压成 0px 宽也能通过，
  // 导致 App Shell 两列网格把访客页挤坏的问题一直没被这条断言发现。
  const authBox = await page.evaluate(() => {
    const auth = document.querySelector('.auth')
    const art = document.querySelector('.auth__art')
    const box = document.querySelector('.auth__box')
    return {
      authW: auth ? Math.round(auth.getBoundingClientRect().width) : 0,
      artW: art ? Math.round(art.getBoundingClientRect().width) : 0,
      boxX: box ? Math.round(box.getBoundingClientRect().x) : 0,
      vw: document.documentElement.clientWidth,
    }
  })
  check('登录页整屏铺满（不被外壳网格挤成窄列）',
    authBox.authW >= authBox.vw - 2, `auth=${authBox.authW}px 视口=${authBox.vw}px`)
  check('左侧插画占住分屏一半（未被压成 0 宽）',
    authBox.artW > authBox.vw * 0.35, `art=${authBox.artW}px`)
  check('表单位于右半屏', authBox.boxX > authBox.vw * 0.5, `box.x=${authBox.boxX}px`)
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

  // 发表一条回响，验证评论链路（用发布前后差值断言，避免依赖演示数据的条数）
  const cmtBefore = await page.locator('.cmt').count()
  await clearMessages(page)
  await page.locator('textarea.composer__in').fill('这是一条来自浏览器验收脚本的回响。')
  await page.locator('.composer__foot .dh-btn--primary').click()
  await page.waitForTimeout(1800)
  const cmtMsg = await msgText(page)
  check('发表回响成功', /成功|回响/.test(cmtMsg), cmtMsg)
  const cmtCount = await page.locator('.cmt').count()
  check('回响列表已刷新', cmtCount === cmtBefore + 1, `${cmtBefore} → ${cmtCount}`)

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

  // ==================== 12. 布局 3 的交互与按钮动效 ====================
  console.log('\n=== 12. 交互与按钮动效 ===')
  await go(page, `${TREE}/#/`)
  await page.waitForTimeout(1800)

  // 公告手风琴（演示库至少有 1 条公告；用第一条验证展开/收起）
  const ann1 = page.locator('.dh-ann').first()
  if (await ann1.count()) {
    check('公告默认展开第一条', await ann1.evaluate((el) => el.classList.contains('is-on')))
    await ann1.locator('.dh-ann__t').click()
    await page.waitForTimeout(400)
    check('点击可收起公告', !(await ann1.evaluate((el) => el.classList.contains('is-on'))))
    await ann1.locator('.dh-ann__t').click()
    await page.waitForTimeout(400)
    check('再次点击可展开并显示正文', await ann1.locator('.dh-ann__bd').isVisible())
  } else {
    check('公告卡片渲染', false, '未渲染任何 .dh-ann')
  }

  // 按钮磁吸：把指针移到按钮右下角，应产生 --tx/--ty 位移变量
  const ctaBtn = page.locator('.hero__cta .dh-btn').first()
  const bb = await ctaBtn.boundingBox()
  await page.mouse.move(bb.x + bb.width * 0.86, bb.y + bb.height * 0.78)
  await page.waitForTimeout(420)
  const tx = await ctaBtn.evaluate((el) => el.style.getPropertyValue('--tx'))
  const mx = await ctaBtn.evaluate((el) => el.style.getPropertyValue('--mx'))
  check('按钮磁吸产生位移（--tx）', /-?\d+(\.\d+)?px/.test(tx), `--tx=${tx || '空'}`)
  check('按钮聚光跟随写入光标位置（--mx）', /%$/.test(mx), `--mx=${mx || '空'}`)

  // 涟漪：按下后应临时插入 .dh-rip
  await page.mouse.down()
  await page.waitForTimeout(120)
  const ripCount = await ctaBtn.locator('.dh-rip').count()
  await page.mouse.up()
  check('按下产生涟漪节点', ripCount > 0, `涟漪=${ripCount}`)
  await page.waitForTimeout(800)
  check('涟漪自动移除（不堆积 DOM）', (await ctaBtn.locator('.dh-rip').count()) === 0)
  await go(page, `${TREE}/#/`)   // 别让上面那次 press 真的触发跳转
  await page.waitForTimeout(1500)

  // 分类筛选写入 URL，左侧导航同步高亮
  await page.locator('.filters .dh-pill').nth(1).click()
  await page.waitForTimeout(1600)
  check('分类筛选写入 URL（可分享/可后退）', /category=\d+/.test(page.url()), page.url())
  check('左侧导航分类同步高亮', (await page.locator('.dh-sidenav__cat.is-on').count()) > 0)
  const filteredTotal = await page.locator('.filters__sp').innerText()
  check('筛选后总数文案更新', /共\s*\d+\s*条/.test(filteredTotal), filteredTotal)
  await shot(page, '12-home-category-filter')

  // 触底自动加载
  await go(page, `${TREE}/#/`)
  await page.waitForTimeout(1800)
  const beforeLoad = await page.locator('.feed .post').count()
  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
  await page.waitForTimeout(2600)
  const afterLoad = await page.locator('.feed .post').count()
  check('触底自动加载更多', afterLoad > beforeLoad, `${beforeLoad} → ${afterLoad}`)

  // 回到顶部
  check('滚动后出现回到顶部按钮', await page.locator('.dh-totop').isVisible().catch(() => false))
  await page.locator('.dh-totop').click()
  await page.waitForTimeout(1400)
  check('点击后回到顶部', (await page.evaluate(() => window.scrollY)) < 80,
    `scrollY=${await page.evaluate(() => window.scrollY)}`)

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
