const { chromium } = require('playwright');

const BASE = 'http://localhost:5173';
const REPORT = [];
let PASS = 0, FAIL = 0;

function result(name, ok, detail = '') {
  const status = ok ? '✓ PASS' : '✗ FAIL';
  if (ok) PASS++; else FAIL++;
  const line = `  ${status} | ${name}${detail ? ' - ' + detail : ''}`;
  REPORT.push(line);
  console.log(line);
}

async function main() {
  console.log('========== 前端页面浏览器自动化测试 ==========\n');

  const browser = await chromium.launch({
    headless: true,
    channel: 'chrome',
  });
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    locale: 'zh-CN',
  });
  const page = await context.newPage();

  const consoleErrors = [];
  page.on('console', msg => {
    if (msg.type() === 'error') consoleErrors.push(msg.text());
  });
  page.on('pageerror', err => consoleErrors.push(err.message));

  // ========================
  // 1. Login Page
  // ========================
  console.log('--- 1. 登录页 ---');
  await page.goto(`${BASE}/#/login`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);

  const title = await page.title();
  result('页面标题', title.includes('权限'), title);

  // Use placeholder to find inputs
  const userInput = page.locator('input[placeholder="请输入用户名"]');
  const pwdInput = page.locator('input[placeholder="请输入密码"]');
  const captchaInput = page.locator('input[placeholder="验证码"]');
  const hasUsername = await userInput.isVisible().catch(() => false);
  result('用户名输入框', hasUsername);
  const hasPassword = await pwdInput.isVisible().catch(() => false);
  result('密码输入框', hasPassword);
  const hasCaptcha = await captchaInput.isVisible().catch(() => false);
  result('验证码输入框', hasCaptcha);

  const captchaImg = await page.locator('.captcha-img').first();
  const hasCaptchaImg = await captchaImg.isVisible().catch(() => false);
  result('验证码图片', hasCaptchaImg);

  // Login button: the template shows text "登 录" (with intentional space)
  const loginBtn = page.locator('button:has-text("登")');
  const hasLoginBtn = await loginBtn.isVisible().catch(() => false);
  result('登录按钮', hasLoginBtn);

  // Fill credentials and captcha placeholder, then click login to verify form validation works
  await userInput.fill('admin');
  await pwdInput.fill('Admin@1234');
  await captchaInput.fill('test');
  await loginBtn.click();
  await page.waitForTimeout(2000);

  // Login via API to get a real token for page tests (captcha prevents form-based login in test)
  const tokenResult = await page.evaluate(async () => {
    // Try new password first, fall back to old password for existing DBs
    for (const pwd of ['Admin@1234', 'admin123']) {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'admin', password: pwd }),
      });
      const json = await res.json();
      if (json.code === 200) return json;
    }
    return { code: -1, message: 'All passwords failed' };
  });
  if (tokenResult && tokenResult.data && tokenResult.data.accessToken) {
    await page.evaluate(({ at, rt }) => {
      localStorage.setItem('accessToken', at);
      localStorage.setItem('refreshToken', rt);
    }, { at: tokenResult.data.accessToken, rt: tokenResult.data.refreshToken });
    result('API登录获取Token', true, tokenResult.data.accessToken.substring(0, 20) + '...');
  } else {
    result('API登录获取Token', false, JSON.stringify(tokenResult));
  }

  // ========================
  // 2. Dashboard
  // ========================
  console.log('\n--- 2. 仪表盘 ---');
  // Navigate to app root — the layout will redirect to dashboard
  await page.goto(`${BASE}/#/`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(2000);

  const dashOk = page.url().includes('dashboard');
  result('首页加载', dashOk, page.url());

  const sidebarVisible = await page.locator('.el-menu, .el-aside, aside').first().isVisible().catch(() => false);
  result('侧边栏可见', sidebarVisible);

  const mainText = await page.locator('.el-main, .page-container, .layout-main, main').first().textContent().catch(() => '');
  result('仪表盘有内容', mainText.trim().length > 10, `${mainText.trim().length} 字符`);

  // ========================
  // 3. User Management
  // ========================
  console.log('\n--- 3. 用户管理 (/system/user) ---');
  await page.goto(`${BASE}/#/system/user`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);

  const userTable = await page.locator('table, .el-table').first().isVisible().catch(() => false);
  result('表格渲染', userTable);

  const userRows = await page.locator('table tbody tr, .el-table__body tr').count();
  result('数据行数', userRows > 0, `${userRows} 行`);

  const userText = await page.locator('table tbody').first().textContent().catch(() => '');
  result('超级管理员', userText.includes('超级管理员'));
  result('技术主管', userText.includes('技术主管'));
  result('后端开发', userText.includes('后端开发'));

  const addUserBtns = await page.locator('button').allTextContents();
  result('新增用户按钮', addUserBtns.some(t => t.includes('新增用户')));

  // ========================
  // 4. Role Management
  // ========================
  console.log('\n--- 4. 角色管理 (/system/role) ---');
  await page.goto(`${BASE}/#/system/role`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);

  const roleTable = await page.locator('table, .el-table').first().isVisible().catch(() => false);
  result('表格渲染', roleTable);

  const roleRows = await page.locator('table tbody tr, .el-table__body tr').count();
  result('数据行数', roleRows > 0, `${roleRows} 行`);

  const roleText = await page.locator('table tbody').first().textContent().catch(() => '');
  result('超级管理员', roleText.includes('超级管理员'));
  result('技术负责人', roleText.includes('技术负责人'));
  result('普通用户', roleText.includes('普通用户'));

  // ========================
  // 5. Menu Management
  // ========================
  console.log('\n--- 5. 菜单管理 (/system/menu) ---');
  await page.goto(`${BASE}/#/system/menu`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);

  const menuTable = await page.locator('table, .el-table').first().isVisible().catch(() => false);
  result('表格渲染', menuTable);

  const menuRows = await page.locator('table tbody tr, .el-table__body tr').count();
  result('数据行数', menuRows > 0, `${menuRows} 行`);

  const menuText = await page.locator('table tbody').first().textContent().catch(() => '');
  result('系统管理目录', menuText.includes('系统管理'));
  result('用户管理菜单', menuText.includes('用户管理'));

  // ========================
  // 6. Dept Management
  // ========================
  console.log('\n--- 6. 部门管理 (/system/dept) ---');
  await page.goto(`${BASE}/#/system/dept`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);

  const deptTable = await page.locator('table, .el-table').first().isVisible().catch(() => false);
  result('表格渲染', deptTable);

  const deptRows = await page.locator('table tbody tr, .el-table__body tr').count();
  result('数据行数', deptRows > 0, `${deptRows} 行`);

  const deptText = await page.locator('table tbody').first().textContent().catch(() => '');
  result('总公司', deptText.includes('总公司'));
  result('技术部', deptText.includes('技术部'));
  result('产品部', deptText.includes('产品部'));

  // ========================
  // 7. Operation Log
  // ========================
  console.log('\n--- 7. 操作日志 (/log/operation) ---');
  await page.goto(`${BASE}/#/log/operation`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);

  const opLogTable = await page.locator('table, .el-table').first().isVisible().catch(() => false);
  result('表格渲染', opLogTable);

  const opLogText = await page.locator('table').first().textContent().catch(() => '');
  result('表格有内容', opLogText.trim().length > 0, `${opLogText.trim().length} 字符`);

  // ========================
  // 8. Login Log
  // ========================
  console.log('\n--- 8. 登录日志 (/log/login) ---');
  await page.goto(`${BASE}/#/log/login`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);

  const loginLogTable = await page.locator('table, .el-table').first().isVisible().catch(() => false);
  result('表格渲染', loginLogTable);

  const loginLogRows = await page.locator('table tbody tr, .el-table__body tr').count();
  result('有登录记录', loginLogRows > 0, `${loginLogRows} 行`);

  // ========================
  // 9. Sidebar Navigation
  // ========================
  console.log('\n--- 9. 侧边栏导航 ---');
  const menuItems = await page.locator('.el-menu-item, .el-sub-menu__title').allTextContents();
  const menuLabels = menuItems.map(t => t.replace(/\s/g, '').trim()).filter(Boolean);
  result('首页', menuLabels.some(t => t.includes('首页')));
  result('系统管理', menuLabels.some(t => t.includes('系统管理')));

  // ========================
  // 10. JS Console Errors
  // ========================
  console.log('\n--- 10. JS控制台错误 ---');
  if (consoleErrors.length > 0) {
    const unique = [...new Set(consoleErrors)];
    for (const err of unique.slice(0, 8)) {
      result('JS错误', false, err.substring(0, 150));
    }
  } else {
    result('无JS运行时错误', true, '控制台干净');
  }

  // ========================
  // 11. Blank Page Check
  // ========================
  console.log('\n--- 11. 空页面检查 ---');
  const pages = [
    { name: '用户管理', url: '/system/user' },
    { name: '角色管理', url: '/system/role' },
    { name: '菜单管理', url: '/system/menu' },
    { name: '部门管理', url: '/system/dept' },
    { name: '操作日志', url: '/log/operation' },
    { name: '登录日志', url: '/log/login' },
  ];

  for (const p of pages) {
    await page.goto(`${BASE}/#${p.url}`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(800);
    const bodyText = await page.locator('.el-main, .page-container, .layout-main').first().textContent().catch(() => '');
    const isEmpty = bodyText.trim().length < 5;
    result(p.name, !isEmpty, isEmpty ? '页面为空!' : `内容 ${bodyText.length} 字符`);
  }

  // ========================
  // Summary
  // ========================
  console.log('\n========== 测试结果汇总 ==========');
  const total = PASS + FAIL;
  const pct = Math.round(PASS / total * 100);
  console.log(`通过: ${PASS}  失败: ${FAIL}  总计: ${total}  通过率: ${pct}%`);

  if (FAIL > 0) {
    console.log('\n--- 失败项 ---');
    for (const line of REPORT) {
      if (line.includes('✗ FAIL')) console.log(line);
    }
  }

  await browser.close();
}

main().catch(err => {
  console.error('测试异常:', err.message);
  process.exit(1);
});
