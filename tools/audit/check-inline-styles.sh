#!/bin/bash
# ============================================================
# check-inline-styles.sh
# 找出内联 style 属性中的颜色和间距
# ============================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="D:/TEACH/teaching-system/frontend/src"
cd "$FRONTEND_DIR" || exit 1

echo "═══════════════════════════════════════════════════════"
echo "  内联样式审计 — 找出 style 中的颜色/间距"
echo "═══════════════════════════════════════════════════════"
echo ""

TOTAL_INLINE_COLOR=$(grep -rn 'style="[^"]*color\s*:[^"]*"' views/ components/ --include="*.vue" 2>/dev/null | wc -l)
TOTAL_INLINE_SPACING=$(grep -rn 'style="[^"]*margin[^"]*"\|style="[^"]*padding[^"]*"' views/ components/ --include="*.vue" 2>/dev/null | wc -l)
TOTAL_INLINE_FLEX=$(grep -rn 'style="[^"]*display\s*:\s*flex[^"]*"' views/ components/ --include="*.vue" 2>/dev/null | wc -l)

echo "  内联颜色（应改为 CSS 变量）🚩: $TOTAL_INLINE_COLOR"
echo "  内联间距（建议使用 spacing 类）⚠️: $TOTAL_INLINE_SPACING"
echo "  内联 flex（建议使用 flex 类）⚠️: $TOTAL_INLINE_FLEX"
echo "  合计需优化: $((TOTAL_INLINE_COLOR + TOTAL_INLINE_SPACING + TOTAL_INLINE_FLEX))"
echo ""

echo "▶ 内联颜色明细（前 20）:"
grep -rn 'style="[^"]*color\s*:[^"]*"' views/ components/ --include="*.vue" 2>/dev/null | head -20

echo ""
echo "═══════════════════════════════════════════════════════"
