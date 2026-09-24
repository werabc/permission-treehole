/**
 * 越权写防护 —— 真实后端攻击验证（直接打 HTTP 接口，不经浏览器）
 *
 * 验证目标：DataScopeGuard 是否真的挡住了"看不见却能改"的越权写。
 *
 * 场景：
 *   1. 用 admin 登录（全部数据范围），建 2 个部门 + 2 个用户 + 1 个"研发部主管"角色
 *   2. 研发部主管：data_scope = 本部门及以下，只对自己部门可见
 *   3. 换该账号登录，发起越权写请求 → 必须全部被 5002(DATA_FORBIDDEN) 拒绝
 *   4. 发起范围内写请求 → 必须成功
 *   5. 清理全部测试数据
 *
 * 说明：沙箱禁止从 node 里 spawn 子进程（EBUSY），所以取验证码走 Redis 原生 TCP，
 *       建数据走真实 HTTP 接口，不依赖 mysql/redis-cli 命令行。
 *
 * 用法：node scripts/verify_write_scope.cjs
 */
const net = require('net')

const BASE = 'http://127.0.0.1:8081'
const DB = 'permission_admin_e2e'
const STAMP = Date.now().toString().slice(-8)
const PWD = 'Admin@1234'

/** 极简 Redis 客户端（只用到 GET），避免 spawn 子进程 */
function redisGet(key) {
  return new Promise(resolve => {
    const sock = net.createConnection({ host: '127.0.0.1', port: 6379 })
    let buf = ''
    let settled = false
    const done = v => { if (settled) return; settled = true; try { sock.destroy() } catch {} ; resolve(v) }
    sock.setTimeout(2000, () => done(''))
    sock.on('error', () => done(''))
    sock.on('data', d => {
      buf += d.toString('utf-8')
      // 期待 "+OK" 后跟 bulk string：$<len>\r\n<value>\r\n
      const m = buf.match(/\$(\d+)\r\n([\s\S]*?)\r\n/)
      if (m) done(m[2].replace(/^"|"$/g, ''))
    })
    sock.on('end', () => {
      const m = buf.match(/\$(\d+)\r\n([\s\S]*?)\r\n/)
      done(m ? m[2].replace(/^"|"$/g, '') : '')
    })
    const auth = '*2\r\n$4\r\nAUTH\r\n$11\r\nredis123456\r\n'
    const get = `*2\r\n$3\r\nGET\r\n$${Buffer.byteLength(key)}\r\n${key}\r\n`
    sock.write(auth + get)
    setTimeout(() => done(''), 1800)
  })
}

let pass = 0, fail = 0
const lines = []

/**
 * 兜底清理：无论脚本正常结束还是中途断言失败，都把本轮 stamp 产生的数据清干净。
 * 只删"本脚本自己创建"的（按 username/dept_name/role_code 后缀 STAMP 精确匹配），
 * 不会误伤既有数据。
 */
let ADMIN_TOKEN = null

/** 分页拉全量（接口默认分页，单页可能拿不全，这里循环到拿满） */
async function findAll(token, path, key) {
  const out = []
  for (let p = 1; p <= 10; p++) {
    const r = await req('GET', `${path}?pageNum=${p}&pageSize=100`, token)
    const rec = r.data?.records || r.data?.list || []
    out.push(...rec)
    const total = r.data?.total ?? out.length
    if (out.length >= total || rec.length === 0) break
  }
  return out
}

/**
 * 兜底清理：顺序 **角色 → 用户 → 部门**，且每步都校验结果、失败重试。
 * 顺序不可颠倒：部门删除会被"部门下存在用户(4003)"挡住，用户删除又会连带清 sys_user_role。
 * 只删本轮 STAMP 产生的数据，不会误伤既有数据。
 */
async function finalCleanup() {
  if (!ADMIN_TOKEN) return
  try {
    // 1) 角色（先断开与用户的关联）
    for (const r of (await findAll(ADMIN_TOKEN, '/api/role/page')).filter(x => (x.roleCode || '').includes(STAMP))) {
      await req('DELETE', `/api/role/${r.id}`, ADMIN_TOKEN)
    }
    // 2) 用户：删除后必须确认已不可见，否则重试（限流/瞬时失败都会在这里被纠回）
    for (let attempt = 0; attempt < 3; attempt++) {
      const users = (await findAll(ADMIN_TOKEN, '/api/user/page')).filter(u => (u.username || '').includes(STAMP))
      if (!users.length) break
      await req('DELETE', `/api/user/${users.map(u => u.id).join(',')}`, ADMIN_TOKEN)
      await new Promise(r => setTimeout(r, 300))
    }
    // 3) 部门：同样校验 + 重试；叶子优先（先删有 parentId 的，再删顶级）
    for (let attempt = 0; attempt < 3; attempt++) {
      const tree = await req('GET', '/api/dept/tree', ADMIN_TOKEN)
      const flat = []
      const walk = l => { for (const d of l || []) { flat.push(d); walk(d.children) } }
      walk(Array.isArray(tree.data) ? tree.data : [])
      const mine = flat.filter(x => (x.deptName || '').includes(STAMP))
      if (!mine.length) break
      for (const d of mine) await req('DELETE', `/api/dept/${d.id}`, ADMIN_TOKEN)
      await new Promise(r => setTimeout(r, 300))
    }
  } catch (e) {
    console.error('[兜底清理] 出错：', e.message)
  }
}
const check = (g, name, cond, extra = '') => {
  if (cond) { pass++; lines.push(`  [PASS] ${name}${extra ? ' :: ' + extra : ''}`) }
  else { fail++; lines.push(`  [FAIL] ${name}${extra ? ' :: ' + extra : ''}`) }
}
const section = t => lines.push(`\n=== ${t} ===`)

/** 真实登录：拿验证码 → 从 redis 取明文 → 提交 */
async function login(username, password) {
  const capResp = await fetch(`${BASE}/api/auth/captcha`).then(r => r.json())
  const captchaKey = capResp?.data?.captchaKey
  if (!captchaKey) throw new Error('未取到 captchaKey: ' + JSON.stringify(capResp).slice(0, 200))
  const code = await redisGet('captcha:' + captchaKey)
  const r = await fetch(`${BASE}/api/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password, captchaKey, captchaCode: code }),
  })
  const body = await r.json().catch(() => null)
  return { status: r.status, body, token: body?.data?.accessToken }
}

/** 带 token 请求，返回 {http, code, msg, data} */
async function req(method, path, token, payload) {
  const r = await fetch(BASE + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    body: payload === undefined ? undefined : JSON.stringify(payload),
  })
  const body = await r.json().catch(() => null)
  return { http: r.status, code: body?.code, msg: body?.message || body?.msg, data: body?.data }
}
const write = req

/** 按关键字在用户/部门分页里找 id（不依赖 mysql 命令行） */
async function findUserId(token, username) {
  const r = await req('GET', `/api/user/page?pageNum=1&pageSize=200&keyword=${encodeURIComponent(username)}`, token)
  const rec = r.data?.records || r.data?.list || (Array.isArray(r.data) ? r.data : [])
  return (rec.find(u => u.username === username) || {}).id
}
async function findDeptId(token, deptName) {
  const r = await req('GET', '/api/dept/tree', token)
  const walk = list => {
    for (const d of list || []) {
      if (d.deptName === deptName) return d.id
      const hit = walk(d.children)
      if (hit) return hit
    }
    return null
  }
  return walk(Array.isArray(r.data) ? r.data : [])
}
async function findRoleId(token, roleCode) {
  const r = await req('GET', `/api/role/page?pageNum=1&pageSize=200`, token)
  const rec = r.data?.records || r.data?.list || (Array.isArray(r.data) ? r.data : [])
  return (rec.find(x => x.roleCode === roleCode) || {}).id
}

;(async () => {
  section('准备测试数据（admin 操作）')
  const adminLogin = await login('admin', PWD)
  check('准备', 'admin 登录成功', !!adminLogin.token, adminLogin.token ? 'ok' : JSON.stringify(adminLogin.body).slice(0, 200))
  const adminToken = adminLogin.token
  ADMIN_TOKEN = adminToken
  if (!adminToken) { console.log(lines.join('\n')); process.exit(1) }

  // 两个部门
  const devDeptName = `验证研发部${STAMP}`
  const finDeptName = `验证财务部${STAMP}`
  await write('POST', '/api/dept', adminToken, { deptName: devDeptName, parentId: 0, sort: 999, status: 1 })
  await write('POST', '/api/dept', adminToken, { deptName: finDeptName, parentId: 0, sort: 998, status: 1 })
  const devDeptId = await findDeptId(adminToken, devDeptName)
  const finDeptId = await findDeptId(adminToken, finDeptName)
  check('准备', '研发部已创建', !!devDeptId, `id=${devDeptId}`)
  check('准备', '财务部已创建', !!finDeptId, `id=${finDeptId}`)

  // 两个用户，分别落在两个部门
  const devUser = `scope_dev_${STAMP}`
  const finUser = `scope_fin_${STAMP}`
  await write('POST', '/api/user', adminToken, { username: devUser, password: PWD, nickname: '研发部主管', deptId: Number(devDeptId), status: 1 })
  await write('POST', '/api/user', adminToken, { username: finUser, password: PWD, nickname: '财务部专员', deptId: Number(finDeptId), status: 1 })
  const devUserId = await findUserId(adminToken, devUser)
  const finUserId = await findUserId(adminToken, finUser)
  check('准备', '研发部用户已创建', !!devUserId, `id=${devUserId}`)
  check('准备', '财务部用户已创建', !!finUserId, `id=${finUserId}`)

  // "研发部主管"角色：本部门及以下 + 用户管理全部权限码
  const roleName = `研发主管${STAMP}`
  const roleCode = `scope_lead_${STAMP}`
  await write('POST', '/api/role', adminToken, { roleName, roleCode, dataScope: 4, status: 1, roleDesc: '越权验证用' })
  const roleId = await findRoleId(adminToken, roleCode)
  check('准备', '研发部主管角色已创建', !!roleId, `id=${roleId}`)

  // 给这个角色配上用户管理的所有按钮权限
  if (roleId) {
    const menuResp = await req('GET', '/api/menu/tree', adminToken)
    const flat = []
    const walk = list => { for (const m of list || []) { flat.push(m); walk(m.children) } }
    walk(Array.isArray(menuResp.data) ? menuResp.data : [])
    const permIds = flat.filter(m => (m.permission || '').startsWith('system:user:')).map(m => m.id)
    const setResp = await write('PUT', `/api/role/${roleId}/menus`, adminToken, { menuIds: permIds })
    check('准备', '角色已配置用户管理权限', permIds.length > 0 && setResp.code === 200, `权限码=${permIds.length} setCode=${setResp.code}`)
  }
  // 把研发部用户设为该角色
  if (roleId && devUserId) {
    const rr = await write('PUT', `/api/user/${devUserId}/roles`, adminToken, { roleIds: [Number(roleId)] })
    check('准备', '研发部主管已分配角色', rr.code === 200, `code=${rr.code}`)
  }
  // ---------- 用研发部主管登录 ----------
  const devLogin = await login(devUser, PWD)
  const devToken = devLogin.token
  if (!devToken) {
    check('登录', '研发部主管登录成功', false, JSON.stringify(devLogin.body).slice(0, 300))
    console.log(lines.join('\n')); process.exit(1)
  }
  check('登录', '研发部主管登录成功', true)

  // 确认读写隔离基线：能看见自己、看不见财务部
  section('读写隔离基线')
  const selfRead = await req('GET', `/api/user/${devUserId}`, devToken)
  check('读隔离', '可读本部门用户（读侧正常）', selfRead.code === 200, `code=${selfRead.code}`)
  const finRead = await req('GET', `/api/user/${finUserId}`, devToken)
  check('读隔离', '读不到财务部用户（读侧隔离生效）', finRead.code !== 200, `code=${finRead.code}`)

  section('越权写攻击（研发部主管 → 财务部用户）')
  const attacks = []
  attacks.push(['改他人资料', await write('PUT', `/api/user/${finUserId}`, devToken, { id: Number(finUserId), username: finUser, nickname: '被篡改', deptId: Number(finDeptId) })])
  attacks.push(['禁用他人账号', await write('PUT', `/api/user/${finUserId}/status`, devToken, { status: 0 })])
  attacks.push(['重置他人密码', await write('PUT', `/api/user/${finUserId}/reset-password`, devToken, { password: 'Hacked@1234' })])
  attacks.push(['给他人分配超管角色', await write('PUT', `/api/user/${finUserId}/roles`, devToken, { roleIds: [1] })])
  attacks.push(['删除他人账号', await write('DELETE', `/api/user/${finUserId}`, devToken)])

  for (const [name, r] of attacks) {
    const blocked = r.code === 5002 || r.http === 403
    check('越权拦截', `${name} 被拒`, blocked, `code=${r.code} http=${r.http} msg=${r.msg || ''}`)
  }

  section('内置超管保护（研发部主管 → admin id=1）')
  const b1 = await write('PUT', `/api/user/1/reset-password`, devToken, { password: 'Hacked@1234' })
  check('超管保护', '重置超管密码被拒', b1.code === 5002 || b1.http === 403, `code=${b1.code} msg=${b1.msg || ''}`)
  const b2 = await write('PUT', `/api/user/1/status`, devToken, { status: 0 })
  check('超管保护', '禁用超管被拒', b2.code === 5002 || b2.http === 403, `code=${b2.code} msg=${b2.msg || ''}`)
  const b3 = await write('DELETE', `/api/user/1`, devToken)
  check('超管保护', '删除超管被拒', b3.code === 5002 || b3.http === 403, `code=${b3.code} msg=${b3.msg || ''}`)

  section('范围内写操作（应成功）')
  const c1 = await write('PUT', `/api/user/${devUserId}/status`, devToken, { status: 1 })
  check('范围内可写', '改本部门用户状态成功', c1.code === 200, `code=${c1.code} msg=${c1.msg || ''}`)
  const c2 = await write('PUT', `/api/user/${devUserId}`, devToken, { id: Number(devUserId), username: devUser, nickname: '研发部主管改', deptId: Number(devDeptId), status: 1 })
  check('范围内可写', '改本部门用户资料成功', c2.code === 200, `code=${c2.code} msg=${c2.msg || ''}`)

  section('部门越权写（应被拒）')
  const d1 = await write('PUT', `/api/dept/${finDeptId}`, devToken, { id: Number(finDeptId), deptName: finDeptName + '改', parentId: 0, sort: 1, status: 1 })
  check('部门越权', '改范围外部门被拒', d1.code === 5002 || d1.http === 403, `code=${d1.code} msg=${d1.msg || ''}`)
  const d2 = await write('DELETE', `/api/dept/${finDeptId}`, devToken)
  check('部门越权', '删范围外部门被拒', d2.code === 5002 || d2.http === 403, `code=${d2.code} msg=${d2.msg || ''}`)
  const d3 = await write('POST', '/api/dept', devToken, { deptName: `偷建部门${STAMP}`, parentId: 0, sort: 1, status: 1 })
  check('部门越权', '非超管建顶级部门被拒', d3.code === 5002 || d3.http === 403, `code=${d3.code} msg=${d3.msg || ''}`)

  section('清理测试数据')
  if (roleId) await write('DELETE', `/api/role/${roleId}`, adminToken)
  // 顺序：角色 → 用户 → 部门。角色要先删，否则其 sys_user_role 关联会随用户删除而残留。
  const delIds = [devUserId, finUserId].filter(Boolean).join(',')
  if (delIds) await write('DELETE', `/api/user/${delIds}`, adminToken)
  const delU = await findUserId(adminToken, finUser)
  check('清理', '测试用户已删除', !delU, `残留=${delU || '无'}`)
  if (devDeptId) await write('DELETE', `/api/dept/${devDeptId}`, adminToken)
  if (finDeptId) await write('DELETE', `/api/dept/${finDeptId}`, adminToken)
  const delD = await findDeptId(adminToken, finDeptName)
  check('清理', '测试部门已删除', !delD, `残留=${delD || '无'}`)

  console.log('\n========================================')
  console.log('--- 完整明细 ---')
  console.log(lines.join('\n'))
  console.log('\n========================================')
  console.log(`通过 ${pass} / ${pass + fail}`)
  await finalCleanup()
  console.log('[兜底清理] 已执行')
  if (fail > 0) {
    console.log('\n--- 失败明细 ---')
    for (const l of lines) if (l.includes('[FAIL]')) console.log(l)
    process.exit(1)
  }
})().catch(async e => { console.error('脚本异常:', e); await finalCleanup(); process.exit(1) })
