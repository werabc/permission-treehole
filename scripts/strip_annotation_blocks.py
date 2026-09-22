#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""清除 Java 文件中的'文件注解与作用说明'注释块。
块结构： // ====...  /  // 文件注解与作用说明  /  // ====...  /  ...内容...  /  // ====...
只删注释块，不动代码。"""
import re, io, glob

MARKER = re.compile(r'^//\s*=+\s*$')
TITLE = '// 文件注解与作用说明'
changed, errors = [], []

def clean_java(path):
    with io.open(path, 'r', encoding='utf-8') as f:
        lines = f.read().splitlines(keepends=True)
    out, i, removed = [], 0, 0
    while i < len(lines):
        s = lines[i].rstrip('\r\n').strip()
        if (i + 1 < len(lines) and MARKER.match(s)
                and lines[i+1].rstrip('\r\n').strip() == TITLE):
            # i=开分隔线, i+1=标题, i+2=第二根分隔线, 之后找闭合分隔线
            j = i + 3
            while j < len(lines) and not MARKER.match(lines[j].rstrip('\r\n').strip()):
                j += 1
            if j < len(lines):
                removed += (j - i + 1)
                i = j + 1
                continue
            errors.append('%s: 闭合分隔线缺失' % path)
        out.append(lines[i])
        i += 1
    if removed:
        while out and out[-1].strip() == '':
            out.pop()
        with io.open(path, 'w', encoding='utf-8', newline='') as f:
            f.write(''.join(out) + '\n')
        changed.append((path, removed))

for path in glob.glob('permission-admin/**/src/**/*.java', recursive=True):
    clean_java(path)

print('已清理 %d 个文件，共删除 %d 行注释' % (len(changed), sum(n for _, n in changed)))
if errors:
    print('\n[警告] %d 个文件结构异常:' % len(errors))
    for e in errors:
        print('  ' + e)
