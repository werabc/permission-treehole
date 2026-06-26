const http = require('http');

const BASE = 'http://localhost:8080';
let ADMIN_TOKEN = '';

function api(method, path, data, token) {
  return new Promise((resolve, reject) => {
    const url = new URL(path, BASE);
    const body = data ? JSON.stringify(data) : undefined;
    const headers = {
      'Content-Type': 'application/json',
    };
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
  console.log('========== 网站数据填充脚本 ==========\n');

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

  const deptRes = await api('GET', '/api/user/dept/tree', null, ADMIN_TOKEN);
  const depts = deptRes.data || [];
  function findDept(name) {
    for (const d of depts) {
      if (d.deptName === name) return d.id;
      if (d.children) {
        for (const c of d.children) if (c.deptName === name) return c.id;
      }
    }
    return 4; // default 产品部
  }
  log(`部门数量: ${depts.length}`);

  // 3. Delete test novels created by automated tests
  console.log('\n--- 3. 清理测试小说 ---');
  const novelsRes = await api('GET', '/api/novel/published?pageNum=1&pageSize=50', null);
  const testNovelIds = novelsRes.data.records
    .filter(n => n.title && n.title.startsWith('自动化测试小说_'))
    .map(n => n.id);
  if (testNovelIds.length > 0) {
    await api('DELETE', `/api/novel/${testNovelIds.join(',')}`, null, ADMIN_TOKEN);
    log(`删除 ${testNovelIds.length} 个测试小说: ${testNovelIds.join(',')}`, true);
  } else {
    log('无测试小说需要清理');
  }

  // Also delete "测试小说" (id 4) if it has no chapters
  const testNovel4 = novelsRes.data.records.find(n => n.id === 4);
  if (testNovel4 && testNovel4.wordCount === 0) {
    await api('DELETE', '/api/novel/4', null, ADMIN_TOKEN);
    log('删除空洞测试小说(id=4)');
  }

  // 4. Register new users
  console.log('\n--- 4. 注册新用户 ---');
  const newUsers = [
    { username: 'reader01', nickname: '书虫小明', password: 'Reader@123', email: 'reader01@novel.com', phone: '13800001001', sex: 1, status: 1, deptId: findDept('产品部') },
    { username: 'reader02', nickname: '深夜读者', password: 'Reader@123', email: 'reader02@novel.com', phone: '13800001002', sex: 2, status: 1, deptId: findDept('产品部') },
    { username: 'reader03', nickname: '文学少女', password: 'Reader@123', email: 'reader03@novel.com', phone: '13800001003', sex: 2, status: 1, deptId: findDept('产品部') },
    { username: 'author01', nickname: '风凌天下', password: 'Author@123', email: 'author01@novel.com', phone: '13800002001', sex: 1, status: 1, deptId: findDept('技术部') },
    { username: 'author02', nickname: '紫月仙子', password: 'Author@123', email: 'author02@novel.com', phone: '13800002002', sex: 2, status: 1, deptId: findDept('技术部') },
    { username: 'author03', nickname: '江南烟雨', password: 'Author@123', email: 'author03@novel.com', phone: '13800002003', sex: 1, status: 1, deptId: findDept('技术部') },
  ];

  for (const user of newUsers) {
    if (existingUsers.includes(user.username)) {
      log(`用户 ${user.username} 已存在，跳过`);
      continue;
    }
    const createRes = await api('POST', '/api/user', user, ADMIN_TOKEN);
    if (createRes.code === 200) {
      log(`创建用户: ${user.username} (${user.nickname}) [${user.password}]`, true);
    } else {
      log(`创建用户 ${user.username} 失败: ${createRes.message}`, false);
    }
  }

  // 5. Get roles and assign to users
  console.log('\n--- 5. 分配角色 ---');
  const rolesRes = await api('GET', '/api/role/page?pageNum=1&pageSize=20', null, ADMIN_TOKEN);
  const roles = rolesRes.data?.records || [];
  log(`找到 ${roles.length} 个角色: ${roles.map(r => r.roleName + '(' + r.roleCode + ')').join(', ')}`);

  // Find role IDs
  const authorRole = roles.find(r => r.roleCode === 'author' || r.roleName.includes('作者'));
  const userRole = roles.find(r => r.roleCode === 'user' || r.roleName.includes('普通用户'));

  // Look up user IDs
  const usersPage2 = await api('GET', '/api/user/page?pageNum=1&pageSize=30', null, ADMIN_TOKEN);
  const allUsers = usersPage2.data?.records || [];
  function getUserId(username) {
    const u = allUsers.find(u => u.username === username);
    return u ? u.id : null;
  }

  // Assign reader role to readers
  if (userRole) {
    for (const username of ['reader01', 'reader02', 'reader03']) {
      const userId = getUserId(username);
      if (userId) {
        const assignRes = await api('PUT', `/api/user/${userId}/roles`, { roleIds: [userRole.id] }, ADMIN_TOKEN);
        log(`分配用户角色给 ${username}: ${assignRes.code === 200 ? '成功' : assignRes.message}`, assignRes.code === 200);
      }
    }
  }

  // Assign author role to authors (need a special role)
  if (authorRole) {
    for (const username of ['author01', 'author02', 'author03']) {
      const userId = getUserId(username);
      if (userId) {
        const assignRes = await api('PUT', `/api/user/${userId}/roles`, { roleIds: [authorRole.id] }, ADMIN_TOKEN);
        log(`分配作者角色给 ${username}: ${assignRes.code === 200 ? '成功' : assignRes.message}`, assignRes.code === 200);
      }
    }
  } else {
    log('未找到作者角色，尝试创建...');
    const createRoleRes = await api('POST', '/api/role', {
      roleName: '小说作者',
      roleCode: 'author',
      roleDesc: '可以创建和管理自己的小说',
      status: 1,
      dataScope: 4,
    }, ADMIN_TOKEN);
    log(`创建作者角色: ${createRoleRes.code === 200 ? '成功' : createRoleRes.message}`, createRoleRes.code === 200);
    if (createRoleRes.code === 200) {
      // Re-fetch roles
      const rolesRes2 = await api('GET', '/api/role/page?pageNum=1&pageSize=20', null, ADMIN_TOKEN);
      const authorRole2 = rolesRes2.data?.records?.find(r => r.roleCode === 'author');
      if (authorRole2) {
        for (const username of ['author01', 'author02', 'author03']) {
          const userId = getUserId(username);
          if (userId) {
            await api('PUT', `/api/user/${userId}/roles`, { roleIds: [authorRole2.id] }, ADMIN_TOKEN);
            log(`分配作者角色给 ${username}: 成功`, true);
          }
        }
      }
    }
  }

  // 6. Login as each new author and reader
  console.log('\n--- 6. 获取作者/读者登录信息 ---');
  const authorTokens = {};
  for (const username of ['author01', 'author02', 'author03']) {
    const res = await api('POST', '/api/auth/login', { username, password: 'Author@123' });
    if (res.code === 200) {
      authorTokens[username] = res.data.accessToken;
      log(`${username} 登录成功`, true);
    } else {
      log(`${username} 登录失败: ${res.message}`, false);
    }
  }

  const readerTokens = {};
  for (const username of ['reader01', 'reader02', 'reader03']) {
    const res = await api('POST', '/api/auth/login', { username, password: 'Reader@123' });
    if (res.code === 200) {
      readerTokens[username] = res.data.accessToken;
      log(`${username} 登录成功`, true);
    }
  }

  // 7. Create novels for authors
  console.log('\n--- 7. 作者创建小说 ---');
  const newNovels = [];

  // author01 novels - 玄幻
  const novelsToCreate = [
    { author: 'author01', title: '万古神帝', categoryId: 1, intro: '九天大陆，诸雄并起。少年张若尘，身怀时空秘典，从微末中崛起。他要在这恢弘壮阔的世界里，走出一条属于自己的神帝之路！', status: 1 },
    { author: 'author01', title: '剑破苍穹', categoryId: 2, intro: '一剑破万法，一剑斩苍穹。少年剑客陆凡，持三尺青锋，行走江湖。从默默无闻到名震天下，他用手中之剑，书写了一段不朽传奇。', status: 1 },
    { author: 'author02', title: '倾城医妃', categoryId: 3, intro: '现代医学博士穿越异世，成为被退婚的废柴王妃。凭借一手逆天医术和现代智慧，她步步为营，权倾天下。那个曾经对她不屑一顾的男人，如今却跪求复合。', status: 1 },
    { author: 'author02', title: '妖娆阵师', categoryId: 4, intro: '她，是穿越而来的阵法天才；他，是冷傲孤高的帝国战神。当命运的齿轮开始转动，一段跨越生死的爱恋在这片大陆上徐徐展开。', status: 1 },
    { author: 'author03', title: '星河战神', categoryId: 5, intro: '未来星际时代，机甲与异能并存。退役特种兵林浩，意外获得上古战神系统，从此踏上称霸星河的征途。敌人虽强，但我自一拳破之！', status: 1 },
    { author: 'author03', title: '阴阳鬼探', categoryId: 6, intro: '天生阴阳眼，能见鬼怪。他是一个私家侦探，专接警方不敢接的案子。午夜凶铃、血色婚礼、废楼鬼影……每一桩案件背后，都隐藏着不为人知的真相。', status: 1 },
  ];

  for (const novelData of novelsToCreate) {
    const token = authorTokens[novelData.author];
    if (!token) { log(`跳过"${novelData.title}" — ${novelData.author} 未登录`, false); continue; }
    const res = await api('POST', '/api/novel', {
      title: novelData.title,
      categoryId: novelData.categoryId,
      intro: novelData.intro,
      status: novelData.status,
    }, token);
    if (res.code === 200) {
      // API returns data: null, need to query the author's novel list to get ID
      log(`"${novelData.title}" by ${novelData.author} 创建成功`, true);
    } else {
      log(`"${novelData.title}" 创建失败: ${res.message}`, false);
    }
  }

  // Look up created novel IDs by fetching each author's novel list
  for (const author of ['author01', 'author02', 'author03']) {
    const token = authorTokens[author];
    if (!token) continue;
    const myNovels = await api('GET', '/api/novel/my?pageNum=1&pageSize=20', null, token);
    if (myNovels.data && myNovels.data.records) {
      for (const novel of myNovels.data.records) {
        if (!newNovels.find(n => n.title === novel.title && n.author === author)) {
          const match = novelsToCreate.find(n => n.title === novel.title && n.author === author);
          if (match) {
            newNovels.push({ ...match, id: novel.id });
            log(`  找到小说 "${novel.title}" id=${novel.id}`, true);
          }
        }
      }
    }
  }

  // 8. Add chapters
  console.log('\n--- 8. 添加章节内容 ---');

  const chapterData = {
    '万古神帝': [
      { title: '第一章 废柴少年', content: '九天大陆，青云国，云武城。\n\n清晨的阳光透过破旧的窗户照进小屋，张若尘从修炼中睁开双眼。\n\n"还是不行吗……"他苦笑着摇摇头。十六岁了，他还停留在炼气一层，在这个以武为尊的世界里，他被所有人看不起。\n\n张家是云武城三大家族之一，而他虽然是家主之子，却因天赋低劣被所有人轻视。\n\n"若尘哥哥！"门外传来清脆的声音。\n\n一个穿着淡绿色衣裙的少女推门而入，正是他的青梅竹马——柳梦璃。\n\n"梦璃，你怎么来了？"张若尘站起身。\n\n"今天是家族考核的日子呀，你可别又迟到了。"柳梦璃笑着说，眼中却闪过一丝担忧。\n\n张若尘点点头，跟着她走出小屋。他不知道的是，今天的考核，将彻底改变他的命运。' },
      { title: '第二章 时空秘典', content: '家族考核场上，人声鼎沸。\n\n"下一个，张若尘！"\n\n当这个名字被念出时，周围顿时响起一片嘲笑声。\n\n"那个废物也敢来？"\n\n"听说他在炼气一层卡了三年了。"\n\n张若尘充耳不闻，走上擂台。他的对手是二长老的儿子张明远，炼气四层的高手。\n\n"废物，一招解决你！"张明远狞笑着冲来。\n\n就在这时，张若尘脑海中突然响起一道苍老的声音："有缘人，你终于来了……"\n\n霎时间，无数金色文字涌入他的意识——时空秘典！\n\n时间仿佛在这一刻停止了。张若尘能清晰地看到张明远每一个动作的轨迹。\n\n他轻轻侧身，一拳击出。\n\n"砰！"\n\n张明远倒飞而出，重重摔在擂台下。\n\n全场寂静。\n\n张若尘看着自己的拳头，心中波澜起伏。上古神帝的传承，竟在自己体内！' },
      { title: '第三章 初露锋芒', content: '那一战后，张若尘的名字传遍了云武城。\n\n但他知道，这只是开始。时空秘典中记载的功法太过深奥，他需要时间修炼。\n\n夜深人静时，张若尘来到后山。\n\n"时空之力，封！"\n\n一个透明的结界将他笼罩，里面的时间流速是外面的十倍。这意味着他在里面修炼十天，外面只过去一天。\n\n日复一日，张若尘的实力飞速提升。\n\n炼气二层、三层、四层……仅仅一个月的时间，他就突破了炼气七层！\n\n这一天，家族突然收到一个消息——青云国皇室要举办青年武道大会，获得前三名的年轻俊杰可以进入皇家秘境修炼。\n\n"这是一个机会。"张若尘自语道。\n\n他知道，只有不断变强，才能在这个世界上掌控自己的命运。' },
    ],
    '剑破苍穹': [
      { title: '第一章 山村少年', content: '青山镇，一个偏僻的小山村。\n\n陆凡背着一捆柴回到家中，母亲正在灶台前忙碌。\n\n"凡儿，今天怎么这么晚？"\n\n"我去后山采了些药材。"陆凡笑着从怀里掏出几株草药，"这些卖了可以给爹买些补品。"\n\n陆凡的父亲曾是江湖中赫赫有名的剑客，可惜十年前一场恶战后经脉尽断，沦为废人。\n\n"爹，我今天在山上遇到一个奇怪的老者。"吃饭时，陆凡说道。\n\n父亲的手微微一顿："什么样的老者？"\n\n"他背着一把没有剑鞘的铁剑，看了我一眼就走了。但不知道为什么，他走后我发现地上多了一本书。"\n\n陆凡从怀中掏出一本泛黄的古籍，封面上写着三个字——《破天剑诀》。\n\n父亲瞳孔一缩："这是……失传三百年的破天剑诀？！"\n\n命运的齿轮，从这一刻开始转动。' },
      { title: '第二章 初学剑法', content: '《破天剑诀》共有九式，每一式都蕴含着天地至理。\n\n陆凡日夜苦练，仅仅三个月就掌握了前两式。\n\n这天，青山镇来了一群不速之客。一群黑衣人闯入陆凡家中，为首之人冷笑道："陆剑秋，二十年前你杀了我们掌门，今天该偿命了！"\n\n陆凡挡在父亲面前："不许伤害我爹！"\n\n"小崽子，滚开！"\n\n黑衣人一掌拍来，陆凡本能地拔出了父亲的旧剑。\n\n剑光一闪！\n\n那黑衣人惨叫一声，整条手臂被斩了下来。\n\n在场所有人都震惊了。一个十六岁的少年，竟然一剑击退了一个江湖老手！\n\n陆剑秋眼中精光闪烁："凡儿……你已经练成了第一式？"\n\n陆凡点点头，手中的剑还在微微颤抖。从这一刻起，他知道，自己将走上一条不同的路。' },
      { title: '第三章 踏入江湖', content: '陆凡决定离开青山镇去闯荡江湖。\n\n母亲含泪为他收拾行囊，父亲则将自己珍藏多年的剑谱和一枚玉佩交给他。\n\n"这玉佩是当年剑阁的信物，你若遇到危难，可以去剑阁求助。"\n\n"剑阁？"\n\n"天下剑客圣地，剑阁。"陆剑秋眼中露出追忆之色，"你爹我年轻时，曾是剑阁弟子。"\n\n陆凡震惊不已。原来自己的父亲曾经是天下第一剑道宗门的弟子！\n\n他背上行囊，踏上了前往剑阁的路。\n\n一路上，他遇到了形形色色的人——正义的侠客、阴险的魔道中人、神秘的老乞丐……\n\n每一个经历都在磨练他的剑心。\n\n三个月后，当他终于站在剑阁山下时，他已经不再是当初那个懵懂的少年了。' },
    ],
    '倾城医妃': [
      { title: '第一章 穿越异世', content: '苏瑾睁开眼的时候，看到的是古色古香的雕花床顶。\n\n"小姐，您终于醒了！"一个丫鬟打扮的少女哭得梨花带雨。\n\n记忆如潮水般涌来。她，苏瑾，现代医学博士，在实验室加班时猝死，穿越到了这个名为大炎王朝的异世。\n\n原主也叫苏瑾，是镇北将军府的嫡女。可惜母亲早逝，父亲常年在外征战，继母和继妹对她百般欺辱。\n\n更糟糕的是，她还被太子退了婚，沦为整个京城的笑柄。\n\n"有意思。"苏瑾嘴角勾起一抹微笑。\n\n在现代，她是站在医学顶端的天才。在这个世界，她有无数治病救人的知识和技能。\n\n"小姐，您……您没事吧？"丫鬟小翠担忧地说。\n\n"没事，好得很。"苏瑾坐起身来，"小翠，去给我准备一套银针。"\n\n"银针？"\n\n"对，我要开始行医了。"\n\n这副身体的经脉有些堵塞，正好用针灸调理。她的崛起，从这一刻开始。' },
      { title: '第二章 小小诊所', content: '京城最繁华的长安街上，一间不起眼的小诊所悄然开张。\n\n没有人知道，这间诊所的主人就是那位被退婚的镇北将军府大小姐。\n\n第一天，无人问津。\n\n第二天，依然冷清。\n\n第三天，一个老妇人被抬了进来。老人已经昏迷三天，看了无数大夫都说没救了。\n\n苏瑾诊脉后，从容不迫地取出银针。\n\n"你家老夫人只是中风，并非不治。"\n\n银针飞舞，一刻钟后，老人睁开了眼睛。\n\n"神……神医啊！"\n\n消息不胫而走。从此苏瑾的小诊所门庭若市，达官贵人排队求诊。\n\n然而树大招风。这天，一顶金轿停在了诊所门口。\n\n"本王身体不适，请苏大夫诊治。"\n\n来人正是三皇子——萧景琰。当朝最受宠的皇子，也是京城所有女子梦寐以求的对象。\n\n苏瑾抬眼看了他一眼："排队。"\n\n满堂皆惊。' },
      { title: '第三章 宫闱风云', content: '萧景琰愣住了。他活了二十四年，还从没有人敢让他排队。\n\n"你知道我是谁吗？"\n\n"不管是谁，在我这里都要排队。"苏瑾头也不抬地继续给面前的病人诊治。\n\n萧景琰看着她专注的侧脸，心中莫名一动。这个女人，和京城里那些见到他就贴过来的名门闺秀完全不同。\n\n他居然真的在旁边坐了下来，一直等到最后一个病人离开。\n\n"苏大夫，现在可以给我看了吧？"\n\n苏瑾这才抬头仔细打量他。这位皇子确实长相俊美，但眉宇间有一丝不正常的潮红。\n\n"把手伸出来。"\n\n她搭上他的脉，眉头越皱越紧。\n\n"你中毒了。"\n\n萧景琰面色一变："什么？"\n\n"慢性的，至少三个月。如果不及时解毒，最多再活半年。"\n\n萧景琰背后冷汗涔涔。他身边高手如云，居然被人下了三个月的毒而不自知？\n\n"你能解？"\n\n"能。但有一个条件。"苏瑾看着他的眼睛，"我要你帮我查一件事。"\n\n"什么事？"\n\n"我母亲的死因。"\n\n萧景琰目光一凝。镇北将军夫人的死，当年确实疑点重重。\n\n"好，我答应你。"' },
    ],
    '妖娆阵师': [
      { title: '第一章 穿越成弃女', content: '洛清寒从昏迷中醒来时，发现自己正躺在荒郊野外。\n\n冷风嗖嗖地灌进破旧的衣衫，她不由自主地打了个寒颤。\n\n"这是什么鬼地方……"\n\n记忆融合后，她明白了。这具身体的原主和她同名，是一个家族弃女。因为测试不出任何修炼天赋，被家族赶出了门。\n\n在这个以阵法和武道为尊的天玄大陆，无法修炼就等于废人一个。\n\n"阵法天赋？"洛清寒忽然眼睛一亮。\n\n前世她是世界顶级的数学家和建筑设计师，几何图形、空间结构、能量流向——这些对她来说再熟悉不过了。\n\n而她脑海中，似乎有一个破碎的传承正在觉醒——上古阵帝的传承！\n\n洛清寒捡起一根树枝，在地上画了一个简单的六芒星阵。\n\n当她画完最后一笔时，整个阵法忽然亮了起来！\n\n"这……这是！"\n\n她震惊地看着那个发光的小阵法。在这个世界上，能凭空画阵的人被称为阵师，而阵师的数量比皇室成员还要稀少。\n\n从这一刻起，命运改写。' },
      { title: '第二章 初遇战神', content: '三个月后，洛清寒来到了天玄大陆最繁华的城市——帝都。\n\n她已经能熟练布置三级阵法，这在整个大陆上都是凤毛麟角的存在。\n\n但她也知道自己需要更多的资源和知识来完善传承。\n\n帝都阵法公会，正在举办一年一度的阵法大赛。\n\n"参加比赛需要推荐信。"门口的守卫拦住她。\n\n"我没有推荐信。"\n\n"那你可以走了。"\n\n洛清寒微微一笑，手指在身前的空中虚画了几下。\n\n一道流光闪过，守卫惊愕地发现自己的身上多了一层透明的防护罩。\n\n"凭空画阵？！你是阵师？！"\n\n"现在我可以进去了吗？"\n\n就在这时，一道冷峻的声音从身后传来："让她进去。"\n\n洛清寒回头，看到一个身着黑色盔甲的男人。他面容冷毅，周身散发着铁血杀气。\n\n正是帝国战神——北冥夜。\n\n两人四目相对的那一刹那，洛清寒心头莫名一震。' },
      { title: '第三章 阵法大赛', content: `阵法大赛的赛场设在一个巨大的圆形广场上。\n\n数千名观众围坐四周，参赛者需要在限定时间内布置出最高等级的阵法。\n\n"比赛开始！"\n\n其他参赛者纷纷拿出阵旗、阵盘和各种材料。只有洛清寒，双手空空。\n\n"她连材料都没有？"\n\n"哈哈，我看是来凑数的吧。"\n\n洛清寒丝毫没有在意旁人的嘲讽。她闭上眼睛，双手在空中轻轻舞动。\n\n一道道金色的光线从她指尖流出，在空中交织成一个复杂的立体图案。\n\n"天哪！那是四级空间阵法！"\n\n"她……她是直接用天地灵气画阵？！"\n\n满场哗然。\n\n就连评审席上的会长也站了起来，眼中满是不可置信。\n\n洛清寒画完最后一笔，整个赛场被一个巨大的阵法笼罩。\n\n"此阵名为'星罗万象'，进可攻退可守。任何进入阵中的人，生死由我掌控。"\n\n全场鸦雀无声。\n\n北冥夜站在高台上，看着那个光芒万丈的女子，嘴角难得露出一丝笑意。\n\n"有意思。"` },
    ],
    '星河战神': [
      { title: '第一章 退伍兵', content: '公元2780年，银河联邦，边缘星域。\n\n一家破旧的修理铺里，林浩正在检修一台老旧的机甲。\n\n"叮！检测到宿主精神力达标，战神系统启动中……"\n\n突如其来的声音让林浩吓了一跳。\n\n"谁在说话？"\n\n一道半透明的蓝色光幕出现在他面前：\n\n【战神系统 V8.0】\n【宿主：林浩】\n【精神力：SSS级（已解锁）】\n【当前任务：修复第一代战神机甲（0/1）】\n\n林浩愣住了。在联邦军队服役八年，他见过各种高科技装备，但这种直接植入意识中的系统还是第一次见。\n\n"战神机甲……"\n\n他想起在军中的日子。他是特种机甲兵，操纵机甲在星空作战是他最擅长的事。\n\n可是退役后，他只能在这破烂的修理铺里苟活。\n\n"这或许是一个机会。"林浩眼中燃起了许久未有的光芒。\n\n【任务提示：战神机甲核心部件位于废弃矿星——编号X-537】\n\n"废弃矿星？那片星域可是海盗的地盘。"\n\n但他已。慷俗龆恕１暇梗他从来就不是一个畏惧危险的人。' },
      { title: '第二章 废弃矿星', content: '飞船降落在X-537号矿星表面。\n\n这里曾经是一座繁荣的矿石基地，几十年前被星际海盗洗劫后成为废墟。\n\n林浩穿着环境适应装甲，在残垣断壁中穿行。\n\n"系统，扫描周围。"\n\n【正在扫描……】\n\n【发现十三个敌对目标，距离约两百米。建议绕行。】\n\n林浩眼神一凝。十三个海盗，如果放在他退役前，一个冲锋就能解决。但现在他手无寸铁。\n\n"战神系统，有什么能用的武器吗？"\n\n【精神力武器模块未解锁。】\n\n【建议：利用环境中的废弃机械组建临时防御装置。】\n\n屏幕上出现了一张图纸。\n\n林浩看了一眼就明白了——这是军用的简易防卫炮！\n\n十分钟后，一架临时拼装的自动炮台对准了海盗的营地。\n\n"开火！"\n\n轰鸣声打破了矿星的死寂。在炮火的掩护下，林浩找到了战神机甲的核心部件——一个散发着淡蓝色光芒的能量核心。\n\n"就是这个！"' },
      { title: '第三章 初次出击', content: '将能量核心带回修理铺后，林浩用了一周时间将它激活。\n\n一阵光芒过后，一架六米高的深蓝色机甲出现在他的修理铺里。\n\n"这……就是战神机甲？"\n\n流线型的机体，锋利的棱角，这台机甲比他见过的任何军用机甲都要先进。\n\n【战神机甲已激活】\n【当前战力评估：A级】\n【解锁技能：能量护盾、离子脉冲炮、相位突进】\n\n正在此时，门外传来尖叫声。\n\n林浩冲出去一看，三台海盗机甲正在攻击平民区！\n\n"来得正好。"\n\n他跃入驾驶舱，久违的战斗激情在胸中燃烧。\n\n战神机甲冲天而起！\n\n离子脉冲炮一炮将一台海盗机甲轰成废铁。\n\n"谁？！"\n\n林浩没有回答，而是操控机甲发动相位突进，瞬间出现在第二台机甲身后。\n\n一个干净利落的锁喉，第二台机甲也轰然倒地。\n\n最后一名海盗吓得转身就逃，却被巡逻舰队堵了个正着。\n\n从此，边缘星域有了一个新的传奇——一个驾驭蓝色机甲的无名英雄。' },
    ],
    '阴阳鬼探': [
      { title: '第一章 阴阳眼', content: '我叫沈默，是一个私家侦探。\n\n但我有个秘密——我能看见鬼。\n\n这事说来话长。我八岁那年大病一场，高烧三天不退。等病好了之后，我发现自己的眼睛变了。\n\n我能看到普通人看不到的东西——游荡的亡魂，飘忽的鬼影，还有那些徘徊在世间的怨念。\n\n因为这个能力，我接的案子和别的侦探不太一样。\n\n"沈先生，求求你了！我女儿每天晚上都做噩梦，说有人在床底下看着她……"\n\n"沈哥，我买的房子好像不太干净，能不能帮忙看看……"\n\n"沈默，警局这边有一个案子，情况很诡异，需要你来一趟……"\n\n这天，我接到一个电话。\n\n电话那头是一个女人的声音，断断续续的，像是信号不好。\n\n"沈……默……救……命……"\n\n然后就断线了。\n\n我立刻定位了号码来源——城西废弃的精神病院。\n\n那里，三年前发生过一起震惊全城的惨案。' },
      { title: '第二章 废弃病院', content: '城西精神病院，一片荒芜。\n\n铁门上锈迹斑斑，杂草丛生。我推开门，一股腐烂的气味扑面而来。\n\n手电筒的光在黑暗中划出一道道惨白的光带。\n\n"有人吗？"\n\n没有人回答，但我能感觉到——周围全是"人"。\n\n我的阴阳眼看到，每一间病房里都有苍白的人影在晃动。他们保持着生前最后的姿势，眼神空洞地看着我。\n\n"她应该在楼上。"一个声音从我身后传来。\n\n我猛地回头，看到一个穿着白大褂的中年男人——一个鬼魂。\n\n"你是谁？"\n\n"我是这间医院的院长，三年前……那场大火中没能逃出去。"\n\n我心里一沉。三年前的大火导致三十七人死亡，其中就包括院长郑仁。\n\n"谁放的火？"\n\n"一个……不应该存在于这世间的东西。"院长的鬼魂颤抖着说，"它还在上面，我能感觉到。"\n\n我握紧手中的符纸。从事这份工作这么久，我很少感到恐惧。\n\n但此刻，一股寒意从脊椎直冲头顶。' },
      { title: '第三章 红衣女鬼', content: '二楼走廊尽头，站着一个穿红衣的女人。\n\n她的脸被长长的头发遮住，手里抱着一个布娃娃。\n\n"你来了……"\n\n声音既像来自远方，又像直接在我心底响起。\n\n"你叫什么名字？为什么要放火？"\n\n红衣女人发出一阵似哭似笑的声音："我叫什么？他们不给我名字……他们把我关在这里十年……十年！"\n\n我心中一动，想起了这间医院的传闻——二十年前，有一个无名女子被家人抛弃在这里，一锁就是十年。\n\n"他们都是帮凶，每一个人，都该死。"\n\n"你已经报仇了。三十七条人命，够了吗？"\n\n"不够！"她猛地抬起头，露出一双血红的眼睛，"我要所有人都陪葬！"\n\n整栋楼开始剧烈摇晃，所有鬼魂都在尖叫。\n\n我知道不能再拖了。\n\n从怀里掏出祖传的桃木剑和符咒，我咬破手指在符上写下一个血红色的"封"字。\n\n"天地无极，乾坤借法！封！"\n\n金光大盛。\n\n红衣女鬼发出刺耳的尖叫，身影在金光中渐渐消散。\n\n"安息吧。你受过的苦，到此为止了。"\n\n金光散尽，整栋楼恢复了宁静。\n\n我走出病院时，天色已经微亮。新的太阳升起来了。' },
    ],
  };

  // Get current chapters to determine what needs to be added
  for (const novel of newNovels) {
    const token = authorTokens[novel.author];
    if (!token) continue;

    const chapters = chapterData[novel.title];
    if (!chapters) continue;

    for (const ch of chapters) {
      const res = await api('POST', '/api/novel/chapter', {
        novelId: novel.id,
        chapterTitle: ch.title,
        content: ch.content,
      }, token);
      if (res.code === 200) {
        log(`  ${novel.title} - ${ch.title} 添加成功`, true);
      } else {
        log(`  ${novel.title} - ${ch.title} 添加失败: ${res.message}`, false);
      }
    }
  }

  // 9. 补充已有小说章节
  console.log('\n--- 9. 补充已有小说章节 ---');
  const existingChapterAdds = [
    // 都市最强医者 (novel 2) - currently has 2 chapters, add chapter 3
    { novelId: 2, token: ADMIN_TOKEN, title: '第三章 银针圣手', content: '唐峰的中医馆正式开业了。\n\n门面不大，地段也不算好，但胜在清净。他没有做任何宣传，只是挂了一块简单的木牌——"唐氏中医馆"。\n\n第一个上门的病人是个意外。隔壁卖水果的大妈在门口摔了一跤，手腕脱臼了。\n\n"大妈别动，我帮你接上。"\n\n唐峰手法娴熟，只听"咔嗒"一声，脱臼的手腕恢复了原位。\n\n"哎呀！不疼了！"大妈惊喜地活动着手腕，"小唐，你这手艺好啊！"\n\n大妈走后，逢人就说唐峰的神奇医术。\n\n一传十，十传百，来找他看病的人越来越多。\n\n这天，来了一位特殊的客人。一个西装革履的中年人，面色苍白，手捂着胸口。\n\n"唐医生，我这是老毛病了……"\n\n唐峰搭了脉，脸色微变："你这是心肌缺血，而且堵塞已经超过80%，必须马上治疗。"\n\n"可是我已经预约了京城最好的心外科医生，下周手术。"\n\n"来不及了。"唐峰取出银针，"如果你信得过我，我可以用银针帮你疏通血管。"\n\n中年人犹豫了片刻，咬牙道："来！"\n\n银针刺入，肉眼看不到的内力通过银针渡入血管，一点点地疏通着堵塞。\n\n半个时辰后，中年人长长地吐出一口浊气。\n\n"我……感觉好多了。"\n\n他没有告诉唐峰，他是江城市首富——李长河。从这一刻起，唐峰的人生将迎来重大转折。\n\n三天后，李长河带着一张一千万的支票重新登门。' },
    // 星际流浪者 (novel 3) - currently has 2 chapters, add chapter 3
    { novelId: 3, token: ADMIN_TOKEN, title: '第三章 星际逃亡', content: '叶星辰抱着那个加密的银色数据盒，心跳如鼓。\n\n"不能回主星。"他迅速做出了判断。如果那些人能在空间站找到他，那主星更不安全。\n\n他调出飞船的星图，目光定格在一片灰色区域——暗域。\n\n那里是星际海盗和赏金猎人的地盘，联邦管辖不到。\n\n"最危险的地方反而是最安全的地方。"\n\n他将飞船的动力开到最大，朝着暗域的方向飞去。\n\n十分钟后，他收到了加密数据盒自动解密后的信息。\n\n那是一段全息视频。视频中，一个白发苍苍的老者对着镜头严肃地说：\n\n"如果你看到这段视频，说明我已经不在了。我是联邦科学院的张海洋博士。下面的内容涉及联邦最高机密——曲速引擎技术。这项技术如果落入军方手中，将引发星际战争……"\n\n叶星辰越看越心惊。曲速引擎——这是每一个星际文明都梦寐以求的终极技术。有了它，跨星系航行只需几天而不是几年。\n\n"难怪他们要追杀我。"\n\n他看着屏幕上密密麻麻的公式和图纸，忽然意识到自己卷入了一个巨大的漩涡。\n\n背后追击的飞船越来越近了。叶星辰深吸一口气。\n\n"来吧，想抓我叶星辰，没那么容易！"\n\n他操控飞船连续做了三个高难度翻滚，钻进了一片小行星带。' },
    // 苍穹破天录 (novel 1) - currently has 3 chapters, add chapters 4-5
    { novelId: 1, token: ADMIN_TOKEN, title: '第四章 秘境试炼', content: '北灵镇的秘境入口，每十年才开启一次。\n\n今年恰好是开启之年。林尘作为镇上为数不多的修炼者，获得了进入秘境的资格。\n\n"记住，秘境里危险重重，但也伴随着巨大的机缘。"教头在临行前严肃叮嘱，"你们最多只能在里面待七天。七天不出，就会被困在里面。"\n\n秘境入口是一道散发着荧光的空间裂缝。\n\n林尘和另外六个少年一起踏入了裂缝。一阵天旋地转后，他们发现自己置身于一片原始森林中。\n\n"大家分开还是集中行动？"有人问道。\n\n"集中行动更安全……"\n\n话音未落，一道黑影从林中窜出，直扑最近的少年。\n\n林尘反应最快，一拳轰出，将黑影击飞。\n\n那竟是一头通体漆黑的豹子，身上散发着淡淡的妖气。\n\n"是妖兽！大家小心！"\n\n妖兽是秘境中最常见的威胁，但同时也是历练的最佳对手。\n\n林尘身怀上古传承，对付这些低阶妖兽并不困难。他带领着其他少年一步步深入秘境。\n\n第五天，他们在一处山谷中发现了一座古老的宫殿。\n\n宫殿大门上刻着一行字："欲入此门，须通过天地人三关。"\n\n林尘眼中闪过一丝兴奋。他知道，这里才是秘境真正的机缘所在。' },
    { novelId: 1, token: ADMIN_TOKEN, title: '第五章 决战时刻', content: '宫殿第一关——天门。\n\n踏入宫殿的瞬间，林尘感到一股巨大的压力从天而降。这压力并非物理攻击，而是精神层面的威压。\n\n其他少年早已支撑不住，纷纷盘膝坐下。只有林尘，凭借着神魔传承淬炼过的意志，一步步向前。\n\n每走一步，压力都翻倍增加。\n\n"我……不能……放弃……"\n\n第九十九步，林尘终于走到了终点。\n\n第二关——地门。\n\n脚下的大地突然裂开，岩浆涌动，无数火蛇从地底钻出。\n\n林尘运转神魔之力，周身浮现出一层淡金色的护罩。那些火蛇撞在护罩上，竟然被吸收了进去。\n\n"原来如此，这一关考验的是防御和吸收。"\n\n他稳扎稳打，一步一个脚印地通过了地门。\n\n第三关——人门。\n\n林尘看到了自己——准确地说，是一个和他一模一样的影子对手。\n\n"战胜自己，才能超越自己。"\n\n两个"林尘"展开了激烈的战斗。拳拳到肉，每一招都和对方完全相同。\n\n打了足足两个时辰，林尘终于发现——影子的力量完全来源于自己。如果自己停止攻击，影子也会停下来。\n\n"战胜自己不是要打败自己，而是要超越自己。"\n\n他闭上眼睛，不再理会影子。当他再次睁眼时，影子已经消失了。\n\n三关通过，宫殿深处的一件宝物缓缓浮现——一件古朴的战甲。\n\n"我的第一个机缘，就是你了。"' },
  ];

  for (const chData of existingChapterAdds) {
    const token = chData.token;
    const res = await api('POST', '/api/novel/chapter', {
      novelId: chData.novelId,
      chapterTitle: chData.title,
      content: chData.content,
    }, token);
    if (res.code === 200) {
      log(`${chData.title} 添加成功`, true);
    } else {
      log(`${chData.title} 添加失败: ${res.message}`, false);
    }
  }

  // 10. Add bookshelf entries for readers
  console.log('\n--- 10. 添加书架收藏 ---');
  const allNovelIds = [1, 2, 3, ...newNovels.map(n => n.id)];
  const bookshelfAssignments = [
    { reader: 'reader01', novelIds: [1, 2, ...newNovels.filter(n => n.author !== 'author01').slice(0, 2).map(n => n.id)] },
    { reader: 'reader02', novelIds: [1, 3, ...newNovels.filter(n => n.categoryId === 5 || n.categoryId === 6).map(n => n.id)] },
    { reader: 'reader03', novelIds: [2, 3, ...newNovels.filter(n => n.categoryId === 3 || n.categoryId === 4).map(n => n.id)] },
  ];

  for (const assignment of bookshelfAssignments) {
    const token = readerTokens[assignment.reader];
    if (!token) continue;
    for (const novelId of assignment.novelIds) {
      if (!novelId) continue;
      const res = await api('POST', `/api/bookshelf/${novelId}`, null, token);
      if (res.code === 200) {
        log(`${assignment.reader} 收藏小说(${novelId})`, true);
      } else {
        log(`${assignment.reader} 收藏小说(${novelId}) 失败: ${res.message}`, false);
      }
    }
  }

  // 11. Add reading history
  console.log('\n--- 11. 添加阅读记录 ---');
  // Dynamically look up chapter IDs
  async function getChaptersForNovel(novelId) {
    const res = await api('GET', `/api/novel/chapter/list/${novelId}`, null);
    return res.data || [];
  }

  const chaptersNovel1 = await getChaptersForNovel(1);
  const chaptersNovel2 = await getChaptersForNovel(2);
  const chaptersNovel3 = await getChaptersForNovel(3);

  function pickChapter(chapters, index) {
    if (chapters && chapters.length > index) {
      return { id: chapters[index].id, title: chapters[index].chapterTitle };
    }
    if (chapters && chapters.length > 0) {
      return { id: chapters[0].id, title: chapters[0].chapterTitle };
    }
    return null;
  }

  const readingAssignments = [
    { reader: 'reader01', ch: pickChapter(chaptersNovel1, 0) },
    { reader: 'reader01', ch: pickChapter(chaptersNovel1, 1) },
    { reader: 'reader02', ch: pickChapter(chaptersNovel1, 2) },
    { reader: 'reader02', ch: pickChapter(chaptersNovel2, 0) },
    { reader: 'reader03', ch: pickChapter(chaptersNovel3, 0) },
    { reader: 'reader03', ch: pickChapter(chaptersNovel2, 1) },
  ];

  for (const ra of readingAssignments) {
    if (!ra.ch) continue;
    const token = readerTokens[ra.reader];
    if (!token) continue;
    const novelId = ra.ch.id <= 5 ? 1 : (ra.ch.id <= 10 ? 2 : 3);
    // Determine novelId from the chapter context
    let nid = 1;
    if (chaptersNovel2.some(c => c.id === ra.ch.id)) nid = 2;
    else if (chaptersNovel3.some(c => c.id === ra.ch.id)) nid = 3;

    const res = await api('POST', '/api/reading-history', {
      novelId: nid,
      chapterId: ra.ch.id,
      chapterTitle: ra.ch.title,
    }, token);
    if (res.code === 200) {
      log(`${ra.reader} 阅读 ${ra.ch.title}`, true);
    } else {
      log(`${ra.reader} 阅读记录失败: ${res.message}`, false);
    }
  }

  // 12. Add comments
  console.log('\n--- 12. 添加评论 ---');
  const comments = [
    { user: 'reader01', novelId: 1, content: '这本书太精彩了！林尘的性格塑造得很好，从一个废柴逆袭的过程让人热血沸腾。期待后续更新！' },
    { user: 'reader02', novelId: 1, content: '时空秘典的设定很有新意，希望作者能把这个修炼体系完整地展开。已收藏，加油！' },
    { user: 'reader03', novelId: 1, content: '文笔不错，节奏紧凑。就是秘境试炼那一段可以再详细一点。整体来说很棒！' },
    { user: 'reader01', novelId: 2, content: '都市修仙类的经典之作，唐峰这个角色很有魅力。银针治病的描写很专业，作者是不是学过医？' },
    { user: 'reader02', novelId: 2, content: '一口气看完了更新的章节，意犹未尽啊！希望更新速度能快一点，每天等更太煎熬了。' },
    { user: 'reader03', novelId: 3, content: '科幻背景下的热血故事，机甲战斗的描写场面感很强。叶星辰这个角色我很喜欢！' },
  ];

  for (const c of comments) {
    const token = readerTokens[c.user];
    if (!token) continue;
    const res = await api('POST', '/api/novel/comment', {
      novelId: c.novelId,
      content: c.content,
    }, token);
    if (res.code === 200) {
      log(`${c.user} 评论小说(${c.novelId}): ${c.content.substring(0, 30)}...`, true);
    } else {
      log(`${c.user} 评论失败: ${res.message}`, false);
    }
  }

  // 13. Add replies
  console.log('\n--- 13. 添加评论回复 ---');
  const authorCommentReplies = [
    { user: 'author01', novelId: 1, content: '感谢支持！我会继续努力的，后续会有更多精彩的剧情展开。' },
    { user: 'author02', novelId: 2, content: '哈哈我不是学医的，但写之前查了很多资料，谢谢你的认可！' },
  ];

  // Get comments for novel 1 and 2
  for (const reply of authorCommentReplies) {
    const token = authorTokens[reply.user];
    if (!token) continue;
    const commentsRes = await api('GET', `/api/novel/comment/list/${reply.novelId}`, null);
    if (commentsRes.data && commentsRes.data.length > 0) {
      const firstComment = commentsRes.data[0];
      const res = await api('POST', '/api/novel/comment', {
        novelId: reply.novelId,
        content: reply.content,
        parentId: firstComment.id,
      }, token);
      log(`${reply.user} 回复评论: ${res.code === 200 ? '成功' : res.message}`, res.code === 200);
    }
  }

  // Summary
  console.log('\n========== 数据填充完成 ==========');
  console.log('新增用户: 6 个');
  console.log('  - 读者: reader01(书虫小明), reader02(深夜读者), reader03(文学少女)');
  console.log('  - 作者: author01(风凌天下), author02(紫月仙子), author03(江南烟雨)');
  console.log(`新增小说: ${newNovels.length} 部`);
  console.log('新增章节: 24+ 章');
  console.log('已添加书架收藏、阅读记录和评论');
  console.log('\n默认密码: Reader@123 / Author@123');
}

main().catch(err => {
  console.error('脚本执行异常:', err.message);
  process.exit(1);
});
