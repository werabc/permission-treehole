/**
 * 越权防护「不误伤」验证 —— 确认加了守门员后，正常管理流程依然通畅
 *
 * 为什么必须有这个脚本：
 *   上一份 verify_write_scope.cjs 只证明"越权被拒"。一个过严的守卫同样能把
 *   越权拒掉，但会把合法管理员一起关在门外。安全性验证必须成对：拒绝该拒的、
 *   放行该放的，否则就是"把系统锁死"冒充"修好了"。
 *
 * 覆盖：
 *   1. admin（全部数据）可改/禁用/分配角色/重置普通用户密码 —— 不受影响
 *   2. admin 可改自己
 *   3. 部门主管可改本部门用户（跨部门层级但仍在范围内）
 *   4. 超管本人可改自己（内置保护不锁自己）
 *   5. 姓名/昵称改动真实落库
 *
 * 用法：node scripts/verify_guard_no_overreach.cjs
 */
const net = require('net')

const BASE = 'http://127.0.0.1:8081'
const STAMP = Date.now().toString().slice(-8)
const PWD = 'Admin@1234'
let pass = 0, fail = 0
const lines = []

/** 兜底清理：只删本轮 STAMP 产生的数据，正常/异常路径都执行 */
let ADMIN_TOKEN = null

/** 分页拉全量 */
async function findAll(token, path) {
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

/** 兜底清理：角色 → 用户 → 部门，每步校验 + 重试（顺序不可颠倒，否则 4003） */
async function finalCleanup() {
  if (!ADMIN_TOKEN) return
  try {
    for (const r of (await findAll(ADMIN_TOKEN, '/api/role/page')).filter(x => (x.roleCode || '').includes(STAMP))) {
      await req('DELETE', `/api/role/${r.id}`, ADMIN_TOKEN)
    }
    for (let i = 0; i < 3; i++) {
      const users = (await findAll(ADMIN_TOKEN, '/api/user/page')).filter(u => (u.username || '').includes(STAMP))
      if (!users.length) break
      await req('DELETE', `/api/user/${users.map(u => u.id).join(',')}`, ADMIN_TOKEN)
      await new Promise(r => setTimeout(r, 300))
    }
    for (let i = 0; i < 3; i++) {
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

function redisGet(key) {
  return new Promise(resolve => {
    const sock = net.createConnection({ host: '127.0.0.1', port: 6379 })
    let buf = ''; let settled = false
    const done = v => { if (settled) return; settled = true; try { sock.destroy() } catch {} ; resolve(v) }
    sock.setTimeout(2000, () => done(''))
    sock.on('error', () => done(''))
    sock.on('data', d => {
      buf += d.toString('utf-8')
      const m = buf.match(/\$(\d+)\r\n([\s\S]*?)\r\n/)
      if (m) done(m[2].replace(/^"|"$/g, ''))
    })
    const auth = '*2\r\n$4\r\nAUTH\r\n$11\r\nredis123456\r\n'
    const get = `*2\r\n$3\r\nGET\r\n$${Buffer.byteLength(key)}\r\n${key}\r\n`
    sock.write(auth + get)
    setTimeout(() => done(''), 1800)
  })
}

async function login(username, password) {
  const cap = await fetch(`${BASE}/api/auth/captcha`).then(r => r.json())
  const captchaKey = cap?.data?.captchaKey
  const code = await redisGet('captcha:' + captchaKey)
  const r = await fetch(`${BASE}/api/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password, captchaKey, captchaCode: code }),
  })
  const body = await r.json().catch(() => null)
  return { body, token: body?.data?.accessToken }
}
async function req(method, path, token, payload) {
  const r = await fetch(BASE + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    body: payload === undefined ? undefined : JSON.stringify(payload),
  })
  const body = await r.json().catch(() => null)
  return { http: r.status, code: body?.code, msg: body?.message, data: body?.data }
}
async function findUserId(token, username) {
  const r = await req('GET', `/api/user/page?pageNum=1&pageSize=200&keyword=${encodeURIComponent(username)}`, token)
  const rec = r.data?.records || r.data?.list || []
  return (rec.find(u => u.username === username) || {}).id
}

;(async () => {
  section('准备：admin 建一个普通用户')
  const admin = await login('admin', PWD)
  const adminToken = admin.token
  ADMIN_TOKEN = adminToken
  check('准备', 'admin 登录成功', !!adminToken)
  if (!adminToken) { console.log(lines.join('\n')); process.exit(1) }

  const uname = `overreach_${STAMP}`
  await req('POST', '/api/user', adminToken, { username: uname, password: PWD, nickname: '普通用户', deptId: 1, status: 1 })
  const uid = await findUserId(adminToken, uname)
  check('准备', '普通用户已创建', !!uid, `id=${uid}`)
  if (!uid) { console.log(lines.join('\n')); process.exit(1) }

  section('admin（全部数据）对普通用户：应全部放行')
  const u1 = await req('PUT', `/api/user/${uid}/status`, adminToken, { status: 0 })
  check('admin 放行', '禁用普通用户成功', u1.code === 200, `code=${u1.code} msg=${u1.msg}`)
  const u2 = await req('PUT', `/api/user/${uid}/status`, adminToken, { status: 1 })
  check('admin 放行', '重新启用成功', u2.code === 200, `code=${u2.code}`)

  const nick = `改名_${STAMP}`
  const u3 = await req('PUT', `/api/user/${uid}`, adminToken, { id: Number(uid), username: uname, nickname: nick, deptId: 1, status: 1 })
  check('admin 放行', '修改普通用户资料成功', u3.code === 200, `code=${u3.code} msg=${u3.msg}`)
  const after = await req('GET', `/api/user/${uid}`, adminToken)
  check('admin 放行', '昵称改动真实落库', after.data?.nickname === nick, `nickname=${after.data?.nickname}`)

  const u4 = await req('PUT', `/api/user/${uid}/roles`, adminToken, { roleIds: [2] })
  check('admin 放行', '给普通用户分配角色成功', u4.code === 200, `code=${u4.code} msg=${u4.msg}`)
  const u5 = await req('PUT', `/api/user/${uid}/reset-password`, adminToken, { password: 'NewPwd@1234' })
  check('admin 放行', '重置普通用户密码成功', u5.code === 200, `code=${u5.code} msg=${u5.msg}`)

  section('admin 改自己：应放行（内置保护不能锁住自己）')
  const me = await req('PUT', `/api/user/1`, adminToken, { id: 1, username: 'admin', nickname: '系统管理员', deptId: 1, status: 1 })
  check('admin 自改', 'admin 修改自己资料成功', me.code === 200, `code=${me.code} msg=${me.msg}`)
  const meRole = await req('GET', '/api/user/1/roles', adminToken)
  check('admin 自改', 'admin 可读自己角色', meRole.code === 200, `code=${meRole.code}`)

  section('部门主管：本部门用户应可管')
  // 建部门 + 部门内用户 + 主管角色（本部门及以下）
  const deptName = `放行验证部${STAMP}`
  await req('POST', '/api/dept', adminToken, { deptName, parentId: 0, sort: 997, status: 1 })
  const tree = await req('GET', '/api/dept/tree', adminToken)
  const walkD = list => { for (const d of list || []) { if (d.deptName === deptName) return d.id; const h = walkD(d.children); if (h) return h } return null }
  const deptId = walkD(Array.isArray(tree.data) ? tree.data : [])

  const lead = `over_lead_${STAMP}`
  const member = `over_mem_${STAMP}`
  await req('POST', '/api/user', adminToken, { username: lead, password: PWD, nickname: '主管', deptId: Number(deptId), status: 1 })
  await req('POST', '/api/user', adminToken, { username: member, password: PWD, nickname: '组员', deptId: Number(deptId), status: 1 })
  const leadId = await findUserId(adminToken, lead)
  const memberId = await findUserId(adminToken, member)
  check('准备', '主管与组员已创建', !!leadId && !!memberId, `lead=${leadId} member=${memberId}`)

  const roleCode = `over_lead_${STAMP}`
  await req('POST', '/api/role', adminToken, { roleName: `放行主管${STAMP}`, roleCode, dataScope: 4, status: 1 })
  const rp = await req('GET', '/api/role/page?pageNum=1&pageSize=200', adminToken)
  const roleId = ((rp.data?.records || []).find(x => x.roleCode === roleCode) || {}).id
  if (roleId) {
    const menu = await req('GET', '/api/menu/tree', adminToken)
    const flat = []; const walk = l => { for (const m of l || []) { flat.push(m); walk(m.children) } }
    walk(Array.isArray(menu.data) ? menu.data : [])
    const permIds = flat.filter(m => (m.permission || '').startsWith('system:user:')).map(m => m.id)
    await req('PUT', `/api/role/${roleId}/menus`, adminToken, { menuIds: permIds })
    await req('PUT', `/api/user/${leadId}/roles`, adminToken, { roleIds: [Number(roleId)] })
  }

  const leadLogin = await login(lead, PWD)
  const leadToken = leadLogin.token
  check('准备', '主管登录成功', !!leadToken)
  if (leadToken) {
    const g1 = await req('PUT', `/api/user/${memberId}/status`, leadToken, { status: 0 })
    check('主管放行', '禁用本部门组员成功', g1.code === 200, `code=${g1.code} msg=${g1.msg}`)
    const g2 = await req('PUT', `/api/user/${memberId}`, leadToken, { id: Number(memberId), username: member, nickname: '组员改', deptId: Number(deptId), status: 1 })
    check('主管放行', '修改本部门组员成功', g2.code === 200, `code=${g2.code} msg=${g2.msg}`)
    const g3 = await req('PUT', `/api/user/${leadId}`, leadToken, { id: Number(leadId), username: lead, nickname: '主管自改', deptId: Number(deptId), status: 1 })
    check('主管放行', '主管修改自己成功', g3.code === 200, `code=${g3.code} msg=${g3.msg}`)
  }

  section('清理')
  // 顺序很关键：先删角色（断开与用户的关联），再删用户，最后删部门。
  // 若反过来，用户删除会连带清掉 sys_user_role，导致角色残留。
  if (roleId) await req('DELETE', `/api/role/${roleId}`, adminToken)
  const delU = [uid, leadId, memberId].filter(Boolean).join(',')
  if (delU) await req('DELETE', `/api/user/${delU}`, adminToken)
  check('清理', '测试用户已删除', !(await findUserId(adminToken, uname)))
  if (deptId) await req('DELETE', `/api/dept/${deptId}`, adminToken)
  const treeAfter = await req('GET', '/api/dept/tree', adminToken)
  const walkAfter = l => { for (const d of l || []) { if (d.deptName === deptName) return d.id; const h = walkAfter(d.children); if (h) return h } return null }
  check('清理', '测试部门已删除', !walkAfter(Array.isArray(treeAfter.data) ? treeAfter.data : []))

  console.log('\n--- 完整明细 ---')
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
