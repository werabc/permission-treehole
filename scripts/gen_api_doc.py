"""
从 Controller 源码生成接口文档（docs/接口文档.md）。
手写 137 个端点必然与代码脱节，这里以源码为准生成，只在文件末尾追加人工章节。
"""
import io
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CTRL = os.path.join(ROOT, 'permission-admin/permission-api/src/main/java/com/permission/controller')
OUT = os.path.join(ROOT, 'docs/接口文档.md')

METHOD_RE = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping(?:\("([^"]*)"\))?')
PARAM_RE = re.compile(r'@(RequestParam|PathVariable|RequestBody|AuthenticationPrincipal)(?:\(([^)]*)\))?\s*(?:final\s+)?([\w<>,\[\]\.\s]+?)\s+(\w+)\s*[,)]')


def parse_params(block):
    """从方法签名里抽取参数说明"""
    params = []
    sig = block[:block.find('{')] if '{' in block else block
    for m in PARAM_RE.finditer(sig):
        kind, ann_args, typ, name = m.group(1), m.group(2) or '', m.group(3).strip(), m.group(4)
        typ = re.sub(r'\s+', ' ', typ).strip()
        required = 'required = false' not in ann_args
        default = ''
        dm = re.search(r'defaultValue\s*=\s*"([^"]*)"', ann_args)
        if dm:
            default = dm.group(1)
        note = name
        if kind == 'RequestParam':
            # 有默认值就不该再说"必填"；且默认值不要用反引号，避免与表格里的反引号嵌套
            if default:
                note += f'（query，可选，默认 {default}）'
            else:
                note += '（query，必填）' if required else '（query，可选）'
        elif kind == 'PathVariable':
            note += '（路径变量）'
        elif kind == 'RequestBody':
            note += f'（body，{typ}）'
        elif kind == 'AuthenticationPrincipal':
            note += '（登录用户，由框架注入）'
        if 'MultipartFile' in typ:
            note = name + '（multipart/form-data 文件字段）'
        params.append(note)
    return params


def extract(path):
    s = io.open(path, encoding='utf-8').read()
    cls = os.path.basename(path)[:-5]
    tag = ''
    tm = re.search(r'@Tag\(name = "([^"]*)"', s)
    if tm:
        tag = tm.group(1)
    base = ''
    bm = re.search(r'@RequestMapping\("([^"]+)"\)', s)
    if bm:
        base = bm.group(1)

    rows = []
    lines = s.split('\n')
    i = 0
    while i < len(lines):
        m = METHOD_RE.search(lines[i])
        if not m:
            i += 1
            continue
        verb = m.group(1).upper()
        sub = m.group(2) or ''
        # 收集本方法块：从 mapping 行到方法体第一个 '{' 后的平衡或下一个 mapping
        j = i + 1
        block_lines = []
        depth_started = False
        while j < len(lines):
            if METHOD_RE.search(lines[j]):
                break
            block_lines.append(lines[j])
            if '{' in lines[j] and not depth_started:
                depth_started = True
                # 找到匹配的收尾（简化：方法体按缩进 4 空格的 } 判断）
                k = j + 1
                while k < len(lines):
                    if lines[k].startswith('    }'):
                        break
                    k += 1
                block_lines.extend(lines[j + 1:k + 1])
                j = k + 1
                break
            j += 1
        # @Operation 通常写在 mapping 之前，必须把前几行一起纳入，否则 summary 永远是空
        head = '\n'.join(lines[max(0, i - 4):i])
        block = head + '\n' + '\n'.join(block_lines)

        perm = ''
        pm = re.search(r"hasAnyAuthority\(([^)]*)\)", block)
        if pm:
            codes = re.findall(r"'([^']+)'", pm.group(1))
            perm = ' / '.join(codes)
        summary = ''
        sm = re.search(r'@Operation\(summary = "([^"]*)"', block)
        if sm:
            summary = sm.group(1)
        op_log = ''
        lm = re.search(r'@OperationLog\(module = "([^"]*)", value = "([^"]*)"', block)
        if lm:
            op_log = f'{lm.group(1)}-{lm.group(2)}'
        ret = ''
        rm = re.search(r'public\s+([\w<>,\[\]\.\s]+?)\s+\w+\s*\(', block)
        if rm:
            ret = re.sub(r'\s+', ' ', rm.group(1)).strip()
        login_required = 'hasAnyAuthority' not in block
        rows.append({
            'verb': verb, 'path': base + sub, 'summary': summary, 'perm': perm,
            'params': parse_params(block), 'ret': ret, 'oplog': op_log, 'block': block,
        })
        i = j if j > i else i + 1
    return cls, tag, base, rows


def load_public_matchers():
    """解析 SecurityConfig 的 permitAll 规则，用于准确区分「公开」与「仅登录」。
    纯靠 Controller 注解无法判断——很多公开接口本身没有任何注解。"""
    rules = []
    sec = os.path.join(ROOT, 'permission-admin/permission-framework/src/main/java/'
                              'com/permission/framework/config/SecurityConfig.java')
    if os.path.exists(sec):
        txt = io.open(sec, encoding='utf-8').read()
        # 关键：不能用 requestMatchers\(([^;]*?)\)\s*\.permitAll\(\) ——
        # [^;]*? 会跨越 ")" 一路吞到后面几条语句，把 POST/DELETE 的路径也算成 permitAll。
        # 正确做法：从每个 .permitAll() 往前找最近的 requestMatchers( 作为起点。
        for m in re.finditer(r'\.permitAll\(\)', txt):
            start = txt.rfind('requestMatchers(', 0, m.start())
            if start == -1:
                continue
            args = txt[start + len('requestMatchers('):m.start()]
            methods = re.findall(r'HttpMethod\.(\w+)', args)
            for path in re.findall(r'"([^"]+)"', args):
                rules.append(('GET' if methods == ['GET'] else None, path))

    const = os.path.join(ROOT, 'permission-admin/permission-common/src/main/java/'
                               'com/permission/common/constant/SecurityConstants.java')
    if os.path.exists(const):
        c = io.open(const, encoding='utf-8').read()
        for m in re.finditer(r'String\s+(LOGIN_URL|REFRESH_TOKEN_URL)\s*=\s*"([^"]+)"', c):
            rules.append((None, m.group(2)))
    return rules


PUBLIC_RULES = load_public_matchers()


def is_public(verb, path):
    for method, rule in PUBLIC_RULES:
        if method and method != verb:
            continue
        regex = re.escape(rule)
        regex = regex.replace(r'\{id\}', '[^/]+')
        regex = re.sub(r'\\\{[^}]+\\\}', '[^/]+', regex)
        regex = regex.replace(r'\*\*', '.*').replace(r'\*', '[^/]*')
        if re.match('^' + regex + '$', path):
            return True
    return False


files = sorted(f for f in os.listdir(CTRL) if f.endswith('.java'))
data = [extract(os.path.join(CTRL, f)) for f in files]
total = sum(len(r[3]) for r in data)

lines = []
w = lines.append
w('# 接口文档')
w('')
w('> 本文件由 `scripts/gen_api_doc.py` 依据 Controller 源码生成，请勿手工编辑端点表格。')
w(f'> 当前共 **{total}** 个端点。')
w('')
w('## 通用约定')
w('')
w('### 统一响应结构')
w('')
w('```json')
w('{ "code": 200, "message": "success", "data": {} }')
w('```')
w('')
w('- `code=200` 表示业务成功；非 200 时 `data` 通常为空，`message` 为可直接展示给用户的提示。')
w('- **部分业务失败也返回 HTTP 200 + 业务码**（如"无权操作"是 `code=5002` 而不是 HTTP 403），')
w('  前端判断必须以 `code` 为准，不要只看 HTTP 状态码。')
w('- Jackson 开启了 `non_null`：**为 null 的字段不会出现在 JSON 里**。')
w('  前端读取可选字段请用 `data.field` 之前先判空，或统一按"字段缺失 = 无值"处理。')
w('')
w('### 分页')
w('')
w('列表接口统一入参 `pageNum`（从 1 开始）、`pageSize`（服务端上限 100，超出会被截断）。')
w('返回结构：')
w('')
w('```json')
w('{ "records": [], "total": 0, "size": 10, "current": 1, "pages": 0 }')
w('```')
w('')
w('### 鉴权')
w('')
w('| 端类型 | 说明 |')
w('|---|---|')
w('| 公开 | 无需 Token，如列表 / 详情 / 搜索 / 头像读取 |')
w('| 需登录 | 请求头 `Authorization: Bearer <token>` |')
w('| 需权限码 | 除登录外还要命中注解里的权限码；表里列出全部可接受码，命中任意一个即通过 |')
w('')
w('管理端与树洞端**各自签发 Token，互不通用**：管理端 `/api/auth/login`，树洞端 `/api/th/auth/login`。')
w('')
w('`admin` 角色码是超级权限，在所有 `hasAnyAuthority` 判断里都放行，用于向后兼容。')
w('')
w('---')
w('')

order = [
    'AuthController', 'ThAuthController', 'FileController', 'ThPublicController',
    'DashboardController', 'ThAnalyticsController', 'ThModerationController',
    'ThPostAdminController', 'ThCommentAdminController', 'ThReportAdminController',
    'ThUserAdminController', 'ThCategoryAdminController', 'ThAnnouncementController',
    'ThSensitiveWordAdminController', 'ThSettingsController', 'OnlineUserController',
    'UserController', 'RoleController', 'MenuController', 'DeptController', 'LogController',
]
group_of = {}
for cls, tag, base, rows in data:
    group_of[cls] = (tag, base, rows)

w('## 端点总览')
w('')
w('| 控制器 | 说明 | 端点数 | 路径前缀 |')
w('|---|---|---|---|')
for cls in order:
    if cls not in group_of:
        continue
    tag, base, rows = group_of[cls]
    w(f'| `{cls}` | {tag or "-"} | {len(rows)} | `{base}` |')
handled = set(order)
for cls in sorted(group_of):
    if cls in handled:
        continue
    tag, base, rows = group_of[cls]
    w(f'| `{cls}` | {tag or "-"} | {len(rows)} | `{base}` |')
w('')

w('---')
w('')

seq = [c for c in order if c in group_of] + sorted(set(group_of) - handled)
for cls in seq:
    tag, base, rows = group_of[cls]
    w(f'## {tag or cls}')
    w('')
    w(f'`{cls}` · 前缀 `{base}`')
    w('')
    w('| 方法 | 路径 | 说明 | 权限码 | 参数 | 返回 |')
    w('|---|---|---|---|---|---|')
    for r in rows:
        if r['perm']:
            perm = f'`{r["perm"]}`'
        elif is_public(r['verb'], r['path']):
            perm = '公开'
        else:
            perm = '仅登录'
        ps = '<br>'.join(f'`{p}`' for p in r['params']) or '-'
        ret = f'`{r["ret"]}`' if r['ret'] else '-'
        w(f'| {r["verb"]} | `{r["path"]}` | {r["summary"] or "-"} | {perm} | {ps} | {ret} |')
    w('')

appendix = os.path.join(ROOT, 'scripts/api_doc_appendix.md')
if os.path.exists(appendix):
    lines.append('')
    lines.append(io.open(appendix, encoding='utf-8').read().rstrip())
    lines.append('')

io.open(OUT, 'w', encoding='utf-8', newline='').write('\n'.join(lines))
print(f'已生成 {OUT}，端点 {total} 个' + ('（含附录）' if os.path.exists(appendix) else ''))
