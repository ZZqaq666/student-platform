#!/bin/bash
# ScholarPal 全量接口联调（v2：修正传参、路径与编码）
# 测试账号从环境变量读取，不要把口令写进仓库（脚本可能被推到公开仓库）。
# 本地跑法：SMOKE_USER=xxx SMOKE_PASS=yyy bash scripts/api-smoke.sh
: "${SMOKE_USER:?请通过环境变量 SMOKE_USER 传入测试账号}"
: "${SMOKE_PASS:?请通过环境变量 SMOKE_PASS 传入测试账号口令}"
B=http://localhost:8080/api
PASS=0; FAIL=0; NOTE=0

say() { printf "%-50s %s\n" "$1" "$2"; }
chk() { if [ "$2" = "$1" ]; then say "$3" "OK   HTTP=$2"; PASS=$((PASS+1));
        else say "$3" "FAIL HTTP=$2 (期望 $1) $4"; FAIL=$((FAIL+1)); fi; }
note() { say "$2" "注  HTTP=$1 $3"; NOTE=$((NOTE+1)); }

# GET，返回状态码
g() { curl -s -o /tmp/o.json -w '%{http_code}' --max-time ${2:-20} -H "Authorization: Bearer $AT" "$B$1"; }
# 带 JSON body 的方法调用：$1=path $2=method $3=body(inline) $4=bodyfile
m() {
  local extra=()
  if [ -n "$3" ]; then extra=(-d "$3"); elif [ -n "$4" ]; then extra=(--data-binary "@$4"); fi
  curl -s -o /tmp/o.json -w '%{http_code}' -X "$2" --max-time 25 \
    -H "Authorization: Bearer $AT" -H 'Content-Type: application/json; charset=utf-8' \
    "${extra[@]}" "$B$1"
}

echo "=========== 1) 认证链路 ==========="
L=$(curl -s -X POST $B/auth/login -H 'Content-Type: application/json' \
  -d "{\"username\":\"$SMOKE_USER\",\"password\":\"$SMOKE_PASS\"}")
AT=$(echo $L|grep -oE '"token":"[^"]+'|cut -d'"' -f4)
RT=$(echo $L|grep -oE '"refreshToken":"[^"]+'|cut -d'"' -f4)
[ -z "$AT" ] && { say "登录" "FAIL 无法取得令牌，终止"; exit 1; }
say "POST /auth/login" "OK   access+refresh 均签发"
chk 200 "$(g /auth/validate)" "GET  /auth/validate"
chk 401 "$(curl -s -o /dev/null -w '%{http_code}' $B/bookshelf/books)" "无令牌访问业务接口"
chk 401 "$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $RT" $B/bookshelf/books)" "refresh 冒充 access"

echo "=========== 2) 书架 / 书籍 ==========="
chk 200 "$(g /bookshelf/books)" "GET  /bookshelf/books"
chk 200 "$(g /bookshelf/my)" "GET  /bookshelf/my"
chk 200 "$(g '/bookshelf/books/search?keyword=calc')" "GET  /bookshelf/books/search"
chk 200 "$(g /books/recent)" "GET  /books/recent"
chk 200 "$(g /books/1)" "GET  /books/{id}"
chk 200 "$(g /books/1/chapters/tree)" "GET  /books/{id}/chapters/tree"
chk 200 "$(g /books/1/high-frequency)" "GET  /books/{id}/high-frequency"
chk 200 "$(g /knowledge-graph/book/1)" "GET  /knowledge-graph/book/{bookId}"
chk 200 "$(g /knowledge-graph/book/1/tree)" "GET  /knowledge-graph 树"

echo "=========== 3) 学习笔记（含中文 UTF-8） ==========="
printf '{"title":"联调笔记-中文校验","content":"极限与导数的关系","bookId":1}' > /tmp/note.json
c=$(m /notes POST "" /tmp/note.json); chk 200 "$c" "POST /notes（中文正文）"
NID=$(grep -oE '"id":[0-9]+' /tmp/o.json | head -1 | cut -d: -f2)
chk 200 "$(g "/notes/book/1")" "GET  /notes/book/{id}"
if [ -n "$NID" ]; then
  printf '{"title":"联调笔记-已改名","content":"更新后的中文正文"}' > /tmp/note2.json
  chk 200 "$(m "/notes/$NID" PUT "" /tmp/note2.json)" "PUT  /notes/{id}（改名）"
  chk 200 "$(m "/notes/$NID" DELETE)" "DELETE /notes/{id}"
else
  say "PUT/DELETE /notes" "FAIL 建笔记未返回 id"
  FAIL=$((FAIL+1))
fi

echo "=========== 4) 练习 / 错题本 ==========="
chk 200 "$(g /exercise/book/1)" "GET  /exercise/book/{bookId}"
chk 200 "$(g /exercise/stats)" "GET  /exercise/stats"
chk 200 "$(g /exercise/wrong-book)" "GET  /exercise/wrong-book"
chk 200 "$(m /exercise/wrong-book/filter POST '{"pageNum":1,"pageSize":10}')" "POST 错题筛选分页"
chk 200 "$(g '/exercise/wrong-book?sortField=created_at&sortDirection=desc')" "GET  错题排序白名单"
# pageSize 超限的实现是静默钳制到 1000（P0 设计），不是拒绝，所以断言 200
chk 200 "$(m /exercise/wrong-book/filter POST '{"pageSize":99999}')" "POST 超限 pageSize 被钳制而非 500"
chk 200 "$(g '/exercise/wrong-book?sortField=id%3Bdrop%20table&sortDirection=asc')" "GET  注入式排序列被拒（不报 500）"

echo "=========== 5) 问答 / 会话 / 学习 ==========="
chk 200 "$(g '/qa/history?limit=5')" "GET  /qa/history"
chk 200 "$(g /qa/history/sessions)" "GET  /qa/history/sessions"
chk 200 "$(g /qa/exam/subjects)" "GET  /qa/exam/subjects"
chk 200 "$(g /conversation/list)" "GET  /conversation/list"
chk 200 "$(g '/learning/curve?bookId=1')" "GET  /learning/curve"
chk 200 "$(g '/learning/history')" "GET  /learning/history"
# book_id 有外键指向 book.id，必须用库里真实存在的 id，否则触发约束冲突
REAL_BOOK=$(g /bookshelf/books >/dev/null; grep -oE '"id":[0-9]+' /tmp/o.json | head -1 | cut -d: -f2)
chk 200 "$(m /learning/progress POST "{\"bookId\":${REAL_BOOK:-1},\"progress\":10,\"pagesRead\":5,\"studyTimeMinutes\":10}")" "POST /learning/progress (bookId=$REAL_BOOK)"

echo "=========== 6) 学长答疑 ==========="
chk 200 "$(g '/senior/questions?page=1&pageSize=5')" "GET  /senior/questions 分页"
chk 200 "$(g /senior/seniors)" "GET  学长列表"

echo "=========== 7) SSRF 防护 ==========="
chk 403 "$(curl -s -o /dev/null -w '%{http_code}' --max-time 12 -H "Authorization: Bearer $AT" "$B/image-proxy/proxy?url=http://169.254.169.254/latest/meta-data/")" "云元数据被拒"
chk 403 "$(curl -s -o /dev/null -w '%{http_code}' --max-time 12 -H "Authorization: Bearer $AT" "$B/image-proxy/proxy?url=http://127.0.0.1:6379/")" "内网 Redis 被拒"
chk 403 "$(curl -s -o /dev/null -w '%{http_code}' --max-time 12 -H "Authorization: Bearer $AT" "$B/image-proxy/proxy?url=file:///etc/passwd")" "file 协议被拒"

echo "=========== 8) 未匹配路径应 404 不该 500 ==========="
chk 404 "$(g /definitely-not-an-endpoint)" "GET  不存在的路径"

echo "=========== 9) AI 真实回答（异步非流式） ==========="
printf '{"question":"\xe7\x94\xa8\xe4\xb8\x80\xe5\x8f\xa5\xe8\xaf\x9d\xe8\xaf\xb4\xe6\x98\x8e\xe4\xbb\x80\xe4\xb9\x88\xe6\x98\xaf\xe5\xaf\xbc\xe6\x95\xb0","subject":"\xe6\x95\xb0\xe5\xad\xa6","sessionId":"probe-v2"}' > /tmp/ask.json
t0=$(date +%s)
code=$(m /qa/ask POST "" /tmp/ask.json); COST=$(( $(date +%s)-t0 ))
if [ "$code" = "200" ] && grep -q '"answer"' /tmp/o.json && ! grep -q "模拟的 AI" /tmp/o.json; then
  say "POST /qa/ask 异步" "OK   ${COST}s 真实回答：$(grep -oE '"answer":"[^"]\{0,34\}' /tmp/o.json | cut -c10-)…"
  PASS=$((PASS+1))
else
  say "POST /qa/ask 异步" "FAIL ${COST}s HTTP=$code $(head -c 100 /tmp/o.json)"; FAIL=$((FAIL+1))
fi

echo "=========== 10) SSE：正常流 + 错误可见性 ==========="
printf '{"question":"\xe4\xbb\x80\xe4\xb9\x88\xe6\x98\xaf\xe6\x8a\x9a\xe5\x8f\x91","subject":"\xe6\x95\xb0\xe5\xad\xa6"}' > /tmp/askq.json
SSE=$(curl -sN -X POST $B/qa/ask/stream -H "Authorization: Bearer $AT" -H 'Content-Type: application/json; charset=utf-8' -H 'Accept: text/event-stream' --data-binary @/tmp/askq.json --max-time 60 | head -c 400)
if echo "$SSE" | grep -q '^data:'; then
  say "POST /qa/ask/stream 正常流" "OK   收到 SSE data 帧"; PASS=$((PASS+1))
else
  say "POST /qa/ask/stream 正常流" "FAIL 无 data 帧：$(echo "$SSE"|head -c 90)"; FAIL=$((FAIL+1))
fi
sc=$(curl -s -o /tmp/e.txt -w '%{http_code}' -X POST $B/qa/ask/stream -H "Authorization: Bearer $AT" -H 'Content-Type: application/json' -H 'Accept: text/event-stream' -d '{"bad":"field"}' --max-time 20)
note "$sc" "SSE 请求体解析失败" "HTTP=$sc，body=$(wc -c </tmp/e.txt)B（Accept 严格为 event-stream 时 JSON 错误体无法协商，前端靠 !response.ok 兜底）"

echo
echo "=========== 汇总 ==========="
echo "通过 $PASS   失败 $FAIL   备注 $NOTE"
