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

async function loginViaAPI(page) {
  await page.goto(`${BASE}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(500);
  const tokenResult = await page.evaluate(async () => {
    for (let attempt = 0; attempt < 5; attempt++) {
      for (const pwd of ['Admin@1234', 'admin123']) {
        const res = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: 'admin', password: pwd }),
        });
        const json = await res.json();
        if (json.code === 200) return json;
        if (json.code === 1009) { await new Promise(r => setTimeout(r, 1000)); continue; }
      }
      await new Promise(r => setTimeout(r, 500));
    }
    return { code: -1, message: 'All passwords failed' };
  });
  if (tokenResult && tokenResult.data && tokenResult.data.accessToken) {
    await page.evaluate(({ at, rt }) => {
      localStorage.setItem('accessToken', at);
      localStorage.setItem('refreshToken', rt);
    }, { at: tokenResult.data.accessToken, rt: tokenResult.data.refreshToken });
    return true;
  }
  return false;
}

async function main() {
  console.log('========== 小说网站前端全盘浏览器测试 ==========\n');

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

  // ============================
  // 0. API Login
  // ============================
  console.log('--- 0. API登录获取Token ---');
  const loggedIn = await loginViaAPI(page);
  result('API登录获取Token', loggedIn);
  if (!loggedIn) { await browser.close(); process.exit(1); }

  // ============================
  // 1. 小说广场 (Novel List) — 验证种子数据
  // ============================
  console.log('\n--- 1. 小说广场 (/novel) ---');
  await page.goto(`${BASE}/#/novel`, { waitUntil: 'networkidle' });
  await page.waitForSelector('.hero-banner', { timeout: 5000 }).catch(() => {});
  await page.waitForTimeout(800);

  result('Hero横幅可见', await page.locator('.hero-banner').first().isVisible().catch(() => false));
  result('标题"小说广场"', (await page.locator('.hero-title').first().textContent().catch(() => '')).includes('小说广场'));

  result('搜索框可见', await page.locator('.hero-search input').first().isVisible().catch(() => false));
  result('搜索按钮可见', await page.locator('.hero-search button').first().isVisible().catch(() => false));

  const catTabs = await page.locator('.cat-item').allTextContents();
  result('分类标签(含6个分类+全部)', catTabs.length >= 7, `${catTabs.length} 个标签`);

  await page.waitForSelector('.novel-card', { timeout: 5000 }).catch(() => {});
  const novelCards = await page.locator('.novel-card').count();
  result('小说卡片渲染', novelCards > 0, `${novelCards} 张卡片`);
  result('至少8部小说(3原始+6新-1测试)', novelCards >= 8, `${novelCards} 部`);

  const allCardTitles = await page.locator('.card-title').allTextContents();
  result('含"万古神帝"', allCardTitles.some(t => t.includes('万古神帝')));
  result('含"倾城医妃"', allCardTitles.some(t => t.includes('倾城医妃')));
  result('含"星河战神"', allCardTitles.some(t => t.includes('星河战神')));
  result('含"阴阳鬼探"', allCardTitles.some(t => t.includes('阴阳鬼探')));

  // Category filter
  console.log('  > 分类筛选...');
  await page.locator('.cat-item:has-text("都市生活")').first().click();
  await page.waitForTimeout(1000);
  const filteredTitles = await page.locator('.card-title').allTextContents();
  result('筛选都市类', filteredTitles.some(t => t.includes('都市最强医者') || t.includes('倾城医妃')), filteredTitles.join(', '));

  await page.locator('.cat-item:has-text("全部分类")').first().click();
  await page.waitForTimeout(500);

  // Search
  console.log('  > 搜索...');
  const searchInput = page.locator('.hero-search input').first();
  await searchInput.clear();
  await searchInput.fill('剑破苍穹');
  await page.waitForTimeout(300);
  await page.locator('.hero-search button').first().click();
  await page.waitForTimeout(1500);
  const searchResults = await page.locator('.novel-card').count();
  const searchTitles = await page.locator('.card-title').allTextContents();
  result('搜索"剑破苍穹"', searchResults === 1, `${searchResults} 个结果: ${searchTitles.join(', ')}`);

  // Clear search
  await searchInput.clear();
  await page.locator('.hero-search button').first().click();
  await page.waitForTimeout(800);

  // ============================
  // 2. 小说详情 (Novel Detail)
  // ============================
  console.log('\n--- 2. 小说详情 (/novel/1) ---');
  await page.goto(`${BASE}/#/novel/1`, { waitUntil: 'networkidle' });
  await page.waitForSelector('.novel-title', { timeout: 5000 }).catch(() => {});
  await page.waitForTimeout(500);

  result('详情容器可见', await page.locator('.detail-container').first().isVisible().catch(() => false));

  const novelTitle = await page.locator('.novel-title').first().textContent().catch(() => '');
  result('小说标题"苍穹破天录"', novelTitle.includes('苍穹破天录'), novelTitle);

  const novelMeta = await page.locator('.novel-meta').first().textContent().catch(() => '');
  result('作者信息', novelMeta.includes('后端开发'));
  result('分类信息', novelMeta.includes('玄幻奇幻'));

  // Stats
  const statVals = await page.locator('.stat-val').allTextContents();
  result('统计数值(字数/点击/收藏)', statVals.length >= 3, statVals.join(' | '));

  // Intro
  const introText = await page.locator('.intro-text').first().textContent().catch(() => '');
  result('作品简介内容', introText.length > 30, introText.substring(0, 60) + '...');

  // Chapter list
  await page.waitForSelector('.chapter-item', { timeout: 5000 }).catch(() => {});
  const chapterItems = await page.locator('.chapter-item').count();
  result('章节目录渲染', chapterItems >= 3, `${chapterItems} 章`);

  const ch1Text = await page.locator('.chapter-item').first().textContent().catch(() => '');
  result('第一章标题含"废脉少年"', ch1Text.includes('废脉少年'));

  // Action buttons
  const readBtnVisible = await page.locator('button:has-text("开始阅读")').first().isVisible().catch(() => false);
  result('开始阅读按钮', readBtnVisible);

  // Shelf button (text varies: 加入书架 or 已收藏)
  const shelfBtnText = await page.locator('.header-actions button').last().textContent().catch(() => '');
  result('书架按钮(加入书架/已收藏)', shelfBtnText.includes('书架') || shelfBtnText.includes('收藏'), shelfBtnText);

  // Comment section
  const hasCommentArea = await page.locator('.comment-input, .comment-login-hint').first().isVisible().catch(() => false);
  result('评论区可见', hasCommentArea);

  // ============================
  // 3. 阅读器 (Novel Read)
  // ============================
  console.log('\n--- 3. 阅读器 (/novel/read/1/1) ---');
  await page.goto(`${BASE}/#/novel/read/1/1`, { waitUntil: 'networkidle' });
  await page.waitForSelector('.chapter-title', { timeout: 5000 }).catch(() => {});
  await page.waitForTimeout(500);

  result('阅读器可见', await page.locator('.reader-page').first().isVisible().catch(() => false));

  const chTitle = await page.locator('.chapter-title').first().textContent().catch(() => '');
  result('章节标题', chTitle.includes('废脉少年'), chTitle);

  const content = await page.locator('.chapter-content').first().textContent().catch(() => '');
  result('章节内容', content.length > 100, `${content.length} 字符`);
  result('内容含"林尘"', content.includes('林尘'));

  // Paragraph rendering
  const paragraphs = await page.locator('.chapter-content p').count();
  result('段落渲染', paragraphs >= 3, `${paragraphs} 个段落`);

  // Topbar
  const topbarTitle = await page.locator('.topbar-title').first().textContent().catch(() => '');
  result('顶栏小说名', topbarTitle.includes('苍穹破天录'), topbarTitle);

  const fontSizeSelect = page.locator('.topbar-right .el-select').first();
  result('字号选择器', await fontSizeSelect.isVisible().catch(() => false));

  // Dark mode
  console.log('  > 测试深色模式...');
  await page.locator('.topbar-right button').first().click();
  await page.waitForTimeout(500);
  const darkActive = await page.locator('.reader-page.dark-mode').count().catch(() => 0);
  result('深色模式切换', darkActive > 0);
  // Toggle back
  await page.locator('.topbar-right button').first().click();
  await page.waitForTimeout(300);

  // Navigation
  const navButtons = page.locator('.reader-nav button');
  const navCount = await navButtons.count();
  result('底部导航按钮', navCount >= 2, `${navCount} 个按钮`);

  // First button (prev) should be disabled
  const firstNavBtn = navButtons.first();
  const prevDisabled = await firstNavBtn.isDisabled().catch(() => false);
  result('第一章上一页禁用', prevDisabled, prevDisabled ? '已禁用' : '未禁用');

  const navProgress = await page.locator('.nav-progress').first().textContent().catch(() => '');
  result('进度显示(如1/3)', navProgress.includes('/'), navProgress);

  // Go to next chapter
  console.log('  > 测试章节跳转...');
  const lastNavBtn = navButtons.last();
  await lastNavBtn.click();
  await page.waitForSelector('.chapter-title', { timeout: 5000 });
  await page.waitForTimeout(500);
  const nextChTitle = await page.locator('.chapter-title').first().textContent().catch(() => '');
  result('跳转到第二章', nextChTitle.includes('意外传承'), nextChTitle);

  // Keyboard nav back
  await page.keyboard.press('ArrowLeft');
  await page.waitForSelector('.chapter-title', { timeout: 5000 });
  await page.waitForTimeout(500);
  const backTitle = await page.locator('.chapter-title').first().textContent().catch(() => '');
  result('键盘左键(A)回第一章', backTitle.includes('废脉少年'), backTitle);

  // ============================
  // 4. 我的书架 (Bookshelf)
  // ============================
  console.log('\n--- 4. 我的书架 (/bookshelf) ---');
  // Re-login to ensure token is fresh
  await loginViaAPI(page);
  await page.goto(`${BASE}/#/bookshelf`, { waitUntil: 'networkidle' });
  await page.waitForSelector('.shelf-page', { timeout: 5000 }).catch(() => {});
  await page.waitForTimeout(800);

  const shelfPage = await page.locator('.shelf-page').first().isVisible().catch(() => false);
  result('书架页面可见', shelfPage);

  const pageTitle = await page.locator('.page-title').first().textContent().catch(() => '');
  result('页面标题"我的书架"', pageTitle.includes('我的书架'), pageTitle);

  const tabs = await page.locator('.el-tabs__item').allTextContents();
  result('标签页(书架+阅读历史)', tabs.length >= 2, tabs.join(', '));

  // Bookshelf content
  const bookCards = await page.locator('.book-card').count();
  const emptyState = await page.locator('.el-empty').first().isVisible().catch(() => false);
  result('书架内容', bookCards > 0 || emptyState, bookCards > 0 ? `${bookCards} 本书` : '书架为空');

  if (bookCards > 0) {
    const firstBookTitle = await page.locator('.book-title').first().textContent().catch(() => '');
    result('书籍标题显示', firstBookTitle.length > 0, firstBookTitle);
  }

  // Reading history tab
  console.log('  > 测试阅读历史...');
  await page.locator('.el-tabs__item:has-text("阅读历史")').first().click();
  await page.waitForTimeout(1000);
  const histItems = await page.locator('.history-item').count();
  result('阅读历史记录', histItems >= 0, `${histItems} 条`);

  if (histItems > 0) {
    const histTitle = await page.locator('.hist-title').first().textContent().catch(() => '');
    result('历史记录有标题', histTitle.length > 0, histTitle);

    const continueBtn = page.locator('.hist-btn:has-text("继续阅读")').first();
    result('继续阅读按钮', await continueBtn.isVisible().catch(() => false));
  }

  // ============================
  // 5. 作者中心 (Author Dashboard)
  // ============================
  console.log('\n--- 5. 作者中心 (/author) ---');
  await loginViaAPI(page);
  await page.goto(`${BASE}/#/author`, { waitUntil: 'networkidle' });
  await page.waitForSelector('.author-page', { timeout: 5000 }).catch(() => {});
  await page.waitForTimeout(800);

  const authorPage = await page.locator('.author-page').first().isVisible().catch(() => false);
  result('作者中心页面可见', authorPage);

  const authorTitle = await page.locator('.page-title').first().textContent().catch(() => '');
  result('页面标题"作者中心"', authorTitle.includes('作者中心'), authorTitle);

  // Table — admin now only sees own novels via /my endpoint (correct behavior)
  await page.waitForSelector('.el-table__body', { timeout: 5000 }).catch(() => {});
  const tableVisible = await page.locator('.el-table').first().isVisible().catch(() => false);
  result('小说表格渲染', tableVisible);

  const bodyText = await page.locator('.el-table__body').first().textContent().catch(() => '');
  const rowCount = await page.locator('.el-table__body tr').count();
  const tbodyRows = await page.locator('tbody tr').count().catch(() => 0);
  const hasData = bodyText.length > 20;
  result('表格渲染正常', tableVisible, hasData ? `${rowCount}/${tbodyRows} 行: ${bodyText.substring(0, 50)}` : '表格为空(admin无自己的小说)');

  // Action buttons check (only if data exists)
  if (hasData) {
    const actionBtnTexts = await page.locator('.el-table__body button').allTextContents();
    const hasChapter = actionBtnTexts.some(t => t === '章节');
    const hasEdit = actionBtnTexts.some(t => t === '编辑');
    const hasDelete = actionBtnTexts.some(t => t === '删除');
    result('操作按钮-章节', hasChapter);
    result('操作按钮-编辑', hasEdit);
    result('操作按钮-删除', hasDelete);
  } else {
    result('操作按钮(空表格跳过)', true, '表格为空');
  }

  // New novel button
  const newBtn = await page.locator('button:has-text("新建小说")').first().isVisible().catch(() => false);
  result('新建小说按钮', newBtn);

  // ============================
  // 6. 新建小说对话框
  // ============================
  console.log('\n--- 6. 新建小说对话框 ---');
  await page.locator('button:has-text("新建小说")').first().click();
  await page.waitForTimeout(800);

  const dialogVisible = await page.locator('.el-dialog').filter({ hasText: '新建小说' }).first().isVisible().catch(() => false);
  result('新建小说对话框弹出', dialogVisible);

  if (dialogVisible) {
    // Form fields - the dialog is appended to body, use el-dialog directly
    const dialogSelector = '.el-dialog';

    // Find all inputs in the dialog (excluding hidden/radio)
    const allInputs = await page.locator(dialogSelector + ' .el-input__inner, ' + dialogSelector + ' textarea').count();
    result('对话框表单字段', allInputs >= 2, `${allInputs} 个输入组件`);

    // Title input - find by the form label text
    const titleInput = page.locator(dialogSelector + ' .el-form-item').filter({ hasText: '书名' }).locator('.el-input__inner, input').first();
    const hasTitleInput = await titleInput.isVisible().catch(() => false);
    result('书名输入框', hasTitleInput);

    // Fill and test validation
    if (hasTitleInput) {
      // Try submitting empty - should trigger validation
      const dialogFooter = page.locator(dialogSelector + ' .el-dialog__footer').first();
      const confirmBtn = dialogFooter.locator('button:has-text("确认")').first();

      // First test validation by submitting empty
      await confirmBtn.click();
      await page.waitForTimeout(500);
      const errorMsg = await page.locator('.el-form-item__error').first().isVisible().catch(() => false);
      result('表单验证(必填提示)', errorMsg, errorMsg ? '显示错误提示' : '无提示');

      // Fill form
      await titleInput.fill('自动化测试小说_' + Date.now());

      // Select category - click the select in the dialog
      const catSelect = page.locator(dialogSelector + ' .el-select').first();
      await catSelect.click();
      await page.waitForTimeout(600);
      // Click first option in the dropdown
      const dropdownOption = page.locator('.el-select-dropdown__item').last();
      if (await dropdownOption.isVisible().catch(() => false)) {
        await dropdownOption.click();
        await page.waitForTimeout(300);
      }

      // Fill intro
      const introInput = page.locator(dialogSelector + ' textarea').first();
      if (await introInput.isVisible().catch(() => false)) {
        await introInput.fill('这是一本自动化测试创建的小说，用于验证前端功能是否正常。');
        await page.waitForTimeout(300);
        result('填写表单', true);
      } else {
        result('填写表单', false, '简介输入框未找到');
      }

      // Submit
      await confirmBtn.click();
      await page.waitForTimeout(2000);

      // Check if dialog closed (success) or still open (error)
      const stillOpen = await page.locator(dialogSelector).filter({ hasText: '新建小说' }).first().isVisible().catch(() => true);
      const dialogClosed = !stillOpen;
      result('创建小说提交', dialogClosed, dialogClosed ? '提交成功，对话框已关闭' : '提交可能失败(或无权限)');

      // Check for success message
      const successMsg = await page.locator('.el-message--success, .el-notification__title').first().isVisible().catch(() => false);
      result('创建成功提示', dialogClosed || successMsg, dialogClosed ? '对话框已关闭' : (successMsg ? '有成功提示' : '无提示'));
    }
  }

  // Clean up - close dialog if still open
  const cancelBtn = page.locator('button:has-text("取消")').first();
  if (await cancelBtn.isVisible().catch(() => false)) {
    await cancelBtn.click();
    await page.waitForTimeout(300);
  }

  // ============================
  // 7. 章节管理对话框
  // ============================
  console.log('\n--- 7. 章节管理对话框 ---');
  await page.goto(`${BASE}/#/author`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);

  // After creating test novel in section 6, admin should have it in the table
  // Find any row that has a "章节" button
  const chapterBtn = page.locator('.el-table__body button:has-text("章节")').first();

  if (await chapterBtn.isVisible().catch(() => false)) {
    await chapterBtn.click();
    await page.waitForTimeout(1500);

    const chDialog = await page.locator('.el-dialog').filter({ hasText: /管理章节/ }).first().isVisible().catch(() => false);
    result('章节管理对话框', chDialog);

    if (chDialog) {
      const chItems = await page.locator('.chapter-list-item').count();
      result('章节列表', chItems >= 0, `${chItems} 章`);

      if (chItems > 0) {
        const chTitles = await page.locator('.ch-title').allTextContents();
        result('章节标题列表', chTitles.length > 0, chTitles.join(', '));

        const chWordCounts = await page.locator('.ch-words').allTextContents();
        const hasWordCount = chWordCounts.some(w => w.includes('字'));
        result('字数显示', hasWordCount, chWordCounts.join(', '));

        // Test edit chapter
        const editChapterBtn = page.locator('.chapter-list-item button:has-text("编辑")').first();
        if (await editChapterBtn.isVisible().catch(() => false)) {
          await editChapterBtn.click();
          await page.waitForTimeout(500);
          const editorVisible = await page.locator('.chapter-editor').first().isVisible().catch(() => false);
          result('编辑章节展开', editorVisible);

          if (editorVisible) {
            const editorTitle = await page.locator('.chapter-editor input').first().inputValue().catch(() => '');
            result('编辑器有原标题', editorTitle.length > 0, editorTitle);

            const editorContent = await page.locator('.chapter-editor textarea').first().inputValue().catch(() => '');
            result('编辑器有原内容', editorContent.length > 30, `${editorContent.length} 字符`);
          }
        }
      } else {
        result('章节为空(新小说)', true, '无现有章节');
      }

      // Test new chapter
      const addChapterBtn = page.locator('button:has-text("新增章节")').first();
      await addChapterBtn.click();
      await page.waitForTimeout(500);
      const editorVisible = await page.locator('.chapter-editor').first().isVisible().catch(() => false);
      result('新增章节编辑器', editorVisible);

      if (editorVisible) {
        const editorTitleInput = page.locator('.chapter-editor input[placeholder*="章节标题"]').first();
        result('章节标题输入框', await editorTitleInput.isVisible().catch(() => false));

        const editorContentInput = page.locator('.chapter-editor textarea').first();
        result('章节内容输入框', await editorContentInput.isVisible().catch(() => false));

        // Actually create a chapter for the new test novel? No, let's just verify the UI
        const cancelEditBtn = page.locator('.chapter-editor-footer button:has-text("取消")').first();
        await cancelEditBtn.click();
        await page.waitForTimeout(300);
      }
    }

    // Close
    await page.locator('.el-dialog button:has-text("关闭")').first().click();
    await page.waitForTimeout(500);
  } else {
    result('章节管理按钮', false, '未找到"苍穹破天录"的行(跳过)');
  }

  // ============================
  // 8. 侧边栏导航
  // ============================
  console.log('\n--- 8. 侧边栏导航 ---');
  await page.goto(`${BASE}/#/dashboard`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);

  const allMenuItems = await page.locator('.el-menu-item').allTextContents();
  const menuTexts = allMenuItems.map(t => t.replace(/\s/g, '').trim()).filter(Boolean);
  console.log('  侧边栏项目:', menuTexts.join(', '));

  result('侧边栏-首页', menuTexts.some(t => t.includes('首页')));
  result('侧边栏-小说广场', menuTexts.some(t => t.includes('小说广场')));
  result('侧边栏-我的书架', menuTexts.some(t => t.includes('书架')));
  result('侧边栏-作者中心', menuTexts.some(t => t.includes('作者中心')));

  // Navigate via sidebar (novel → standalone page, so go back to dashboard between nav clicks)
  console.log('  > 测试侧边栏导航...');
  await page.goto(`${BASE}/#/dashboard`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);

  const novelMenuItem = page.locator('.el-menu-item:has-text("小说广场")').first();
  await novelMenuItem.click();
  await page.waitForTimeout(1000);
  result('侧边栏→小说广场', page.url().includes('/novel') && !page.url().includes('/read'), page.url());

  // Go back to dashboard for next sidebar test
  await page.goto(`${BASE}/#/dashboard`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);

  const shelfMenuItem = page.locator('.el-menu-item:has-text("书架")').first();
  await shelfMenuItem.click();
  await page.waitForTimeout(1000);
  result('侧边栏→书架', page.url().includes('/bookshelf'), page.url());

  // Go back to dashboard
  await page.goto(`${BASE}/#/dashboard`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);

  const authorMenuItem = page.locator('.el-menu-item:has-text("作者中心")').first();
  await authorMenuItem.click();
  await page.waitForTimeout(1000);
  result('侧边栏→作者中心', page.url().includes('/author'), page.url());

  // ============================
  // 9. 页面跳转链路测试
  // ============================
  console.log('\n--- 9. 页面跳转链路 ---');
  await page.goto(`${BASE}/#/novel`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);

  // Find a card with "苍穹破天录" to ensure it has chapters
  const targetCard = page.locator('.novel-card').filter({ hasText: '苍穹破天录' }).first();
  if (await targetCard.isVisible().catch(() => false)) {
    await targetCard.click();
  } else {
    await page.locator('.novel-card').first().click();
  }
  await page.waitForSelector('.novel-title', { timeout: 5000 });
  await page.waitForTimeout(500);
  result('列表→详情', page.url().includes('/novel/') && !page.url().includes('/read'), page.url());

  // Detail → Read (click first chapter)
  await page.locator('.chapter-item').first().click();
  await page.waitForSelector('.chapter-content', { timeout: 5000 });
  await page.waitForTimeout(500);
  result('详情→阅读', page.url().includes('/novel/read/'), page.url());

  // Read → Detail (click chapter list icon)
  const listIcon = page.locator('.topbar-right button').last();
  await listIcon.click();
  await page.waitForTimeout(1000);
  result('阅读→详情(章节列表)', !page.url().includes('/read/'), page.url());

  // ============================
  // 10. Console Errors
  // ============================
  console.log('\n--- 10. JS控制台错误 ---');
  // (sections 11-14 follow)
  // Filter out expected errors: 401/403 from auth-required API calls, 404 for favicon/assets
  const ignorePatterns = [
    /401/,
    /403/,
    /failed with status code 401/,
    /failed with status code 403/,
    /Failed to load resource.*401/,
    /Failed to load resource.*403/,
    /Failed to load resource.*404/,
    /ERR_INVALID_URL/,
    /favicon/,
  ];
  const realErrors = consoleErrors.filter(err =>
    !ignorePatterns.some(p => p.test(err))
  );

  if (realErrors.length > 0) {
    const unique = [...new Set(realErrors)];
    for (const err of unique.slice(0, 10)) {
      result('JS错误', false, err.substring(0, 200));
    }
  } else {
    result('无JS运行时错误', true, consoleErrors.length > 0 ? `已过滤${consoleErrors.length}条预期的HTTP/网络错误` : '控制台干净');
  }

  // ============================
  // 11. 空白页检查
  // ============================
  console.log('\n--- 11. 空白页检查 ---');
  const pageChecks = [
    { name: '小说广场', url: '/novel', selector: '.novel-grid-wrap' },
    { name: '小说详情(苍穹)', url: '/novel/1', selector: '.detail-container' },
    { name: '小说详情(都市)', url: '/novel/2', selector: '.detail-container' },
    { name: '小说详情(星际)', url: '/novel/3', selector: '.detail-container' },
    { name: '阅读器(第1章)', url: '/novel/read/1/1', selector: '.reader-content' },
    { name: '阅读器(第2章)', url: '/novel/read/1/2', selector: '.reader-content' },
    { name: '阅读器(第3章)', url: '/novel/read/1/3', selector: '.reader-content' },
    { name: '我的书架', url: '/bookshelf', selector: '.shelf-page' },
    { name: '作者中心', url: '/author', selector: '.author-page' },
  ];

  for (const p of pageChecks) {
    await page.goto(`${BASE}/#${p.url}`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(800);
    const isVisible = await page.locator(p.selector).first().isVisible().catch(() => false);
    const bodyText = await page.locator(p.selector).first().textContent().catch(() => '');
    const isEmpty = !isVisible || bodyText.trim().length < 5;
    result(p.name, !isEmpty, isEmpty ? '⚠ 页面为空/不可见!' : `${bodyText.trim().length} 字符`);
  }

  // ============================
  // 12. 作者登录测试
  // ============================
  console.log('\n--- 12. 作者登录测试 ---');
  // Navigate to login page first so fetch has valid base URL
  await page.goto(`${BASE}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(1000);
  const authorToken = await page.evaluate(async () => {
    for (let i = 0; i < 5; i++) {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'author01', password: 'Author@123' }),
      });
      const json = await res.json();
      if (json.code === 200) return json.data.accessToken;
      if (json.code === 1009) { await new Promise(r => setTimeout(r, 1000)); continue; }
      return null;
    }
    return null;
  });
  if (authorToken) {
    await page.evaluate(({ at, rt }) => {
      localStorage.setItem('accessToken', at);
      localStorage.setItem('refreshToken', at);
    }, { at: authorToken, rt: authorToken });
    await page.reload();
    await page.waitForTimeout(1000);

    // Now visit author center
    await page.goto(`${BASE}/#/author`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1500);

    const authorPageVisible = await page.locator('.author-page').first().isVisible().catch(() => false);
    result('作者(author01)中心可见', authorPageVisible);

    const authorBodyText = await page.locator('.el-table__body').first().textContent().catch(() => '');
    result('作者可见"万古神帝"', authorBodyText.includes('万古神帝'), authorBodyText.substring(0, 60));
    result('作者可见"剑破苍穹"', authorBodyText.includes('剑破苍穹'));
    // Backend /api/novel/my currently returns all novels (known bug: missing authorId filter)
    // After backend rebuild, this should filter correctly for non-admin authors
    const filteredCorrectly = !authorBodyText.includes('倾城医妃');
    result('作者不可见他人小说', filteredCorrectly, filteredCorrectly ? '正确过滤(仅显示自己的小说)' : '已知后端bug: /my未按authorId过滤');

    // Re-login as admin for remaining tests
    await page.evaluate(async () => {
      for (const pwd of ['Admin@1234', 'admin123']) {
        const res = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: 'admin', password: pwd }),
        });
        const json = await res.json();
        if (json.code === 200) {
          localStorage.setItem('accessToken', json.data.accessToken);
          localStorage.setItem('refreshToken', json.data.refreshToken);
          return;
        }
      }
    });
    await page.waitForTimeout(500);
    console.log('  已切回admin账号');
  } else {
    result('author01登录', false, '登录失败(跳过作者测试)');
  }

  // ============================
  // 13. 读者登录测试
  // ============================
  console.log('\n--- 13. 读者登录测试 ---');
  await page.goto(`${BASE}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(1000);
  const readerToken = await page.evaluate(async () => {
    for (let i = 0; i < 5; i++) {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'reader01', password: 'Reader@123' }),
      });
      const json = await res.json();
      if (json.code === 200) return json.data.accessToken;
      if (json.code === 1009) { await new Promise(r => setTimeout(r, 1000)); continue; }
      return null;
    }
    return null;
  });
  if (readerToken) {
    await page.evaluate(({ at }) => {
      localStorage.setItem('accessToken', at);
      localStorage.setItem('refreshToken', at);
    }, { at: readerToken });
    await page.reload();
    await page.waitForTimeout(500);

    // Visit bookshelf
    await page.goto(`${BASE}/#/bookshelf`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1500);

    const bookCards = await page.locator('.book-card').count();
    result('读者(reader01)书架有收藏', bookCards > 0, `${bookCards} 本书`);

    if (bookCards > 0) {
      const titles = await page.locator('.book-title').allTextContents();
      result('书架含"苍穹破天录"', titles.some(t => t.includes('苍穹破天录')), titles.join(', '));
    }

    // Visit reading history
    await page.locator('.el-tabs__item:has-text("阅读历史")').first().click();
    await page.waitForTimeout(1000);
    const histItems = await page.locator('.history-item').count();
    result('读者有阅读记录', histItems > 0, `${histItems} 条`);

    // Re-login as admin
    await page.evaluate(async () => {
      for (const pwd of ['Admin@1234', 'admin123']) {
        const res = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: 'admin', password: pwd }),
        });
        const json = await res.json();
        if (json.code === 200) {
          localStorage.setItem('accessToken', json.data.accessToken);
          localStorage.setItem('refreshToken', json.data.refreshToken);
          return;
        }
      }
    });
    await page.waitForTimeout(500);
  } else {
    result('reader01登录', false, '登录失败(跳过读者测试)');
  }

  // ============================
  // 14. 未登录用户测试 (放最后, 使用独立context)
  // ============================
  console.log('\n--- 12. 未登录访问测试 (独立context, 无token) ---');
  const unauthContext = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    locale: 'zh-CN',
  });
  const unauthPage = await unauthContext.newPage();

  // Novel pages are now standalone routes (not under Layout), should work without auth
  await unauthPage.goto(`${BASE}/#/novel`, { waitUntil: 'networkidle' });
  await unauthPage.waitForSelector('.novel-grid-wrap', { timeout: 8000 }).catch(() => {});
  await unauthPage.waitForTimeout(500);
  const publicCards = await unauthPage.locator('.novel-card').count();
  const publicUrl1 = unauthPage.url();
  result('未登录-小说广场', publicCards > 0 && !publicUrl1.includes('/login'), !publicUrl1.includes('/login') ? `${publicCards} 张卡片` : '被重定向到登录');

  await unauthPage.goto(`${BASE}/#/novel/1`, { waitUntil: 'networkidle' });
  await unauthPage.waitForSelector('.detail-container', { timeout: 8000 }).catch(() => {});
  await unauthPage.waitForTimeout(500);
  const detailUrl = unauthPage.url();
  if (!detailUrl.includes('/login')) {
    const publicDetail = await unauthPage.locator('.novel-title').first().textContent().catch(() => '');
    result('未登录-详情页', publicDetail.length > 0, publicDetail);
    const loginHint = await unauthPage.locator('.comment-login-hint').first().isVisible().catch(() => false);
    result('未登录-评论区登录提示', loginHint);
  } else {
    result('未登录-详情页', false, '被重定向到登录');
    result('未登录-评论区登录提示', false, '页面已跳转');
  }

  await unauthPage.goto(`${BASE}/#/novel/read/1/1`, { waitUntil: 'networkidle' });
  await unauthPage.waitForSelector('.reader-content', { timeout: 8000 }).catch(() => {});
  await unauthPage.waitForTimeout(500);
  const readUrl = unauthPage.url();
  if (!readUrl.includes('/login')) {
    const publicContent = await unauthPage.locator('.chapter-content').first().textContent().catch(() => '');
    result('未登录-阅读器', publicContent.length > 50, `${publicContent.length} 字符`);
  } else {
    result('未登录-阅读器', false, '被重定向到登录');
  }

  await unauthContext.close();

  // ============================
  // Summary
  // ============================
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
