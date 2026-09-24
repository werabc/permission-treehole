"""
给树洞管理端页面注入按钮级权限控制。

规则：
- 元素上没有 v-if/v-else 分支 → 直接挂 v-permission（与既有 system 页面写法一致）
- 元素本身带 v-if/v-else 配对 → 改写成 can('code') && 原条件
  （原因：v-permission 指令在 mounted 里直接摘掉 DOM，和 v-else 的配对会错乱）
"""
import io
import os

BASE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    'permission-ui/src/views/th-admin')

# ---- 1) v-permission 直接注入：(文件, 整行原文片段, 权限码) ----
ATTR_PLAN = [
    ('post/index.vue', 'handleBatchAudit(1)',   'th:post:audit'),
    ('post/index.vue', 'handleBatchAudit(2)',   'th:post:audit'),
    ('post/index.vue', 'handleBatchPin(1)',     'th:post:pin'),
    ('post/index.vue', 'handleBatchPin(0)',     'th:post:pin'),
    ('post/index.vue', 'viewDetail(row)',       'th:post:view'),
    ('post/index.vue', 'handleDelete(row)',     'th:post:delete'),

    ('comment/index.vue', 'viewDetail(row)',            'th:comment:view'),
    ('comment/index.vue', 'handleBatchHide',            'th:comment:hide'),
    ('comment/index.vue', 'handleBatchShow',            'th:comment:hide'),
    ('comment/index.vue', 'handleBatchDelete',          'th:comment:delete'),
    ('comment/index.vue', 'handleDelete(row)',          'th:comment:delete'),

    ('report/index.vue', 'viewDetail(row)',     'th:report:view'),
    ('report/index.vue', 'handleBatch(1)',      'th:report:handle'),
    ('report/index.vue', 'handleBatch(2)',      'th:report:handle'),
    ('report/index.vue', 'handleReport(row, 1)', 'th:report:handle'),
    ('report/index.vue', 'handleReport(row, 2)', 'th:report:handle'),

    ('moderation/index.vue', "batchAudit('posts', 1)",   'th:moderation:audit'),
    ('moderation/index.vue', "batchAudit('posts', 2)",   'th:moderation:audit'),
    ('moderation/index.vue', "batchAudit('comments', 1)", 'th:moderation:audit'),
    ('moderation/index.vue', "batchAudit('comments', 2)", 'th:moderation:audit'),
    ('moderation/index.vue', 'auditPost(row.id, 1)',     'th:moderation:audit'),
    ('moderation/index.vue', 'openRejectDialog(row)',    'th:moderation:audit'),
    ('moderation/index.vue', 'auditComment(row.id, 1)',  'th:moderation:audit'),
    ('moderation/index.vue', "openRejectDialog(row, 'comment')", 'th:moderation:audit'),

    ('sensitive/index.vue', 'openCreateDialog',  'th:sensitive:add'),
    ('sensitive/index.vue', 'importVisible = true', 'th:sensitive:import'),
    ('sensitive/index.vue', 'handleRefresh',     'th:sensitive:refresh'),
    ('sensitive/index.vue', 'openEditDialog(row)', 'th:sensitive:edit'),
    ('sensitive/index.vue', 'toggleStatus(row)', 'th:sensitive:edit'),
    ('sensitive/index.vue', 'handleDelete(row)', 'th:sensitive:delete'),

    ('category/index.vue', 'openCreateDialog',     'th:category:add'),
    ('category/index.vue', 'openEditDialog(row)',  'th:category:edit'),
    ('category/index.vue', 'handleDelete(row)',    'th:category:delete'),

    ('announcement/index.vue', 'openCreateDialog',    'th:announcement:add'),
    ('announcement/index.vue', 'openEditDialog(row)', 'th:announcement:edit'),
    ('announcement/index.vue', 'handleDelete(row)',   'th:announcement:delete'),

    ('settings/index.vue', 'saveSettings', 'th:settings:edit'),
]

# ---- 2) 带 v-if/v-else 的行：(文件, 原整行, 新整行) ----
COND_PLAN = [
    ('post/index.vue',
     '<el-button v-if="row.status === 0" link type="success" size="small" @click="handleAudit(row, 1)">通过</el-button>',
     '<el-button v-if="can(\'th:post:audit\') && row.status === 0" link type="success" size="small" @click="handleAudit(row, 1)">通过</el-button>'),
    ('post/index.vue',
     '<el-button v-if="row.status !== 2" link type="danger" size="small" @click="handleAudit(row, 2)">拒绝</el-button>',
     '<el-button v-if="can(\'th:post:audit\') && row.status !== 2" link type="danger" size="small" @click="handleAudit(row, 2)">拒绝</el-button>'),
    ('post/index.vue',
     '<el-button v-if="row.isTop === 1" link type="warning" size="small" @click="handlePin(row, 0)">取消置顶</el-button>',
     '<el-button v-if="can(\'th:post:pin\') && row.isTop === 1" link type="warning" size="small" @click="handlePin(row, 0)">取消置顶</el-button>'),
    ('post/index.vue',
     '<el-button v-else link type="warning" size="small" @click="handlePin(row, 1)">置顶</el-button>',
     '<el-button v-else-if="can(\'th:post:pin\')" link type="warning" size="small" @click="handlePin(row, 1)">置顶</el-button>'),
    ('post/index.vue',
     '<el-button v-if="row.status === 1" link type="info" size="small" @click="handleHide(row, 0)">隐藏</el-button>',
     '<el-button v-if="can(\'th:post:hide\') && row.status === 1" link type="info" size="small" @click="handleHide(row, 0)">隐藏</el-button>'),
    ('post/index.vue',
     '<el-button v-else link type="success" size="small" @click="handleHide(row, 1)">恢复</el-button>',
     '<el-button v-else-if="can(\'th:post:hide\')" link type="success" size="small" @click="handleHide(row, 1)">恢复</el-button>'),
    ('comment/index.vue',
     '<el-button v-if="row.hidden === 0" link type="warning" size="small" @click="handleHide(row, 1)">隐藏</el-button>',
     '<el-button v-if="can(\'th:comment:hide\') && row.hidden === 0" link type="warning" size="small" @click="handleHide(row, 1)">隐藏</el-button>'),
    ('comment/index.vue',
     '<el-button v-else link type="success" size="small" @click="handleHide(row, 0)">显示</el-button>',
     '<el-button v-else-if="can(\'th:comment:hide\')" link type="success" size="small" @click="handleHide(row, 0)">显示</el-button>'),
]

# ---- 3) 需要 can() 的文件 -> 注入 userStore ----
NEED_CAN = ['post/index.vue', 'comment/index.vue']


def inject_attr(path, marker, code):
    s = io.open(path, encoding='utf-8').read()
    lines = s.split('\n')
    hit = 0
    for i, ln in enumerate(lines):
        if f'@click="{marker}"' not in ln and f'@click="{marker};' not in ln:
            continue
        if 'v-permission' in ln or '<el-button' not in ln:
            continue
        lines[i] = ln.replace('<el-button', f'<el-button v-permission="\'{code}\'"', 1)
        hit += 1
    if hit:
        io.open(path, 'w', encoding='utf-8', newline='').write('\n'.join(lines))
    return hit


total = 0
for rel, marker, code in ATTR_PLAN:
    p = os.path.join(BASE, rel)
    n = inject_attr(p, marker, code)
    total += n
    print(f'  {rel:24} {code:22} +{n}')

print(f'\nv-permission 注入合计 {total} 处')

for rel, old, new in COND_PLAN:
    p = os.path.join(BASE, rel)
    s = io.open(p, encoding='utf-8').read()
    assert old in s, f'{rel}: 未匹配 -> {old[:60]}'
    s = s.replace(old, new, 1)
    io.open(p, 'w', encoding='utf-8', newline='').write(s)
print(f'条件式改写完成 {len(COND_PLAN)} 处')

for rel in NEED_CAN:
    p = os.path.join(BASE, rel)
    s = io.open(p, encoding='utf-8').read()
    if 'useUserStore' in s:
        continue
    # 在最后一个 import 之后插入
    lines = s.split('\n')
    last_import = max(i for i, ln in enumerate(lines) if ln.startswith('import '))
    lines.insert(last_import + 1, "import { useUserStore } from '@/stores/user'")
    lines.insert(last_import + 2, '')
    lines.insert(last_import + 3, 'const userStore = useUserStore()')
    # can 助手
    lines.insert(last_import + 4, "/** 按钮级权限判断：admin 角色恒为 true（见 stores/user.hasPermission） */")
    lines.insert(last_import + 5, "const can = (code: string) => userStore.hasPermission(code)")
    io.open(p, 'w', encoding='utf-8', newline='').write('\n'.join(lines))
    print(f'  {rel}: 已注入 userStore / can()')
