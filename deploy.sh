#!/bin/bash
# ============================================================
# 教学管理系统 — 快速部署脚本 v2
# 策略: docker cp + restart 替代 docker build (2min→5s)
# 用法: ./deploy.sh                  # 全量（并行）
#       ./deploy.sh backend          # 仅后端
#       ./deploy.sh frontend         # 仅前端
#       ./deploy.sh --hotfix all     # 实验期紧急修复
# ============================================================
set -e

# ── 实验期版本冻结 ──
FREEZE_START="2026-09-01"; FREEZE_END="2027-01-20"; HOTFIX=false
for arg in "$@"; do case "$arg" in --hotfix) HOTFIX=true ;; all|frontend|backend) TARGET="$arg" ;; esac; done
TARGET="${TARGET:-all}"
TODAY=$(date +%Y-%m-%d); IN_FREEZE=false
if [[ "$TODAY" > "$FREEZE_START" || "$TODAY" == "$FREEZE_START" ]]; then
  if [[ "$TODAY" < "$FREEZE_END" || "$TODAY" == "$FREEZE_END" ]]; then IN_FREEZE=true; fi
fi

SERVER="root@your-server-ip"; SVR="/home/ubuntu/teaching-system"; SSH_KEY="$HOME/.ssh/id_ed25519"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="backend/target/teaching-system-1.0.0.jar"
SKIP_REVIEW=false
for arg in "$@"; do case "$arg" in --skip-review) SKIP_REVIEW=true ;; esac; done

R='\033[0;31m'; G='\033[0;32m'; Y='\033[1;33m'; C='\033[0;36m'; N='\033[0m'
step()  { echo -e "\n${C}[$(date +%H:%M:%S)]${N} $1 ${2:+→ $2}"; }
ok()    { echo -e "  ${G}✓${N} $1"; }
warn()  { echo -e "  ${Y}⚠${N} $1"; }
die()   { echo -e "  ${R}✗ $1${N}"; exit 1; }

# ── Review 门禁 ──
check_review() {
  local review_file="$SCRIPT_DIR/.review"
  local changed=$(git diff --name-only HEAD 2>/dev/null | wc -l)
  local staged=$(git diff --cached --name-only 2>/dev/null | wc -l)
  local total=$((changed + staged))

  if [ "$total" -eq 0 ]; then
    ok "无改动文件，跳过 review 检查"
    return 0
  fi

  if [ ! -f "$review_file" ]; then
    echo -e "\n${R}╔══════════════════════════════════════════╗${N}"
    echo -e "${R}║  🛑 部署被阻止：缺少 review 记录${N}"
    echo -e "${R}║${N}"
    echo -e "${R}║  当前有 ${total} 个文件改动未经过 review${N}"
    echo -e "${R}║  请主控 Claude review 后生成 .review 文件${N}"
    echo -e "${R}║  紧急情况: ./deploy.sh --skip-review${N}"
    echo -e "${R}╚══════════════════════════════════════════╝${N}"
    git diff --stat HEAD 2>/dev/null | head -20
    if [ "$SKIP_REVIEW" != "true" ]; then exit 1; fi
    warn "已跳过 review（--skip-review）"
    return 0
  fi

  # 检查 review 时间是否晚于最新 commit
  local last_commit=$(git log -1 --format=%ct 2>/dev/null)
  local review_time=$(stat -c %Y "$review_file" 2>/dev/null || stat -f %m "$review_file" 2>/dev/null)
  if [ -n "$last_commit" ] && [ -n "$review_time" ] && [ "$review_time" -lt "$last_commit" ]; then
    echo -e "\n${Y}╔══════════════════════════════════════════╗${N}"
    echo -e "${Y}║  ⚠️  Review 记录已过期${N}"
    echo -e "${Y}║  最新 commit 在 review 之后，改动可能未经审查${N}"
    echo -e "${Y}║  建议重新 review 后更新 .review 文件${N}"
    echo -e "${Y}╚══════════════════════════════════════════╝${N}"
    if [ "$SKIP_REVIEW" != "true" ]; then
      warn "继续部署（review 过期但未阻止），5秒后继续..."
      sleep 5
    fi
  else
    local reviewer=$(head -1 "$review_file" 2>/dev/null | sed 's/^Reviewer: //')
    local verdict=$(grep "^Verdict:" "$review_file" 2>/dev/null | sed 's/^Verdict: //')
    ok "Review 通过: $reviewer · $verdict"
  fi
}

# ── UI 一致性门禁（棘轮 + 增量规则，R119）──
check_ui_gate() {
  if bash "$SCRIPT_DIR/tools/audit/ci-gate-ui.sh"; then
    ok "UI 一致性门禁通过"
  else
    if [ "$SKIP_REVIEW" = "true" ]; then
      warn "UI 一致性门禁未通过（--skip-review 显式跳过）"
    else
      die "UI 一致性门禁未通过（硬编码色棘轮/增量规则），修复后重试；紧急绕过: ./deploy.sh --skip-review"
    fi
  fi
}

# ── 每次部署后清理临时文件，但保留 .review ──
cleanup() { rm -f "$SCRIPT_DIR/frontend-dist.tar.gz"; }
trap cleanup EXIT

echo -e "${G}══════════════════════════════════════${N}"
echo -e "${G}  教学管理系统 · 快速部署 v2${N}"
echo -e "${G}  目标: $TARGET | $(date '+%H:%M:%S')${N}"

# ── 冻结检查 ──
if $IN_FREEZE && ! $HOTFIX; then
  echo -e "\n${R}╔══════════════════════════════════════╗${N}"
  echo -e "${R}║  🛑 实验期版本冻结 ($FREEZE_START~$FREEZE_END)${N}"
  echo -e "${R}║  仅允许: ./deploy.sh --hotfix all${N}"
  echo -e "${R}╚══════════════════════════════════════╝${N}"
  exit 1
fi

# ── 构建 ──
build_frontend() {
  step "前端构建" "vite build"
  cd "$SCRIPT_DIR/frontend"
  npm run build 2>&1 | tail -1 || die "前端构建失败"
  cd "$SCRIPT_DIR"
  ok "前端构建完成 ($(du -sh frontend/dist | cut -f1))"
}

build_backend() {
  step "后端构建" "mvn package -DskipTests"
  cd "$SCRIPT_DIR/backend"
  mvn package -DskipTests -q 2>&1 || die "后端构建失败"
  cd "$SCRIPT_DIR"
  ok "后端构建完成 ($(du -sh $JAR | cut -f1))"
}

# ── 确保容器 & 镜像就绪（首次自动构建，后续跳过）──
ensure_ready() {
  local svc=$1 img="teaching-system-$1"
  # 检查容器是否存在
  if ! ssh -i "$SSH_KEY" "$SERVER" "docker ps -a --format '{{.Names}}' | grep -qx 'teaching-$1'"; then
    warn "teaching-$1 容器不存在，执行首次 docker-compose up -d"
    ssh -i "$SSH_KEY" "$SERVER" "cd $SVR && docker-compose up -d $1" || die "$1 首次启动失败"
    ok "teaching-$1 容器已创建"
  fi
  # 检查镜像是否存在（Dockerfile 变更时需手动 docker-compose build）
  if ! ssh -i "$SSH_KEY" "$SERVER" "docker image inspect $img >/dev/null 2>&1"; then
    warn "$img 镜像不存在，构建中..."
    ssh -i "$SSH_KEY" "$SERVER" "cd $SVR && docker-compose build $1" || die "$1 镜像构建失败"
    ok "$img 镜像已创建"
  fi
}

# ── 部署后端：scp JAR → docker cp → restart ──
deploy_backend() {
  step "部署后端" "scp JAR → docker cp → restart"

  # 1. 上传 JAR
  scp -i "$SSH_KEY" "$SCRIPT_DIR/$JAR" "$SERVER:$SVR/$JAR" 2>&1 || die "JAR 传输失败"
  ok "JAR 已上传"

  # 2. 确保容器/镜像就绪
  ensure_ready "backend"

  # 3. docker cp + 清理 Spring Boot JarLauncher 旧缓存 + 清空历史日志 + restart
  ssh -i "$SSH_KEY" "$SERVER" "
    docker cp $SVR/$JAR teaching-backend:/app/app.jar && \
    docker exec teaching-backend sh -c 'cd /app && rm -rf BOOT-INF/classes && unzip -o app.jar 2>/dev/null | tail -1; echo classes_ok' && \
    truncate -s 0 \$(docker inspect --format '{{.LogPath}}' teaching-backend) 2>/dev/null; \
    docker restart teaching-backend
  " || die "后端热更新失败"
  ok "后端已重启（~30s 就绪）"
}

# ── 部署前端：scp dist.tar.gz → docker cp → nginx reload ──
deploy_frontend() {
  step "部署前端" "tar+scp → docker cp → nginx reload"

  # 1. 打包 dist + scp 上传
  cd "$SCRIPT_DIR"
  rm -f frontend-dist.tar.gz
  tar -czf frontend-dist.tar.gz -C frontend dist/ || die "dist 打包失败"
  scp -i "$SSH_KEY" frontend-dist.tar.gz "$SERVER:$SVR/" || die "前端传输失败"
  ok "dist 已上传"

  # 2. 确保容器/镜像就绪
  ensure_ready "frontend"

  # 3. 解压 + docker cp + reload（nginx reload 失败时 fallback 到 restart）
  ssh -i "$SSH_KEY" "$SERVER" "
    rm -rf $SVR/frontend/dist && \
    cd $SVR && tar -xzf frontend-dist.tar.gz -C frontend/ && \
    docker exec teaching-frontend rm -rf /usr/share/nginx/html/static /usr/share/nginx/html/index.html /usr/share/nginx/html/dist && \
    docker cp frontend/dist/. teaching-frontend:/usr/share/nginx/html/ && \
    truncate -s 0 \$(docker inspect --format '{{.LogPath}}' teaching-frontend) 2>/dev/null; \
    (docker exec teaching-frontend nginx -s reload 2>/dev/null || docker restart teaching-frontend)
  " || die "前端热更新失败"
  ok "前端已更新"
}

# ── 智能验证：5s间隔，最快10s出结果 ──
verify() {
  step "验证" "健康检查"
  for i in 1 2 3 4 5 6 7 8; do
    sleep 5
    local be=$(ssh -i "$SSH_KEY" "$SERVER" "curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/health" 2>/dev/null || echo "000")
    local fe=$(ssh -i "$SSH_KEY" "$SERVER" "curl -s -o /dev/null -w '%{http_code}' http://localhost" 2>/dev/null || echo "000")
    if [ "$be" = "200" ] && [ "$fe" = "200" ]; then
      ok "后端: $be  前端: $fe  (${i}x5=${i}5s)"
      return 0
    fi
    echo "  ⏳ 后端:$be 前端:$fe  等待中... ($((i*5))s)"
  done
  warn "验证超时 (40s)，请手动检查：ssh $SERVER 'docker ps && curl localhost:8080/api/health'"
}

# ── 主流程 ──
START=$(date +%s)

# 门禁：review 检查
check_review

# 门禁：UI 一致性（与 review 同级，绕过需 --skip-review）
check_ui_gate

case "$TARGET" in
  frontend)
    build_frontend
    deploy_frontend
    verify
    ;;
  backend)
    build_backend
    deploy_backend
    verify
    ;;
  all)
    # 阶段1: 本地并行构建
    step "阶段1/3" "并行构建"
    build_frontend & FPID=$!
    build_backend & BPID=$!
    wait $FPID && wait $BPID
    ok "构建完成"

    # 阶段2: 并行部署
    step "阶段2/3" "并行部署"
    deploy_backend & DPID=$!
    deploy_frontend & FPID2=$!
    wait $DPID && wait $FPID2
    ok "部署完成"

    # 阶段3: 验证
    step "阶段3/3" "验证"
    verify
    ;;
  *)
    echo "用法: ./deploy.sh [frontend|backend|all] [--hotfix]"
    exit 1
    ;;
esac

ELAPSED=$(($(date +%s) - START))
echo ""
echo -e "${G}══════════════════════════════════════${N}"
echo -e "${G}  ✅ 部署完成 · 耗时 ${ELAPSED}s${N}"
echo -e "${G}  https://your-domain.com${N}"
echo -e "${G}══════════════════════════════════════${N}"
