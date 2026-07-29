#!/bin/bash
# ============================================================
# check-gradient-legacy.sh
# 分类报告所有渐变的使用情况
# ============================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="D:/TEACH/teaching-system/frontend/src"
cd "$FRONTEND_DIR" || exit 1

echo "═══════════════════════════════════════════════════════"
echo "  渐变审计 — 分类报告所有渐变使用"
echo "═══════════════════════════════════════════════════════"
echo ""

TOTAL=$(grep -rn 'linear-gradient\|radial-gradient' views/ components/ --include="*.vue" 2>/dev/null | wc -l)
PURPLE=$(grep -rn 'linear-gradient\|radial-gradient' views/ components/ --include="*.vue" 2>/dev/null | grep -ciE '7c3aed|6c5ce7|7209b7|6366f1|8b5cf6|6d28d9')
CYAN=$(grep -rn 'linear-gradient\|radial-gradient' views/ components/ --include="*.vue" 2>/dev/null | grep -ci '06b6d4')

echo "  总渐变数:       $TOTAL"
echo "  紫色渐变 🚩:    $PURPLE（需改为 primary→primary-dark）"
echo "  青色渐变 ⚠️:    $CYAN（需改为 primary→primary-dark）"
echo ""

echo "▶ 紫色渐变明细:"
grep -rn 'linear-gradient\|radial-gradient' views/ components/ --include="*.vue" 2>/dev/null | grep -iE '7c3aed|6c5ce7|7209b7|6366f1|8b5cf6|6d28d9' | head -10

echo ""
echo "  处理建议:"
echo "  紫色渐变 → linear-gradient(135deg, var(--primary-color), var(--primary-dark))"
echo "  骨架屏渐变 → 统一 .skeleton-shimmer 类"
echo "  卡片渐变 → 纯色 --bg-card"
echo ""
echo "═══════════════════════════════════════════════════════"
