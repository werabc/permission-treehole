const http = require('http');

const BASE = 'http://localhost:8080';
let ADMIN_TOKEN = '';

function api(method, path, data, token) {
  return new Promise((resolve, reject) => {
    const url = new URL(path, BASE);
    const body = data ? JSON.stringify(data) : undefined;
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const req = http.request({
      hostname: url.hostname,
      port: url.port,
      path: url.pathname + url.search,
      method,
      headers,
    }, res => {
      let buf = '';
      res.on('data', c => buf += c);
      res.on('end', () => {
        try { resolve(JSON.parse(buf)); }
        catch { resolve({ raw: buf, status: res.statusCode }); }
      });
    });
    req.on('error', reject);
    if (body) req.write(body);
    req.end();
  });
}

function log(msg, ok = true) {
  const prefix = ok ? '✓' : '✗';
  console.log(`  ${prefix} ${msg}`);
}

async function main() {
  console.log('========== 权限管理系统 - 用户数据填充脚本 ==========\n');

  // 1. Login as admin
  console.log('--- 1. Admin登录 ---');
  const loginRes = await api('POST', '/api/auth/login', { username: 'admin', password: 'admin123' });
  if (loginRes.code !== 200) { console.error('登录失败:', JSON.stringify(loginRes)); process.exit(1); }
  ADMIN_TOKEN = loginRes.data.accessToken;
  log('Admin登录成功', true);

  // 2. Check existing users and departments
  console.log('\n--- 2. 查询现有数据 ---');
  const usersRes = await api('GET', '/api/user/page?pageNum=1&pageSize=20', null, ADMIN_TOKEN);
  const existingUsers = usersRes.data.records.map(u => u.username);
  log(`现有用户: ${existingUsers.join(', ')}`);

  const deptRes = await api('GET', '/api/dept/tree', null, ADMIN_TOKEN);
  const depts = deptRes.data || [];
  function findDept(name) {
    for (const d of depts) {
      if (d.deptName === name) return d.id;
      if (d.children) {
        for (const c of d.children) if (c.deptName === name) return c.id;
      }
    }
    return depts[0]?.id || 1;
  }
  log(`部门数量: ${depts.length}`);

  // 3. Register default system users
  console.log('\n--- 3. 注册系统用户 ---');
  const defaultUsers = [
    { username: 'tech', nickname: '技术主管', password: 'Admin@1234', email: 'tech@company.com', phone: '13800000001', sex: 1, status: 1, deptId: findDept('技术部') },
    { username: 'backend', nickname: '后端开发', password: 'Admin@1234', email: 'backend@company.com', phone: '13800000002', sex: 1, status: 1, deptId: findDept('技术部') },
  ];

  for (const user of defaultUsers) {
    if (existingUsers.includes(user.username)) {
      log(`用户 ${user.username} 已存在，跳过`);
      continue;
    }
    const createRes = await api('POST', '/api/user', user, ADMIN_TOKEN);
    if (createRes.code === 200) {
      log(`创建用户: ${user.username} (${user.nickname})`, true);
    } else {
      log(`创建用户 ${user.username} 失败: ${createRes.message}`, false);
    }
  }

  // 4. Assign roles to users
  console.log('\n--- 4. 分配角色 ---');
  const rolesRes = await api('GET', '/api/role/page?pageNum=1&pageSize=20', null, ADMIN_TOKEN);
  const roles = rolesRes.data?.records || [];
  log(`找到 ${roles.length} 个角色: ${roles.map(r => r.roleName + '(' + r.roleCode + ')').join(', ')}`);

  const usersPage2 = await api('GET', '/api/user/page?pageNum=1&pageSize=30', null, ADMIN_TOKEN);
  const allUsers = usersPage2.data?.records || [];
  function getUserId(username) {
    const u = allUsers.find(u => u.username === username);
    return u ? u.id : null;
  }

  const techRole = roles.find(r => r.roleCode === 'tech');
  const userRole = roles.find(r => r.roleCode === 'user');

  if (techRole) {
    const techId = getUserId('tech');
    if (techId) {
      const res = await api('PUT', `/api/user/${techId}/roles`, { roleIds: [techRole.id] }, ADMIN_TOKEN);
      log(`分配技术主管角色给 tech: ${res.code === 200 ? '成功' : res.message}`, res.code === 200);
    }
  }

  if (userRole) {
    const backendId = getUserId('backend');
    if (backendId) {
      const res = await api('PUT', `/api/user/${backendId}/roles`, { roleIds: [userRole.id] }, ADMIN_TOKEN);
      log(`分配普通用户角色给 backend: ${res.code === 200 ? '成功' : res.message}`, res.code === 200);
    }
  }

  // Summary
  console.log('\n========== 数据填充完成 ==========');
  console.log('系统用户:');
  console.log('  - admin (超级管理员) — 全部权限');
  console.log('  - tech (技术主管) — 只读系统管理');
  console.log('  - backend (后端开发) — 只读系统管理');
  console.log('\n默认密码: Admin@1234');
}

main().catch(err => {
  console.error('脚本执行异常:', err.message);
  process.exit(1);
});
