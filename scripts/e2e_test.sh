#!/usr/bin/env bash
# ============================================================================
# 端到端全量测试：数据权限 8 级矩阵 + 鉴权边界 + 治理链路 + C 端新功能
#
# 前置：
#   1. 灌库：create_tables.sql + init.sql + migration_v1.1.0.sql + migration_v1.2.0.sql + e2e_seed.sql
#   2. 后端已启动（默认 http://localhost:18081，库 permission_admin_e2e）
#
# 环境适配说明（避免踩坑）：
#   - Git Bash 下 curl -d '中文' 会把非 ASCII 字节搞坏 → 请求体统一走 printf | curl --data-binary @-
#   - 本机 redis-cli 不支持 --no-auth-warning 参数；且值被 GenericJackson2JsonRedisSerializer
#     包了一层 JSON 引号，读取验证码需去掉首尾引号
# 用法：bash scripts/e2e_test.sh
# ============================================================================
set -u

BASE=${BASE:-http://localhost:18081}
MY="/d/MYsql1/bin/mysql.exe"
MYSQL_ARGS=(-h127.0.0.1 -uroot -p123456abc --default-character-set=utf8mb4 permission_admin_e2e)
REDIS_CLI="/d/redis/redis-cli -h 127.0.0.1 -p 6379 -a redis123456"
ADMIN_PWD='Admin@1234'
TH_PWD='E2eTest@1234'

PASS=0; FAIL=0
section(){ echo; echo "===== $1 ====="; }
ok(){ echo "  [PASS] $1"; PASS=$((PASS+1)); }
no(){ echo "  [FAIL] $1  (期望=$2 实际=$3)"; FAIL=$((FAIL+1)); }
eq(){ if [ "$2" = "$3" ]; then ok "$1"; else no "$1" "$2" "$3"; fi; }
neq(){ if [ "$2" != "$3" ]; then ok "$1"; else no "$1" "≠$3" "$2"; fi; }

J(){ python -c "import sys,json
try: d=json.load(sys.stdin)
except Exception: d={}
try: print(eval(sys.argv[1]))
except Exception: print('')" "$1" 2>/dev/null; }

sql(){ "$MY" "${MYSQL_ARGS[@]}" -N -B -e "$1" 2>/dev/null | tr -d '\r'; }

# ---------- 请求 ----------
# 统一用法：send METHOD PATH TOKEN [JSON]  —— JSON 通过 stdin 管道发送，保证中文不乱码
send(){
  local m=$1 p=$2 t=${3:-} b=${4:-}
  if [ -n "$b" ]; then
    printf '%s' "$b" | curl -s -X "$m" "$BASE$p" \
      ${t:+-H "Authorization: Bearer $t"} -H 'Content-Type: application/json' --data-binary @-
  else
    curl -s -X "$m" "$BASE$p" ${t:+-H "Authorization: Bearer $t"}
  fi
}

# 登录辅助（管理端需要验证码）
# 每个账号使用独立 X-Forwarded-For：登录限流按 IP 计数，同 IP 连续登录会被 1009 拦掉
admin_token(){
  local u=$1 cap key code
  cap=$(curl -s "$BASE/api/auth/captcha")
  key=$(echo "$cap" | J "d['data']['captchaKey']")
  code=$($REDIS_CLI GET "captcha:$key" 2>/dev/null | tr -d '\r\n"')
  printf '%s' "{\"username\":\"$u\",\"password\":\"$ADMIN_PWD\",\"captchaKey\":\"$key\",\"captchaCode\":\"$code\"}" \
    | curl -s -X POST "$BASE/api/auth/login" -H "X-Forwarded-For: 10.9.9.$((RANDOM % 200 + 1))" \
      -H 'Content-Type: application/json' --data-binary @- | J "d['data']['accessToken']"
}

visible_count(){
  curl -s "$BASE/api/user/page?pageNum=1&pageSize=100" -H "Authorization: Bearer $1" | J "d['data']['total']"
}

echo "E2E 目标: $BASE"

# ============================================================
section "0. 服务健康"
eq "验证码接口可达" "200" "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/auth/captcha")"

# ============================================================
section "1. 鉴权边界（本轮补的洞）"
eq "未带 Token 访问 /api/auth/user-info → 401" "401" "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/auth/user-info")"
eq "未带 Token 访问 /api/auth/logout → 401" "401" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/auth/logout")"
eq "验证码/登录接口仍公开" "200" "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/auth/captcha")"
eq "未登录访问管理接口 → 401" "401" "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/admin/th/user/page")"

ADMIN=$(admin_token admin)
neq "admin 登录成功拿到 Token" "" "$ADMIN"
eq "带 Token 访问 user-info → 200" "200" "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/auth/user-info" -H "Authorization: Bearer $ADMIN")"

U_SELF=$(admin_token u_self)
eq "无查询权限用户查他人角色 → 403" "403" "$(send GET '/api/user/1000/roles' "$U_SELF" | J "d['code']")"
neq "admin 查角色列表放行" "403" "$(send GET '/api/user/1000/roles' "$ADMIN" | J "d['code']")"

# ============================================================
section "2. 数据权限 8 级矩阵（/api/user/page 可见用户数）"
T_COMP=$(admin_token u_comp);   eq "u_comp 登录" "200" "$([ -n "$T_COMP" ] && echo 200 || echo x)"
T_DEPT=$(admin_token u_dept)
T_LIMIT=$(admin_token u_limit)
T_DEPTON=$(admin_token u_depton)
T_CUSTOM=$(admin_token u_custom)

eq "① 全部数据(admin) = 12" "12" "$(visible_count "$ADMIN")"
eq "② 本公司及以下(u_comp) = 10" "10" "$(visible_count "$T_COMP")"
eq "③ 本部门及以下(u_dept) = 5" "5" "$(visible_count "$T_DEPT")"
eq "④ 本部门及以下限1级(u_limit) = 4" "4" "$(visible_count "$T_LIMIT")"
eq "⑤ 本部门(u_depton) = 3" "3" "$(visible_count "$T_DEPTON")"
eq "⑥ 自定义部门[产品部](u_custom) = 2" "2" "$(visible_count "$T_CUSTOM")"
eq "⑦ 仅本人(u_self) = 1" "1" "$(visible_count "$U_SELF")"

NAMES_COMP=$(curl -s "$BASE/api/user/page?pageNum=1&pageSize=100" -H "Authorization: Bearer $T_COMP" | J "[r['username'] for r in d['data']['records']]")
case "$NAMES_COMP" in *"'admin'"*) no "本公司范围不应看到集团 admin" "不含 admin" "含 admin" ;; *) ok "本公司范围看不到集团 admin" ;; esac
case "$NAMES_COMP" in *"'u_other'"*) no "本公司范围不应看到产品部 u_other" "不含 u_other" "含 u_other" ;; *) ok "本公司范围看不到产品部 u_other" ;; esac
case "$NAMES_COMP" in *"'u_g2'"*) ok "本公司范围可见下级小组 u_g2(穿透 3 层)" ;; *) no "本公司范围应可见下级小组 u_g2" "含 u_g2" "不含" ;; esac

# 部门树也受数据权限约束：统计递归节点数（树以"总公司"为唯一根，故不能用根节点数量判断）
dept_node_count(){
  curl -s "$BASE/api/dept/tree" -H "Authorization: Bearer $1" | python -c "
import sys,json
d=json.load(sys.stdin)
def c(ns): return sum(1+c(n.get('children') or []) for n in ns)
print(c(d.get('data') or []))" 2>/dev/null
}
eq "本部门范围部门树节点数 = 3(本部门+祖先链)" "3" "$(dept_node_count "$T_DEPTON")"
eq "本公司范围部门树节点数 = 6(本公司子树+祖先)" "6" "$(dept_node_count "$T_COMP")"
eq "超级管理员部门树节点数 = 8(全量)" "8" "$(dept_node_count "$ADMIN")"

# ============================================================
section "3. 治理链路：敏感词 / 限额 / 举报闭环 / 禁言"
# ============================================================
curl -s -X POST "$BASE/api/th/auth/register" -H 'Content-Type: application/json' -d '{"username":"e2e_u1","password":"E2eTest@1234"}' > /dev/null
curl -s -X POST "$BASE/api/th/auth/register" -H 'Content-Type: application/json' -d '{"username":"e2e_u2","password":"E2eTest@1234"}' > /dev/null
TH1=$(send POST '/api/th/auth/login' '' '{"username":"e2e_u1","password":"E2eTest@1234"}' | J "d['data']['token']")
TH2=$(send POST '/api/th/auth/login' '' '{"username":"e2e_u2","password":"E2eTest@1234"}' | J "d['data']['token']")
neq "C 端用户1 登录成功" "" "$TH1"
neq "C 端用户2 登录成功" "" "$TH2"

# 3.1 先验证"先审后发"开关生效（显式打开，保证用例不依赖库初始值/缓存状态）
send PUT '/api/admin/th/settings' "$ADMIN" '{"post_need_audit":"1"}' > /dev/null
R=$(send POST '/api/th/post' "$TH1" '{"content":"审核开关测试帖","isAnonymous":0}')
AUDIT_POST=$(echo "$R" | J "d['data']")
eq "审核开启时发帖初始状态=0(待审)" "0" "$(sql "SELECT status FROM th_post WHERE id=$AUDIT_POST")"
send PUT '/api/admin/th/settings' "$ADMIN" '{"post_need_audit":"0"}' > /dev/null

# 3.2 敏感词
R=$(send POST '/api/th/post' "$TH1" '{"content":"这是一条正常的树洞内容","isAnonymous":0}')
POST_ID=$(echo "$R" | J "d['data']")
eq "正常内容发帖成功(200)" "200" "$(echo "$R" | J "d['code']")"
neq "返回新帖ID" "" "$POST_ID"
eq "关闭审核后发帖状态=1(已发布)" "1" "$(sql "SELECT status FROM th_post WHERE id=$POST_ID")"

R=$(send POST '/api/th/post' "$TH1" '{"content":"加微信 私聊我 有货源","isAnonymous":0}')
eq "敏感词 L1(加微信) 被拦截(400)" "400" "$(echo "$R" | J "d['code']")"
echo "       提示: $(echo "$R" | J "d['message']")"

# 3.3 每日限额（admin 把上限调到 2）
send PUT '/api/admin/th/settings' "$ADMIN" '{"max_post_per_day":"2"}' > /dev/null
send POST '/api/th/post' "$TH2" '{"content":"限额测试第1帖","isAnonymous":0}' > /dev/null
send POST '/api/th/post' "$TH2" '{"content":"限额测试第2帖","isAnonymous":0}' > /dev/null
R=$(send POST '/api/th/post' "$TH2" '{"content":"限额测试第3帖应被拦截","isAnonymous":0}')
eq "第3帖触发每日限额(1009)" "1009" "$(echo "$R" | J "d['code']")"
echo "       提示: $(echo "$R" | J "d['message']")"
send PUT '/api/admin/th/settings' "$ADMIN" '{"max_post_per_day":"10"}' > /dev/null

# 3.3 评论 / 点赞 / 收藏 / 搜索
R=$(send POST '/api/th/comment' "$TH2" "{\"postId\":$POST_ID,\"content\":\"用户2的评论\",\"isAnonymous\":0}")
COMMENT_ID=$(echo "$R" | J "d['data']")
neq "发表评论成功" "" "$COMMENT_ID"
eq "点赞帖子返回 200" "200" "$(send POST "/api/th/post/$POST_ID/like" "$TH2" | J "d['code']")"
eq "收藏帖子返回 true" "True" "$(send POST "/api/th/collect/$POST_ID" "$TH2" | J "bool(d['data'])")"
eq "收藏判定接口返回 true" "True" "$(curl -s "$BASE/api/th/post/$POST_ID/collected" -H "Authorization: Bearer $TH2" | J "bool(d['data'])")"
eq "我的收藏数量 = 1" "1" "$(curl -s "$BASE/api/th/user/collect-count" -H "Authorization: Bearer $TH2" | J "d['data']")"
COLLECT_LIST=$(curl -s "$BASE/api/th/user/collects?pageNum=1&pageSize=10" -H "Authorization: Bearer $TH2" | J "d['data']['total']")
eq "收藏列表可查到 1 条" "1" "$COLLECT_LIST"
neq "搜索命中发帖内容" "0" "$(curl -s "$BASE/api/th/search?keyword=%E6%AD%A3%E5%B8%B8%E7%9A%84%E6%A0%91%E6%B4%9E" | J "d['data']['total']")"

# 3.4 通知
eq "作者未读通知数 = 2(评论+点赞)" "2" "$(curl -s "$BASE/api/th/user/unread-count" -H "Authorization: Bearer $TH1" | J "d['data']")"
eq "通知列表可查到 2 条" "2" "$(curl -s "$BASE/api/th/user/notifications?pageNum=1&pageSize=10" -H "Authorization: Bearer $TH1" | J "d['data']['total']")"
NID=$(curl -s "$BASE/api/th/user/notifications?pageNum=1&pageSize=10" -H "Authorization: Bearer $TH1" | J "d['data']['records'][0]['id']")
eq "标记已读返回 200" "200" "$(send PUT '/api/th/user/notifications/read' "$TH1" "{\"ids\":[$NID]}" | J "d['code']")"
eq "标记后未读数 = 1" "1" "$(curl -s "$BASE/api/th/user/unread-count" -H "Authorization: Bearer $TH1" | J "d['data']")"

# 3.5 举报闭环
R=$(send POST '/api/th/report' "$TH2" "{\"targetType\":\"POST\",\"targetId\":$POST_ID,\"reason\":\"违规内容\",\"description\":\"E2E举报测试\"}")
eq "提交举报返回 200" "200" "$(echo "$R" | J "d['code']")"
R=$(send POST '/api/th/report' "$TH2" "{\"targetType\":\"POST\",\"targetId\":$POST_ID,\"reason\":\"重复举报\"}")
eq "重复举报被拒(400)" "400" "$(echo "$R" | J "d['code']")"

REPORT_ID=$(sql "SELECT id FROM th_report WHERE target_id=$POST_ID ORDER BY id DESC LIMIT 1")
eq "举报处理接口放行" "200" "$(send PUT "/api/admin/th/report/$REPORT_ID/handle?status=1&result=E2E%E5%88%A4%E5%AE%9A%E6%88%90%E7%AB%8B" "$ADMIN" | J "d['code']")"
case "$(curl -s "$BASE/api/th/user/notifications?pageNum=1&pageSize=10" -H "Authorization: Bearer $TH2" | J "[r['type'] for r in d['data']['records']]")" in
  *REPORT_RESULT*) ok "举报人收到 REPORT_RESULT 通知" ;;
  *) no "举报人收到 REPORT_RESULT 通知" "含 REPORT_RESULT" "无" ;;
esac
eq "举报成立后帖子下架(status=0)" "0" "$(sql "SELECT status FROM th_post WHERE id=$POST_ID")"
eq "作者违规分累计 = 2" "2" "$(sql "SELECT violation_count FROM th_user WHERE username='e2e_u1'")"

# 3.6 禁言
TH1_ID=$(sql "SELECT id FROM th_user WHERE username='e2e_u1'")
eq "admin 禁言用户1 成功" "200" "$(send PUT "/api/admin/th/user/$TH1_ID/mute?days=1&reason=E2E" "$ADMIN" | J "d['code']")"
eq "禁言时间已落库" "Y" "$(sql "SELECT IF(mute_until>NOW(),'Y','N') FROM th_user WHERE id=$TH1_ID")"
R=$(send POST '/api/th/post' "$TH1" '{"content":"被禁言用户发帖应被拒","isAnonymous":0}')
eq "禁言用户发帖 → 403" "403" "$(echo "$R" | J "d['code']")"
echo "       提示: $(echo "$R" | J "d['message']")"
R=$(send POST "/api/th/comment" "$TH1" "{\"postId\":$POST_ID,\"content\":\"禁言用户评论\",\"isAnonymous\":0}")
eq "禁言用户评论 → 403" "403" "$(echo "$R" | J "d['code']")"
eq "封禁处罚日志已留痕" "Y" "$(sql "SELECT IF(COUNT(*)>0,'Y','N') FROM th_user_log WHERE user_id=$TH1_ID AND action IN ('MUTE','VIOLATION')")"

# ============================================================
section "4. C 端自管理：删帖 / 删评论 / 他人主页 / 改密码"
# ============================================================
eq "删除他人评论 → 5002(无权操作)" "5002" "$(send DELETE "/api/th/comment/$COMMENT_ID" "$TH1" | J "d['code']")"
eq "删除自己的评论 → 200" "200" "$(send DELETE "/api/th/comment/$COMMENT_ID" "$TH2" | J "d['code']")"
eq "评论已逻辑删除" "1" "$(sql "SELECT deleted FROM th_comment WHERE id=$COMMENT_ID")"

R=$(send POST '/api/th/post' "$TH2" '{"content":"待删除的帖子","isAnonymous":0}')
DEL_POST=$(echo "$R" | J "d['data']")
eq "删除他人帖子 → 5002(无权操作)" "5002" "$(send DELETE "/api/th/post/$DEL_POST" "$TH1" | J "d['code']")"
eq "删除自己的帖子 → 200" "200" "$(send DELETE "/api/th/post/$DEL_POST" "$TH2" | J "d['code']")"
eq "级联删除点赞记录" "0" "$(sql "SELECT COUNT(*) FROM th_like WHERE target_type='POST' AND target_id=$DEL_POST AND deleted=0")"

TH2_ID=$(sql "SELECT id FROM th_user WHERE username='e2e_u2'")
neq "他人主页可访问(返回昵称)" "" "$(curl -s "$BASE/api/th/user/public/$TH2_ID" | J "d['data']['nickname']")"
eq "他人主页不泄露 email" "0" "$(curl -s "$BASE/api/th/user/public/$TH2_ID" | grep -c 'email')"

eq "原密码错误 → 1006" "1006" "$(send PUT '/api/th/auth/password' "$TH2" '{"oldPassword":"WrongPass@123","newPassword":"E2eTest@1234"}' | J "d['code']")"
eq "新密码强度不足 → 1008" "1008" "$(send PUT '/api/th/auth/password' "$TH2" '{"oldPassword":"E2eTest@1234","newPassword":"weak"}' | J "d['code']")"
eq "正常改密 → 200" "200" "$(send PUT '/api/th/auth/password' "$TH2" '{"oldPassword":"E2eTest@1234","newPassword":"NewE2e@1234"}' | J "d['code']")"
neq "改密后新密码可登录" "" "$(send POST '/api/th/auth/login' '' '{"username":"e2e_u2","password":"NewE2e@1234"}' | J "d['data']['token']")"
neq "改密后旧 Token 已失效(强制重登)" "200" "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/th/user/unread-count" -H "Authorization: Bearer $TH2")"

# ============================================================
section "5. 回归：基础链路 + 管理端可用性"
eq "帖子列表 200" "200" "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/th/post/page?pageNum=1&pageSize=10")"
eq "分类列表 200" "200" "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/th/category/list")"
eq "公告列表 200" "200" "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/th/announcements")"
eq "敏感词管理分页 200" "200" "$(send GET '/api/admin/th/sensitive-word/page?pageNum=1&pageSize=10' "$ADMIN" | J "d['code']")"
eq "敏感词词库统计可用" "200" "$(send GET '/api/admin/th/sensitive-word/stats' "$ADMIN" | J "d['code']")"
eq "新增敏感词 200" "200" "$(send POST '/api/admin/th/sensitive-word' "$ADMIN" '{"word":"e2e测试词","level":2,"category":"其他"}' | J "d['code']")"
eq "词库热刷新后词条数增长" "Y" "$(sql "SELECT IF(COUNT(*)>=12,'Y','N') FROM th_sensitive_word WHERE deleted=0")"
eq "管理端用户分页 200" "200" "$(send GET '/api/admin/th/user/page?pageNum=1&pageSize=10' "$ADMIN" | J "d['code']")"
eq "角色分页 200" "200" "$(send GET '/api/role/page?pageNum=1&pageSize=10' "$ADMIN" | J "d['code']")"
eq "角色详情带数据范围名称" "全部数据" "$(send GET '/api/role/1' "$ADMIN" | J "d['data']['dataScopeName']")"
eq "部门树带层级字段" "1" "$(send GET '/api/dept/tree' "$ADMIN" | J "d['data'][0]['deptLevel']")"

echo
echo "=================================================="
echo "  E2E 汇总:  PASS=$PASS  FAIL=$FAIL"
echo "=================================================="
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
