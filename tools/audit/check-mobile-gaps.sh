#!/bin/bash
# ============================================================
# check-mobile-gaps.sh
# 报告各个视图目录中移动端适配的覆盖情况
# ============================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

FRONTEND_DIR="D:/TEACH/teaching-system/frontend/src"
cd "$FRONTEND_DIR/views" || exit 1

echo "═══════════════════════════════════════════════════════"
echo "  移动端适配覆盖率审计"
echo "═══════════════════════════════════════════════════════"
echo ""

printf "%-24s %5s %8s %8s\n" "目录" "文件数" "isMobile" "@media"
echo "──────────────────────────────────────────"

TOTAL_FILES=0
GAP_COUNT=0
declare -a GAP_DIRS=()

for d in */; do
  dir=$(basename "$d")
  files=$(find "$d" -name "*.vue" -type f 2>/dev/null | wc -l)
  im=$(grep -rl "isMobile" "$d" --include="*.vue" 2>/dev/null | wc -l)
  mq=$(grep -rl "@media" "$d" --include="*.vue" 2>/dev/null | wc -l)

  TOTAL_FILES=$((TOTAL_FILES + files))

  if [ "$im" -gt 0 ] || [ "$mq" -gt 0 ]; then
    status="✅"
  else
    status="🚩"
    GAP_COUNT=$((GAP_COUNT + 1))
    GAP_DIRS+=("$dir")
  fi

  printf "%-24s %5s  %5s/%-5s %5s   %s\n" "$dir" "$files" "$im" "$files" "$mq" "$status"
done

echo ""
echo "═══════════════════════════════════════════════════════"
echo "  汇总"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "  总文件数:       $TOTAL_FILES"
echo "  有移动端适配:   $(( ${#GAP_DIRS[@]} )) 个目录"
echo "  无移动端适配:   $GAP_COUNT 个目录"
echo ""

if [ ${#GAP_DIRS[@]} -gt 0 ]; then
  echo "  需批量适配的目录:"
  for d in "${GAP_DIRS[@]}"; do
    cnt=$(find "$d" -name "*.vue" -type f 2>/dev/null | wc -l)
    echo "    - $d/ ($cnt 个文件)"
  done
  echo ""
  echo "  整改优先级建议:"
  echo "    P0: student/（学生高频使用移动端）"
  echo "    P0: home/、profile/、notification/"
  echo "    P1: task/、bbs/、knowledge/"
  echo "    P2: inspector/、system/、credit/（管理后台桌面优先）"
fi

echo ""
echo "═══════════════════════════════════════════════════════"
