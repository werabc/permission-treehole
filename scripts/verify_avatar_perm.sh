#!/usr/bin/env bash
# ============================================================================
# 验证：头像上传链路 + 树洞管理端细粒度权限 + 点赞态接口
#
# 前置：
#   1. 后端跑在 8081，连 permission_admin_e2e（已加载 migration_v1.3.0.sql）
#   2. 本机 MySQL / Redis 可用（读验证码需要）
#   FILE_STORAGE_DIR 指向项目内 uploads，方便断言落盘文件
#
# 运行：bash scripts/verify_avatar_perm.sh
# ============================================================================
set -uo pipefail

BASE="http://localhost:8081"
ADMIN_PWD="${ADMIN_PWD:-Admin@1234}"
REDIS_CLI="${REDIS_CLI:-/d/redis/redis-cli}"
REDIS_AUTH="${REDIS_AUTH:-redis123456}"
MYSQL="${MYSQL:-/d/MYsql1/bin/mysql.exe}"
MYSQL_AUTH="-h127.0.0.1 -uroot -p123456abc --default-character-set=utf8mb4 permission_admin_e2e"
UPLOAD_DIR="D:/开发项目/s1/permission-admin/uploads"
# 注意：不要用 mktemp -d（生成 /tmp/... 会被 MSYS 路径转换搞坏，
# 表现为 curl 报 "option --data-binary: error encountered when reading a file"）
TMP="D:/开发项目/s1/scripts/.tmp"
rm -rf "$TMP"; mkdir -p "$TMP"
PASS=0; FAIL=0

J() { python -c "import sys,json;d=json.load(sys.stdin);print(eval(sys.argv[1]))" "$1" 2>/dev/null || echo ""; }

eq() { # 描述 期望 实际
  local d="$1" exp="$2" act="$3"
  if [ "$exp" = "$act" ]; then echo "  [PASS] $d"; PASS=$((PASS+1))
  else echo "  [FAIL] $d :: 期望=[$exp] 实际=[$act]"; FAIL=$((FAIL+1)); fi
}
neq() {
  local d="$1" bad="$2" act="$3"
  if [ "$bad" != "$act" ] && [ -n "$act" ]; then echo "  [PASS] $d"; PASS=$((PASS+1))
  else echo "  [FAIL] $d :: 不应为[$bad] 实际=[$act]"; FAIL=$((FAIL+1)); fi
}

code() { echo "$1" | J "d.get('code','')"; }
msg()  { echo "$1" | J "d.get('message','')"; }

sql() { "$MYSQL" $MYSQL_AUTH -N -B -e "$1" 2>/dev/null; }

# 管理端登录：验证码取自 Redis，用页面拿到的 key；每个账号独立 X-Forwarded-For 规避按 IP 限流
admin_token() {
  local u="$1" cap key vcode
  cap=$(curl -s "$BASE/api/auth/captcha")
  key=$(echo "$cap" | J "d['data']['captchaKey']")
  vcode=$($REDIS_CLI -h 127.0.0.1 -p 6379 -a "$REDIS_AUTH" GET "captcha:$key" 2>/dev/null | tr -d '\r\n"')
  printf '%s' "{\"username\":\"$u\",\"password\":\"$ADMIN_PWD\",\"captchaKey\":\"$key\",\"captchaCode\":\"$vcode\"}" \
    | curl -s -X POST "$BASE/api/auth/login" \
        -H "X-Forwarded-For: 10.20.$((RANDOM % 200 + 1)).$((RANDOM % 200 + 1))" \
        -H 'Content-Type: application/json' --data-binary @- | J "d['data']['accessToken']"
}

# 树洞端登录不需要验证码
th_token() {
  printf '%s' "{\"username\":\"$1\",\"password\":\"$2\"}" \
    | curl -s -X POST "$BASE/api/th/auth/login" -H 'Content-Type: application/json' --data-binary @- | J "d['data']['token']"
}

# send 方法 路径 token [body-file]
send() {
  local m="$1" p="$2" t="$3" body="${4:-}"
  if [ -n "$body" ]; then
    curl -s -X "$m" "$BASE$p" -H "Authorization: Bearer $t" -H 'Content-Type: application/json' --data-binary @"$body"
  else
    curl -s -X "$m" "$BASE$p" -H "Authorization: Bearer $t"
  fi
}
http_status() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

# 每轮从干净的存储目录开始，否则上一轮落盘的文件会让计数断言失真
rm -rf "$UPLOAD_DIR"; mkdir -p "$UPLOAD_DIR/avatar"

echo "############ 1. 准备账号与测试图片 ############"
ADMIN=$(admin_token admin)
AUDITOR=$(admin_token th_auditor)
OPERATOR=$(admin_token th_operator)
eq "admin 登录成功" "" "$([ -z "$ADMIN" ] && echo '空token')"
neq "th_auditor 能登录（非 admin 角色）" "" "$AUDITOR"
neq "th_operator 能登录" "" "$OPERATOR"

# 生成一张真实 PNG（1x1）
python - "$TMP" << 'PY'
import base64, sys, os
png = base64.b64decode(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==')
open(os.path.join(sys.argv[1], 'ok.png'), 'wb').write(png)
# 伪装成 png 的文本文件：用于验证"靠魔数判定而不是扩展名"
open(os.path.join(sys.argv[1], 'fake.png'), 'w', encoding='utf-8').write('this is not an image at all')
# SVG 内嵌 script：如果被放行就是存储型 XSS
open(os.path.join(sys.argv[1], 'xss.svg'), 'w', encoding='utf-8').write(
    '<svg xmlns="http://www.w3.org/2000/svg"><script>alert(1)</script></svg>')
# 3MB 大文件
open(os.path.join(sys.argv[1], 'big.png'), 'wb').write(b'\x89PNG\r\n\x1a\n' + b'0' * (3 * 1024 * 1024))
PY
ls -la "$TMP" > /dev/null

echo ""
echo "############ 2. 头像上传 ############"
UP=$(curl -s -X POST "$BASE/api/file/avatar" -H "Authorization: Bearer $ADMIN" -F "file=@$TMP/ok.png")
eq "上传合法 PNG 成功" "200" "$(code "$UP")"
AVATAR_URL=$(echo "$UP" | J "d['data']")
neq "返回可直接使用的相对地址" "" "$AVATAR_URL"
echo "       返回地址: $AVATAR_URL"

# 形如 /api/file/avatar/<32hex>.png
case "$AVATAR_URL" in
  /api/file/avatar/*.png) echo "  [PASS] 地址格式符合约定"; PASS=$((PASS+1)) ;;
  *) echo "  [FAIL] 地址格式异常: $AVATAR_URL"; FAIL=$((FAIL+1)) ;;
esac

eq "上传成功后可读（HTTP 200）" "200" "$(http_status "$BASE$AVATAR_URL")"
CT=$(curl -s -o /dev/null -w '%{content_type}' "$BASE$AVATAR_URL")
eq "Content-Type 为 image/png（不带多余 charset）" "image/png" "$CT"

# 落盘文件名应由服务端生成，且不含客户端原始名
EQF=$(ls "$UPLOAD_DIR/avatar" 2>/dev/null | head -1)
case "$EQF" in
  [0-9a-f]*.png) echo "  [PASS] 落盘文件名为服务端生成的随机名"; PASS=$((PASS+1)) ;;
  *) echo "  [FAIL] 落盘文件名异常: $EQF"; FAIL=$((FAIL+1)) ;;
esac

echo ""
echo "------------ 2.1 恶意 / 非法上传必须被拒 ------------"
R=$(curl -s -X POST "$BASE/api/file/avatar" -H "Authorization: Bearer $ADMIN" -F "file=@$TMP/fake.png")
eq "伪装扩展名的文本文件被拒" "400" "$(code "$R")"

R=$(curl -s -X POST "$BASE/api/file/avatar" -H "Authorization: Bearer $ADMIN" -F "file=@$TMP/xss.svg;type=image/svg+xml")
eq "SVG（可内嵌脚本）被拒" "400" "$(code "$R")"

R=$(curl -s -X POST "$BASE/api/file/avatar" -H "Authorization: Bearer $ADMIN" -F "file=@$TMP/big.png")
eq "超过 2MB 被业务校验拒绝（400，非 500）" "400" "$(code "$R")"

R=$(curl -s -X POST "$BASE/api/file/avatar" -H "Authorization: Bearer $ADMIN" -F "file=@$TMP/ok.png;filename=../../evil.png")
eq "带路径穿越的文件名仍安全落盘" "200" "$(code "$R")"
TRAV=$(sql "SELECT COUNT(*) FROM dual")
eq "uploads 目录外无越权文件（目录下文件数=2）" "2" "$(ls "$UPLOAD_DIR/avatar" 2>/dev/null | wc -l | tr -d ' ')"

S=$(http_status -X POST "$BASE/api/file/avatar" -F "file=@$TMP/ok.png")
neq "未登录上传被拒（非 200）" "200" "$S"

echo ""
echo "------------ 2.2 读取接口的越权与穿越防护 ------------"
# 编码后的 ..%2f 会被 Tomcat 直接拦成 400（连业务代码都到不了），同样达到拒绝目的
TS=$(http_status "$BASE/api/file/avatar/..%2f..%2fpom.xml")
neq "路径穿越读文件被拒（400/404 均可）" "200" "$TS"
eq "非法文件名 → 404" "404" "$(http_status "$BASE/api/file/avatar/notauuid.png")"
eq "不存在的文件 → 404" "404" "$(http_status "$BASE/api/file/avatar/00000000000000000000000000000000.png")"
eq "非白名单分类 → 404" "404" "$(http_status "$BASE/api/file/secret/00000000000000000000000000000000.png")"

echo ""
echo "############ 3. 头像写入资料并回显 ############"
printf '%s' "{\"avatar\":\"$AVATAR_URL\"}" > "$TMP/profile.json"
# 注意：管理端 token 与树洞端 token 不通用，这里用树洞账号验证。
# 树洞用户没有种子数据，脚本自己注册一个，避免依赖库里的历史数据。
TH_USER="verify_$((RANDOM % 100000))"
printf '%s' "{\"username\":\"$TH_USER\",\"password\":\"Verify@1234\"}"   | curl -s -X POST "$BASE/api/th/auth/register" -H 'Content-Type: application/json' --data-binary @- > /dev/null
TH1=$(th_token "$TH_USER" 'Verify@1234')
neq "树洞用户 e2e_u1 登录成功" "" "$TH1"
R=$(send PUT '/api/th/user/profile' "$TH1" "$TMP/profile.json")
eq "把上传地址写入个人资料成功" "200" "$(code "$R")"
INFO=$(send GET '/api/th/auth/user-info' "$TH1")
eq "user-info 回显该头像" "$AVATAR_URL" "$(echo "$INFO" | J "d['data']['avatar']")"

printf '%s' '{"avatar":"javascript:alert(1)"}' > "$TMP/bad.json"
R=$(send PUT '/api/th/user/profile' "$TH1" "$TMP/bad.json")
eq "javascript: 伪协议头像被拒" "400" "$(code "$R")"

printf '%s' '{"avatar":"https://example.com/a.png"}' > "$TMP/ext.json"
R=$(send PUT '/api/th/user/profile' "$TH1" "$TMP/ext.json")
eq "外链 https 头像仍允许" "200" "$(code "$R")"

echo ""
echo "############ 4. 树洞管理端细粒度权限 ############"
echo "------------ 4.1 内容审核员：该能进的能进，该拦的拦住 ------------"
eq "审核员看待审帖子 → 放行" "200" "$(code "$(send GET '/api/admin/th/moderation/posts?pageNum=1&pageSize=5' "$AUDITOR")")"
eq "审核员看待审评论 → 放行" "200" "$(code "$(send GET '/api/admin/th/moderation/comments?pageNum=1&pageSize=5' "$AUDITOR")")"
eq "审核员看帖子列表 → 放行" "200" "$(code "$(send GET '/api/admin/th/post/page?pageNum=1&pageSize=5' "$AUDITOR")")"
eq "审核员看举报列表 → 放行" "200" "$(code "$(send GET '/api/admin/th/report/page?pageNum=1&pageSize=5' "$AUDITOR")")"
eq "审核员改站点配置 → 403" "403" "$(http_status -X PUT "$BASE/api/admin/th/settings" -H "Authorization: Bearer $AUDITOR" -H 'Content-Type: application/json' -d '{"post_need_audit":"0"}')"
eq "审核员看站点配置 → 403" "403" "$(http_status "$BASE/api/admin/th/settings" -H "Authorization: Bearer $AUDITOR")"
eq "审核员看用户列表 → 403" "403" "$(http_status "$BASE/api/admin/th/user/page?pageNum=1&pageSize=5" -H "Authorization: Bearer $AUDITOR")"
eq "审核员删帖 → 403（未授予 th:post:delete）" "403" "$(http_status -X DELETE "$BASE/api/admin/th/post/1" -H "Authorization: Bearer $AUDITOR")"
eq "审核员隐藏帖子 → 放行（有 th:post:hide）" "200" "$(code "$(send PUT '/api/admin/th/post/1/hide?status=0' "$AUDITOR")")"
eq "审核员动词库 → 403" "403" "$(http_status -X POST "$BASE/api/admin/th/sensitive-word/refresh" -H "Authorization: Bearer $AUDITOR")"

echo ""
echo "------------ 4.2 运营：分类/公告/看板放行，内容处置被拦 ------------"
eq "运营看分类列表 → 放行" "200" "$(code "$(send GET '/api/admin/th/category/list' "$OPERATOR")")"
eq "运营看公告列表 → 放行" "200" "$(code "$(send GET '/api/admin/th/announcement/page?pageNum=1&pageSize=5' "$OPERATOR")")"
eq "运营看数据看板 → 放行" "200" "$(code "$(send GET '/api/admin/th/analytics/overview' "$OPERATOR")")"
eq "运营看首页统计 → 放行" "200" "$(code "$(send GET '/api/dashboard/overview' "$OPERATOR")")"
eq "运营封号 → 403" "403" "$(http_status -X PUT "$BASE/api/admin/th/user/1000/ban?days=7" -H "Authorization: Bearer $OPERATOR")"
eq "运营处理举报 → 403" "403" "$(http_status -X PUT "$BASE/api/admin/th/report/1/handle?status=1" -H "Authorization: Bearer $OPERATOR")"

echo ""
echo "------------ 4.3 admin 仍然全通（向后兼容） ------------"
eq "admin 看用户列表 → 放行" "200" "$(code "$(send GET '/api/admin/th/user/page?pageNum=1&pageSize=5' "$ADMIN")")"
eq "admin 看站点配置 → 放行" "200" "$(code "$(send GET '/api/admin/th/settings' "$ADMIN")")"
eq "admin 动词库 → 放行" "200" "$(code "$(send POST '/api/admin/th/sensitive-word/refresh' "$ADMIN")")"

echo ""
echo "------------ 4.4 菜单可见性随角色变化 ------------"
M=$(send GET '/api/menu/user-menus' "$AUDITOR")
eq "审核员看不到【站点配置】菜单" "0" "$(echo "$M" | python -c "
import sys,json
d=json.load(sys.stdin).get('data') or []
def walk(ns,acc):
    for n in ns:
        acc.append(n.get('menuName') or '')
        walk(n.get('children') or [],acc)
a=[];walk(d,a);print(sum(1 for x in a if x=='站点配置'))" 2>/dev/null)"
eq "审核员能看到【内容审核】菜单" "1" "$(echo "$M" | python -c "
import sys,json
d=json.load(sys.stdin).get('data') or []
def walk(ns,acc):
    for n in ns:
        acc.append(n.get('menuName') or '')
        walk(n.get('children') or [],acc)
a=[];walk(d,a);print(sum(1 for x in a if x=='内容审核'))" 2>/dev/null)"
M2=$(send GET '/api/menu/user-menus' "$OPERATOR")
eq "运营能看到【分类管理】菜单" "1" "$(echo "$M2" | python -c "
import sys,json
d=json.load(sys.stdin).get('data') or []
def walk(ns,acc):
    for n in ns:
        acc.append(n.get('menuName') or '')
        walk(n.get('children') or [],acc)
a=[];walk(d,a);print(sum(1 for x in a if x=='分类管理'))" 2>/dev/null)"

echo ""
echo "############ 5. 点赞态接口（本轮补齐） ############"
POST_ID=$(sql "SELECT id FROM th_post WHERE status=1 AND deleted=0 ORDER BY id LIMIT 1")
echo "       测试帖子 ID=$POST_ID"

printf '%s' "{\"postId\":$POST_ID,\"content\":\"验收脚本回响\",\"isAnonymous\":0}" > "$TMP/cmt.json"
CMT_ID=$(send POST '/api/th/comment' "$TH1" "$TMP/cmt.json" | J "d['data']")
neq "发一条评论成功" "" "$CMT_ID"

L1=$(send GET "/api/th/comment/page?pageNum=1&pageSize=20&postId=$POST_ID" "$TH1")
# any(...) 返回 True 表示"取到了这条评论且它的 liked 为 false"（初始态正确）
eq "评论列表回填 liked（初始 false）" "True" "$(echo "$L1" | python -c "
import sys,json
r=json.load(sys.stdin)['data']['records']
print(any(c['id']==$CMT_ID and c.get('liked') is False for c in r))" 2>/dev/null)"
neq "评论列表返回 authorAvatar 字段" "缺失" "$(echo "$L1" | python -c "
import sys,json
r=json.load(sys.stdin)['data']['records']
print('存在' if any('authorAvatar' in c for c in r) else '缺失')" 2>/dev/null)"

eq "点赞评论成功" "200" "$(code "$(send POST "/api/th/comment/$CMT_ID/like" "$TH1")")"
L2=$(send GET "/api/th/comment/page?pageNum=1&pageSize=20&postId=$POST_ID" "$TH1")
eq "点赞后 liked=true" "True" "$(echo "$L2" | python -c "
import sys,json
r=json.load(sys.stdin)['data']['records']
print(any(c['id']==$CMT_ID and c.get('liked') is True for c in r))" 2>/dev/null)"
C1=$(echo "$L2" | python -c "
import sys,json
r=json.load(sys.stdin)['data']['records']
print([c['likeCount'] for c in r if c['id']==$CMT_ID][0])" 2>/dev/null)
eq "点赞数为 1" "1" "$C1"

eq "取消点赞评论成功（新接口）" "200" "$(code "$(send DELETE "/api/th/comment/$CMT_ID/like" "$TH1")")"
L3=$(send GET "/api/th/comment/page?pageNum=1&pageSize=20&postId=$POST_ID" "$TH1")
eq "取消后 liked=false" "False" "$(echo "$L3" | python -c "
import sys,json
r=json.load(sys.stdin)['data']['records']
print(any(c['id']==$CMT_ID and c.get('liked') is True for c in r))" 2>/dev/null)"
C2=$(echo "$L3" | python -c "
import sys,json
r=json.load(sys.stdin)['data']['records']
print([c['likeCount'] for c in r if c['id']==$CMT_ID][0])" 2>/dev/null)
eq "取消后点赞数回到 0" "0" "$C2"

# 帖子点赞态：/liked 接口此前前端从未调用
eq "帖子点赞态初始 false" "False" "$(send GET "/api/th/post/$POST_ID/liked" "$TH1" | J "d['data']")"
send POST "/api/th/post/$POST_ID/like" "$TH1" > /dev/null
eq "点赞后 /liked 返回 true" "True" "$(send GET "/api/th/post/$POST_ID/liked" "$TH1" | J "d['data']")"
send DELETE "/api/th/post/$POST_ID/like" "$TH1" > /dev/null
eq "取消后 /liked 回到 false" "False" "$(send GET "/api/th/post/$POST_ID/liked" "$TH1" | J "d['data']")"

echo ""
echo "############ 6. 匿名内容不得泄露头像 ############"
# 先关掉"先审后发"，否则新帖 status=0，详情接口拿不到内容（与匿名逻辑无关）
printf '%s' '{"post_need_audit":"0"}' > "$TMP/audit.json"
send PUT '/api/admin/th/settings' "$ADMIN" "$TMP/audit.json" > /dev/null

# 给该用户设置真实头像，再发匿名帖，验证匿名帖不回传头像（去匿名化防护）
sql "UPDATE th_user SET avatar='$AVATAR_URL' WHERE username='$TH_USER'" > /dev/null
printf '%s' '{"content":"验收脚本匿名帖：不应带出任何作者信息","isAnonymous":1}' > "$TMP/anon.json"
ANON_ID=$(send POST '/api/th/post' "$TH1" "$TMP/anon.json" | J "d['data']")
neq "发一条匿名帖成功" "" "$ANON_ID"
D=$(send GET "/api/th/post/$ANON_ID" "")
eq "匿名帖 authorAvatar 为空（不回传头像）" "None" "$(echo "$D" | J "d['data'].get('authorAvatar')")"
eq "匿名帖 authorName 为匿名用户" "匿名用户" "$(echo "$D" | J "d['data']['authorName']")"
eq "匿名帖 userId 被清空（不可反查）" "None" "$(echo "$D" | J "d['data'].get('userId')")"

# 同一用户的实名帖则必须带出头像
printf '%s' '{"content":"验收脚本实名帖：应当带出头像","isAnonymous":0}' > "$TMP/real.json"
REAL_ID=$(send POST '/api/th/post' "$TH1" "$TMP/real.json" | J "d['data']")
neq "发一条实名帖成功" "" "$REAL_ID"
D2=$(send GET "/api/th/post/$REAL_ID" "")
eq "实名帖带出作者头像" "$AVATAR_URL" "$(echo "$D2" | J "d['data'].get('authorAvatar')")"
L4=$(send GET "/api/th/comment/page?pageNum=1&pageSize=20&postId=$POST_ID" "$TH1")
eq "评论列表也带出 authorAvatar" "True" "$(echo "$L4" | python -c "
import sys,json
r=json.load(sys.stdin)['data']['records']
print(any(c.get('authorAvatar') for c in r))" 2>/dev/null)"

echo ""
echo "############ 7. 清理本轮制造的测试数据 ############"
# 验证脚本会注册用户、发帖、传文件；不清理会把演示库和存储目录搞脏，
# 也会让"未知用户 / 图片 404"这类噪音出现在后续的前端验收里。
sql "DELETE FROM th_comment WHERE user_id IN (SELECT id FROM th_user WHERE username LIKE 'verify\_%')" > /dev/null
sql "DELETE FROM th_notification WHERE user_id IN (SELECT id FROM th_user WHERE username LIKE 'verify\_%') OR sender_id IN (SELECT id FROM th_user WHERE username LIKE 'verify\_%')" > /dev/null
sql "DELETE FROM th_post WHERE user_id IN (SELECT id FROM th_user WHERE username LIKE 'verify\_%')" > /dev/null
sql "DELETE FROM th_user WHERE username LIKE 'verify\_%'" > /dev/null
rm -rf "$UPLOAD_DIR/avatar"
mkdir -p "$UPLOAD_DIR/avatar"
echo "  已清理 verify_* 账号、其内容与上传文件"

echo ""
echo "========================================"
echo "通过 $PASS / $((PASS+FAIL))"
echo "========================================"
rm -rf "$TMP"
[ "$FAIL" -eq 0 ]
