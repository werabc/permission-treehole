"""
接口对账：把后端所有端点与两个前端的 API 调用做双向比对。
- 前端调用了后端不存在的端点（真 bug）
- 后端端点前端从未调用（可能是遗漏的功能，也可能是给第三方用的）
"""
import io
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CTRL = os.path.join(ROOT, 'permission-admin/permission-api/src/main/java/com/permission/controller')

# ---------- 1. 抽取后端端点 ----------
backend = []  # (class, verb, path)
for f in sorted(os.listdir(CTRL)):
    if not f.endswith('.java'):
        continue
    s = io.open(os.path.join(CTRL, f), encoding='utf-8').read()
    base = ''
    m = re.search(r'@RequestMapping\("([^"]+)"\)', s)
    if m:
        base = m.group(1)
    for mm in re.finditer(r'@(Get|Post|Put|Delete|Patch)Mapping(?:\("([^"]*)"\))?', s):
        verb = mm.group(1).upper()
        path = (base + (mm.group(2) or '')) or '/'
        backend.append((f[:-5], verb, path))


def to_regex(p):
    r = re.escape(p)
    r = re.sub(r'\\\{[^}]+\\\}', r'[^/]+', r)   # {id} -> 任意段
    return '^' + r + '$'


backend_re = [(cls, v, p, re.compile(to_regex(p))) for cls, v, p in backend]

# ---------- 2. 抽取前端调用 ----------
CALL = re.compile(
    r"""(?:request|http|api|service|axios)\s*\.\s*(get|post|put|delete|patch)"""
    r"""\s*(?:<[^>]*>)?\s*\(\s*[`'"]([^`'"]*)[`'"]""",
    re.I,
)


def norm(path):
    """两个前端 axios baseURL 都是 '/api'，所以调用里的相对路径要先补上前缀再比对。"""
    p = re.sub(r'\$\{[^}]*\}', '{x}', path)
    p = p.split('?')[0] or '/'
    if not p.startswith('/api'):
        p = '/api' + (p if p.startswith('/') else '/' + p)
    return p


FRONTENDS = {
    'C端 treehole-web': os.path.join(ROOT, 'treehole-web/src'),
    '管理端 permission-ui': os.path.join(ROOT, 'permission-ui/src'),
}

report = []
for label, d in FRONTENDS.items():
    calls = {}
    for root, _, files in os.walk(d):
        for f in files:
            if not f.endswith(('.ts', '.vue', '.js')):
                continue
            fp = os.path.join(root, f)
            try:
                s = io.open(fp, encoding='utf-8').read()
            except Exception:
                continue
            for mm in CALL.finditer(s):
                raw = mm.group(2)
                if not raw.startswith('/'):
                    continue
                key = (mm.group(1).upper(), norm(raw))
                calls.setdefault(key, set()).add(os.path.relpath(fp, ROOT))
    report.append((label, calls))

print(f'后端端点共 {len(backend)} 个（去重 {len(set((v, p) for _, v, p in backend))} 个）\n')

used = set()
for label, calls in report:
    print('=' * 76)
    print(f'【{label}】不同调用 {len(calls)} 个')
    print('=' * 76)
    missing = []
    for (verb, p), files in sorted(calls.items()):
        hit = [(cls, bp) for cls, bv, bp, br in backend_re if bv == verb and br.match(p)]
        if hit:
            used.add((verb, p))
        else:
            same = [bv for cv, bv, bp, br in backend_re if br.match(p)]
            missing.append((verb, p, files, same))
    if missing:
        print('\n⚠️  前端调用了后端不存在的端点：')
        for verb, p, files, same in missing:
            extra = f'   ← 同路径存在: {same}' if same else ''
            print(f'  {verb:6} {p}{extra}')
            for f in sorted(files):
                print(f'          ↳ {f}')
    else:
        print('\n✅ 前端所有调用均能在后端找到匹配端点')
    print()

# ---------- 3. 反向：后端存在但前端从未调用 ----------
print('=' * 76)
print('【反向】后端端点但两个前端都未调用（可能遗漏，也可能是给外部/工具用的）')
print('=' * 76)

all_calls = set()
for _, calls in report:
    all_calls |= set(calls.keys())

never = []
for cls, v, p in backend:
    pat = re.compile(to_regex(p))
    if any(cv == v and pat.match(cp) for cv, cp in all_calls):
        continue
    never.append((cls, v, p))

by_cls = {}
for cls, v, p in never:
    by_cls.setdefault(cls, []).append(f'{v:6} {p}')
for cls in sorted(by_cls):
    print(f'\n### {cls}  ({len(by_cls[cls])})')
    for line in sorted(by_cls[cls]):
        print('   ', line)
print(f'\n合计未被调用: {len(never)} / {len(backend)}')
