#!/bin/bash
# ============================================================
# check-orphan-colors.sh
# 专门报告「无 CSS 变量匹配」的孤儿色
# ============================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib/colors.sh"

FRONTEND_DIR="D:/TEACH/teaching-system/frontend/src"
cd "$FRONTEND_DIR" || exit 1

echo "═══════════════════════════════════════════════════════"
echo "  孤儿色审计 — 无 CSS 变量匹配的颜色"
echo "═══════════════════════════════════════════════════════"
echo ""

declare -A ORPHAN_FREQ
declare -A ORPHAN_LOCATIONS

VUE_FILES=()
for dir in views components; do
  while IFS= read -r -d '' file; do
    VUE_FILES+=("$file")
  done < <(find "$dir" -name "*.vue" -type f -print0 2>/dev/null)
done

for file in "${VUE_FILES[@]}"; do
  hex_colors=$(grep -on '#[[:xdigit:]][[:xdigit:]][[:xdigit:]][[:xdigit:]][[:xdigit:]][[:xdigit:]]' "$file" 2>/dev/null)
  if [ -z "$hex_colors" ]; then continue; fi

  while IFS= read -r line; do
    line_num=$(echo "$line" | cut -d: -f1)
    color=$(echo "$line" | grep -o '#[[:xdigit:]][[:xdigit:]][[:xdigit:]][[:xdigit:]][[:xdigit:]][[:xdigit:]]')
    normalized=$(normalize_hex "$color")
    result=$(classify_color "$normalized" 2>/dev/null)
    category="${result%%:*}"

    if [ "$category" = "orphan" ] || [ "$category" = "unknown" ]; then
      ORPHAN_FREQ[$normalized]=$((ORPHAN_FREQ[$normalized] + 1))
      if [[ -n "${ORPHAN_LOCATIONS[$normalized]}" ]]; then
        ORPHAN_LOCATIONS[$normalized]="${ORPHAN_LOCATIONS[$normalized]}, $file:$line_num"
      else
        ORPHAN_LOCATIONS[$normalized]="$file:$line_num"
      fi
    fi
  done <<< "$hex_colors"
done

if [ ${#ORPHAN_FREQ[@]} -eq 0 ]; then
  echo "  ✅ 没有孤儿色！"
  exit 0
fi

echo "  发现 ${#ORPHAN_FREQ[@]} 种孤儿色"
echo ""
echo "  颜色       次数  建议映射"
echo "  ───────────────────────────────────"

for entry in $(for c in "${!ORPHAN_FREQ[@]}"; do echo "${ORPHAN_FREQ[$c]} $c"; done | sort -rn); do
  count=$(echo "$entry" | cut -d' ' -f1)
  color=$(echo "$entry" | cut -d' ' -f2)
  suggestion="${ORPHAN_COLORS[$color]:-需人工确认}"
  printf "  %-8s %3s  %s\n" "$color" "$count" "$suggestion"
done

echo ""
echo "═══════════════════════════════════════════════════════"
echo "  提示：所有孤儿色的处理建议见"
echo "  docs/superpowers/specs/ui-color-mapping.md  §第二部分"
echo "═══════════════════════════════════════════════════════"
