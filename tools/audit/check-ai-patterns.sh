#!/bin/bash
# ============================================================
# check-ai-patterns.sh
# 检测 AI 生成模式的视觉残留：
#   - 紫色渐变
#   - AI 色直接使用
#   - "欢迎回来"类 AI 鸡汤
#   - chart 色盘中的紫色/粉色
# ============================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib/colors.sh"

FRONTEND_DIR="D:/TEACH/teaching-system/frontend/src"
cd "$FRONTEND_DIR" || exit 1

echo "═══════════════════════════════════════════════════════"
echo "  AI 模式审计 — 检测 AI 生成视觉残留"
echo "═══════════════════════════════════════════════════════"
echo ""

AI_FOUND=0

# ── 1. 紫色渐变 ───────────────────────────────────────────
echo "▶ 1. 紫色渐变（主色→紫色/青色）"
echo ""

while IFS= read -r -d '' file; do
  rel="$file"
  # 检测渐变色中包含紫色系颜色
  if grep -q 'linear-gradient\|radial-gradient' "$file" 2>/dev/null; then
    grad_lines=$(grep -n 'linear-gradient\|radial-gradient' "$file" 2>/dev/null)
    while IFS= read -r line; do
      if echo "$line" | grep -qiE '7c3aed|6c5ce7|7209b7|6366f1|8b5cf6|6d28d9|06b6d4|f72585'; then
        echo "  🚩 $rel:$line"
        AI_FOUND=$((AI_FOUND + 1))
      fi
    done <<< "$grad_lines"
  fi
done < <(find views components -name "*.vue" -type f -print0 2>/dev/null)

echo ""

# ── 2. 紫色/AI 色直接使用 ─────────────────────────────────
echo "▶ 2. 紫色/AI 色直接使用"
echo ""

for color in "${!AI_COLORS[@]}"; do
  while IFS= read -r -d '' file; do
    rel="$file"
    count=$(grep -c "$color" "$file" 2>/dev/null || true)
    if [ "$count" -gt 0 ]; then
      echo "  🚩 $rel → 使用了 $color (${AI_COLORS[$color]})"
      AI_FOUND=$((AI_FOUND + count))
    fi
  done < <(find views components -name "*.vue" -type f -print0 2>/dev/null)
done

echo ""

# ── 3. AI 鸡汤文案 ──────────────────────────────────────
echo "▶ 3. AI 鸡汤文案"
echo ""

AI_PHRASES=("好心情" "欢迎回来" "今天也有" "加油哦" "棒棒哒" "太棒了" "继续保持")
for phrase in "${AI_PHRASES[@]}"; do
  while IFS= read -r -d '' file; do
    rel="$file"
    if grep -q "$phrase" "$file" 2>/dev/null; then
      echo "  🚩 $rel → 包含 AI 文案「$phrase」"
      AI_FOUND=$((AI_FOUND + 1))
    fi
  done < <(find views components -name "*.vue" -type f -print0 2>/dev/null)
done

echo ""

# ── 汇总 ──────────────────────────────────────────────────
echo "═══════════════════════════════════════════════════════"
if [ "$AI_FOUND" -eq 0 ]; then
  echo "  ✅ 未发现 AI 模式残留"
else
  echo "  🚩 发现 $AI_FOUND 处 AI 模式残留，需清理"
fi
echo "═══════════════════════════════════════════════════════"
