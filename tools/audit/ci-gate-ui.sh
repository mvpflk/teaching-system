#!/bin/bash
# tools/audit/ci-gate-ui.sh —— UI 一致性门禁（v3:棘轮 + 增量规则）
# Usage: called from pre-commit hook or deploy.sh
set -euo pipefail
TOP="$(git rev-parse --show-toplevel)"

# ---------- 规则 1:硬编码色棘轮（只许降不许升） ----------
BASELINE_FILE="$TOP/tools/audit/color-ratchet.txt"
if [ ! -f "$BASELINE_FILE" ]; then
  echo "❌ 基线文件不存在: $BASELINE_FILE"
  echo "   请先运行: bash $TOP/tools/audit/check-hardcoded-colors.sh --count > $BASELINE_FILE"
  exit 1
fi
CURRENT=$(bash "$TOP/tools/audit/check-hardcoded-colors.sh" --count)
BASELINE=$(cat "$BASELINE_FILE")
if [ "$CURRENT" -gt "$BASELINE" ]; then
  echo "❌ 硬编码色计数上升:$BASELINE → $CURRENT（棘轮只许降,请把新增色改为 var(--*)）"
  exit 1
elif [ "$CURRENT" -lt "$BASELINE" ]; then
  echo "🎉 计数下降 $BASELINE → $CURRENT,请同步更新 color-ratchet.txt 锁定成果"
fi

# ---------- 增量规则:仅当本次提交含前端文件(.vue/.js/.ts)时检查 ----------
if git diff --cached --name-only -- '*.vue' '*.js' '*.ts' | grep -q . 2>/dev/null; then
  ADDED=$(git diff --cached -U0 -- '*.vue' '*.js' '*.ts' | grep -E '^\+[^+]' || true)

  # ---------- 规则 2:禁止新增 Vant 全量注册 ----------
  if echo "$ADDED" | grep -qE "app\.use\(Vant\)|import Vant from 'vant'|vant/lib/index\.css"; then
    echo "❌ 新增 Vant 全量注册/全量 CSS,请用 @vant/auto-import-resolver 按需引入"
    exit 1
  fi

  # ---------- 规则 3:禁止新增裸写 EP 默认语义色 ----------
  # var(--el-color-*, #67c23a) 回退写法合法,按行排除
  if echo "$ADDED" | grep -E "#67c23a|#e6a23c|#f56c6c" | grep -vE "var\(--el-color-(success|warning|danger)"; then
    echo "❌ 新增裸写 Element Plus 默认语义色,请改用 var(--el-color-*)"
    exit 1
  fi
fi

echo "✅ UI 一致性门禁通过（当前计数 $CURRENT / 基线 $BASELINE）"
