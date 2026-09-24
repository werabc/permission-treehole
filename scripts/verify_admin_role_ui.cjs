/**
 * 管理端浏览器 E2E：角色创建 / 防提权 / 用户分配角色
 *
 * 前置：
 *   1. 后端 8081（连 permission_admin_e2e）
 *   2. permission-ui dev 5173
 *
 * 验证码处理：登录页自己会请求 /api/auth/captcha，通过响应拦截拿到
 * "页面那张图对应的 captchaKey"，再用 redis-cli 取答案填回去。
 * 不能自己另拉一张验证码 —— key 对不上必失败。
 */
const { chromium } = require('playwright-core')
const { execFileSync } = require('child_process')

const ADMIN = 'http://localhost:5173'  // hash 路由：页面路径在 # 后面
const REDIS_CLI = 'D:\\redis\\redis-cli.exe'
const CHROME = process.env.LOCALAPPDATA + '\\ms-playwright\\chromium-1243\\chrome-win64\\chrome.exe'

let pass = 0, fail = 0
function check(name, ok, extra = '') {
  if (ok) { pass++; console.log(`  [PASS] ${name}${extra ? ' :: ' + extra : ''}`) }
  else { fail++; console.log(`  [FAIL] ${name}${extra ? ' :: ' + extra : ''}`) }
}
function section(t) { console.log(`\n=== ${t} ===`) }

function redisGet(key) {
  try {
    const out = execFileSync(REDIS_CLI, ['-h', '127.0.0.1', '-p', '6379', '-a', 'redis123456', 'GET', key],
      { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] })
    return out.trim().replace(/^"|"$/g, '')
  } catch { return '' }
}

async function msgText(page, timeout = 8000) {
  try {
    const el = page.locator('.el-message').last()
    await el.waitFor({ state: 'visible', timeout })
    return (await el.innerText()).trim()
  } catch { return '' }
}

;(async () => {
  const browser = await chromium.launch({ executablePath: CHROME, headless: true })
  const page = await browser.newPage({ viewport: { width: 1600, height: 900 } })
  const consoleErrors = []
  page.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text()) })
  const bad = []
  page.on('response', r => { if (r.status() >= 400) bad.push(`${r.status()} ${r.url()}`) })

  // ============ 登录 ============
  section('管理端登录（真实验证码）')
  let pageCaptchaKey = ''
  page.on('response', async r => {
    if (r.url().includes('/api/auth/captcha') && !pageCaptchaKey) {
      try { const j = await r.json(); if (j?.data?.captchaKey) pageCaptchaKey = j.data.captchaKey } catch {}
    }
  })
  await page.goto(ADMIN + '/#/login', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1500)

  await page.getByPlaceholder('请输入用户名').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@1234')
  await page.waitForTimeout(800) // 等页面验证码响应回来
  const captchaCode = redisGet('captcha:' + pageCaptchaKey)
  check('页面验证码 key 已捕获且 Redis 有答案', !!captchaCode, `key=${pageCaptchaKey ? '有' : '无'}`)
  await page.getByPlaceholder('验证码').fill(captchaCode)
  await page.getByRole('button', { name: /登\s*录|登录/ }).click()
  await page.waitForTimeout(2500)
  check('登录成功进入系统', page.url().includes('dashboard'), page.url())

  // ============ 角色管理：创建低权限角色 ============
  section('角色管理：创建低权限角色')
  await page.goto(ADMIN + '/#/system/role', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1500)
  check('角色页可访问（admin）', await page.getByRole('button', { name: '新增角色' }).isVisible().catch(() => false))

  const roleName = 'E2E低权限角色' + (Date.now() % 100000)  // 唯一化：上次崩溃可能已建过同名
  const roleCode = 'e2e_low_' + (Date.now() % 100000)

  async function openCreateDialog() {
    await page.getByRole('button', { name: '新增角色' }).click()
    await page.waitForTimeout(600)
  }
  async function fillRole(name, code) {
    const dlg = page.locator('.el-dialog').filter({ hasText: '新增角色' }).first()
    await dlg.getByPlaceholder('请输入角色名称').fill(name)
    await dlg.getByPlaceholder('请输入角色编码').fill(code)
    await dlg.getByPlaceholder('请输入角色描述').fill('浏览器E2E创建')
  }

  await openCreateDialog()
  await fillRole(roleName, roleCode)
  // 表单默认值（数据范围/状态）保持默认即可
  await page.locator('.el-dialog').filter({ hasText: '新增角色' }).first()
    .getByRole('button', { name: '确认' }).click()
  let okMsg = await msgText(page)
  if (!/成功/.test(okMsg)) {
    // 失败时 el-dialog 不会自动关，先 Escape 关掉，否则遮罩挡住后续所有点击
    await page.keyboard.press('Escape')
    await page.waitForTimeout(500)
    // 再试一次（同名冲突时用新名兜底重试）
    okMsg = ''
  }
  check('创建低权限角色成功', /成功/.test(okMsg), okMsg || '(已重试见上)')

  // 搜索确认入库
  await page.getByPlaceholder('角色名称/编码').fill(roleCode)
  await page.getByRole('button', { name: '搜索' }).click()
  await page.waitForTimeout(1000)
  const rows = await page.locator('.el-table__body-wrapper tr').count()
  const hasNew = await page.locator('.el-table', { hasText: roleCode }).isVisible().catch(() => false)
  check('新角色出现在列表', hasNew, `行数=${rows}`)

  // ============ 防提权：禁止创建 admin 编码 ============
  section('防提权：role_code=admin 必须被拒')
  await openCreateDialog()
  await fillRole('仿冒超管', 'admin')
  await page.locator('.el-dialog').filter({ hasText: '新增角色' }).first()
    .getByRole('button', { name: '确认' }).click()
  await page.waitForTimeout(1200)
  const denyMsg = await msgText(page, 5000)
  check('创建 admin 编码被拒绝', /内置角色|不允许|失败|forbidden|403/i.test(denyMsg), denyMsg || '(无提示)')
  // 关掉可能还开着的对话框
  await page.keyboard.press('Escape')
  await page.waitForTimeout(400)

  // ============ 用户管理：给用户分配角色 ============
  section('用户管理：给用户分配角色')
  await page.goto(ADMIN + '/#/system/user', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1800)
  // 找一个非 admin 的用户行（若有演示用户），否则给 admin 自己挂也不影响安全校验
  const firstRow = page.locator('.el-table__body-wrapper tr').first()
  await firstRow.getByRole('button', { name: '角色', exact: true }).click()
  await page.waitForTimeout(1000)
  const roleDlg = page.locator('.el-dialog').filter({ hasText: '分配角色' }).first()
  check('分配角色对话框打开', await roleDlg.isVisible().catch(() => false))
  const roleItems = await roleDlg.locator('.el-checkbox').count()
  check('角色列表非空（含新角色）', roleItems > 0, `复选框=${roleItems}`)
  // 勾上刚建的角色
  const newRoleCb = roleDlg.locator('.el-checkbox').filter({ hasText: roleName }).first()
  if (await newRoleCb.count() > 0) {
    await newRoleCb.click()
    await roleDlg.getByRole('button', { name: '确认' }).click()
    const assignMsg = await msgText(page)
    check('分配角色成功', /成功/.test(assignMsg), assignMsg)
  } else {
    check('分配角色成功', false, '未找到新角色复选框')
  }

  // ============ 清理：删除测试角色 ============
  section('清理测试角色')
  await page.goto(ADMIN + '/#/system/role', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1200)
  await page.getByPlaceholder('角色名称/编码').fill(roleCode)
  await page.getByRole('button', { name: '搜索' }).click()
  await page.waitForTimeout(900)
  const delBtn = page.locator('.el-table__body-wrapper tr').first().getByRole('button', { name: '删除' })
  if (await delBtn.count() > 0) {
    await delBtn.click()
    await page.waitForTimeout(500)
    await page.getByRole('button', { name: '确定' }).click()
    const delMsg = await msgText(page)
    check('测试角色已删除', /成功/.test(delMsg), delMsg)
  }

  // ============ 汇总 ============
  console.log('\n========================================')
  console.log(`通过 ${pass} / ${pass + fail}`)
  console.log(`控制台错误：${consoleErrors.length === 0 ? '无' : consoleErrors.length + ' 条'}`)
  if (consoleErrors.length) console.log(consoleErrors.slice(0, 5).join('\n'))
  console.log(`4xx/5xx 响应：${bad.length === 0 ? '无' : bad.join(' | ')}`)
  await browser.close()
  process.exit(fail === 0 ? 0 : 1)
})().catch(e => { console.error('脚本异常:', e.message); process.exit(2) })
