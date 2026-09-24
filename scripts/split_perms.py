"""
把树洞管理端点的 @PreAuthorize 从统一写死的 'admin' 拆成细粒度权限码。

规则：`hasAnyAuthority('<细码>', 'admin')` —— 保留 admin 作为超管兜底，
老角色不受影响，新角色按需授权。

依据：方法名 -> 权限码（与方法在文件中的顺序无关，避免误替换）。
"""
import io
import os
import re

CTRL = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    'permission-admin/permission-api/src/main/java/com/permission/controller')

CODE_MAP = {
    'ThAnalyticsController.java': {
        'overview': 'th:analytics:view', 'trends': 'th:analytics:view', 'categories': 'th:analytics:view',
    },
    'ThAnnouncementController.java': {
        'page': 'th:announcement:list', 'create': 'th:announcement:add',
        'update': 'th:announcement:edit', 'delete': 'th:announcement:delete',
    },
    'ThCategoryAdminController.java': {
        'list': 'th:category:list', 'create': 'th:category:add',
        'update': 'th:category:edit', 'delete': 'th:category:delete',
    },
    'ThCommentAdminController.java': {
        'page': 'th:comment:list', 'detail': 'th:comment:view',
        'hide': 'th:comment:hide', 'delete': 'th:comment:delete',
    },
    'ThModerationController.java': {
        'pendingPosts': 'th:moderation:list', 'pendingComments': 'th:moderation:list',
        'stats': 'th:moderation:list', 'batchAudit': 'th:moderation:audit',
    },
    'ThPostAdminController.java': {
        'page': 'th:post:list', 'detail': 'th:post:view', 'audit': 'th:post:audit',
        'pin': 'th:post:pin', 'hide': 'th:post:hide', 'delete': 'th:post:delete',
    },
    'ThReportAdminController.java': {
        'page': 'th:report:list', 'stats': 'th:report:list', 'detail': 'th:report:view',
        'handle': 'th:report:handle', 'batchHandle': 'th:report:handle',
    },
    'ThSensitiveWordAdminController.java': {
        'page': 'th:sensitive:list', 'stats': 'th:sensitive:list', 'add': 'th:sensitive:add',
        'importBatch': 'th:sensitive:import', 'update': 'th:sensitive:edit',
        'delete': 'th:sensitive:delete', 'refresh': 'th:sensitive:refresh',
    },
    'ThSettingsController.java': {
        'getAll': 'th:settings:view', 'update': 'th:settings:edit',
    },
    'ThUserAdminController.java': {
        'page': 'th:user:list', 'detail': 'th:user:view', 'logs': 'th:user:view',
        'posts': 'th:user:view', 'comments': 'th:user:view',
        'mute': 'th:user:mute', 'unmute': 'th:user:mute',
        'ban': 'th:user:ban', 'unban': 'th:user:ban',
        'release': 'th:user:release', 'addViolation': 'th:user:violation',
    },
}

total, skipped = 0, []
for fname, mapping in CODE_MAP.items():
    path = os.path.join(CTRL, fname)
    s = io.open(path, encoding='utf-8').read()
    lines = s.split('\n')
    changed = 0
    for i, ln in enumerate(lines):
        if not re.search(r'@(Get|Post|Put|Delete)Mapping', ln):
            continue
        # 向后找方法名
        name = ''
        for j in range(i + 1, min(i + 12, len(lines))):
            mm = re.search(r'public\s+[\w<>,\s\.\[\]]+\s+(\w+)\s*\(', lines[j])
            if mm:
                name = mm.group(1)
                break
        if name not in mapping:
            skipped.append(f'{fname}:{name}')
            continue
        code = mapping[name]
        # 关键：本方法的 @PreAuthorize 位于 mapping 注解【之后】、方法签名之前
        # （踩过坑：按"往前找"会命中上一个方法的注解，导致权限码整体错位一位）
        target = None
        for k in range(i + 1, min(i + 8, len(lines))):
            if re.search(r'\bpublic\b', lines[k]):
                break
            if '@PreAuthorize' in lines[k]:
                target = k
                break
        if target is None:
            skipped.append(f'{fname}:{name} (未找到本方法的 @PreAuthorize)')
            continue
        newline = f'    @PreAuthorize("hasAnyAuthority(\'{code}\', \'admin\')")'
        if lines[target].strip() != newline.strip():
            lines[target] = newline
            changed += 1
    if changed:
        io.open(path, 'w', encoding='utf-8', newline='').write('\n'.join(lines))
    total += changed
    print(f'{fname:42} 改写 {changed} 处')

print(f'\n合计改写 {total} 处')
if skipped:
    print('未匹配（需人工确认）:')
    for s_ in skipped:
        print('  -', s_)
