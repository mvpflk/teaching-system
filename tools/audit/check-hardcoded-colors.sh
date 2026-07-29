#!/bin/bash
# ============================================================
# check-hardcoded-colors.sh (optimized)
# 快速扫描 views/ + components/ 中的硬编码颜色
# 输出汇总 + Top 颜色频率
# ============================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib/colors.sh"

FRONTEND_DIR="D:/TEACH/teaching-system/frontend/src"
cd "$FRONTEND_DIR" || exit 1

# --count mode: only output total color count (for CI gate ratchet)
if [ "${1:-}" = "--count" ]; then
  TOTAL=$(grep -roh '#[[:xdigit:]][[:xdigit:]][[:xdigit:]][[:xdigit:]][[:xdigit:]][[:xdigit:]]' \
    views/ components/ --include="*.vue" 2>/dev/null | wc -l)
  echo "$TOTAL"
  exit 0
fi

echo "═══ 硬编码颜色审计 ═══"
echo ""

ALL_COLORS=$(grep -roh '#[[:xdigit:]][[:xdigit:]][[:xdigit:]][[:xdigit:]][[:xdigit:]][[:xdigit:]]' \
  views/ components/ --include="*.vue" 2>/dev/null | sort | uniq -c | sort -rn)

TOTAL_FILES=$(find views components -name "*.vue" -type f 2>/dev/null | wc -l)
TOTAL_COLORS=$(echo "$ALL_COLORS" | awk '{s+=$1} END {print s}')
UNIQUE_COLORS=$(echo "$ALL_COLORS" | wc -l)

echo "  扫描文件: $TOTAL_FILES  |  颜色引用: $TOTAL_COLORS  |  独特颜色: $UNIQUE_COLORS"
echo ""

MAPPED=0; EP=0; ORPHAN=0; AI_S=0; SKEL=0; UNKNOWN=0
while IFS= read -r line; do
  [ -z "$line" ] && continue
  count=$(echo "$line" | awk '{print $1}')
  color=$(echo "$line" | awk '{print $2}')
  normalized=$(normalize_hex "$color")
  result=$(classify_color "$normalized" 2>/dev/null)
  category="${result%%:*}"
  case "$category" in
    mapped) MAPPED=$((MAPPED + count));;
    ep_internal) EP=$((EP + count));;
    orphan) ORPHAN=$((ORPHAN + count));;
    ai) AI_S=$((AI_S + count));;
    skeleton) SKEL=$((SKEL + count));;
    *) UNKNOWN=$((UNKNOWN + count));;
  esac
done <<< "$ALL_COLORS"

echo "  已映射 ✅:     $MAPPED"
echo "  EP 内部 ⓔ:    $EP"
echo "  孤儿色 ⚠️:    $ORPHAN"
echo "  AI 模式 🚩:   $AI_S"
echo "  骨架屏 ◇:     $SKEL"
echo "  未知 ❓:      $UNKNOWN"
echo "  ─────────────────────"
echo "  需关注:       $((EP + ORPHAN + AI_S + SKEL + UNKNOWN))"
echo ""

echo "═══ Top 20 颜色 ═══"
echo "$ALL_COLORS" | head -20 | while read count color; do
  result=$(classify_color "$color" 2>/dev/null)
  echo "  $count × $color → $result"
done
