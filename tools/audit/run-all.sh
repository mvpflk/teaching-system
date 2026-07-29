#!/bin/bash
# ============================================================
# run-all.sh — UI 一致性审计主入口
# ============================================================
# 用法:
#   bash tools/audit/run-all.sh              # 运行全部审计并输出报告
#   bash tools/audit/run-all.sh --quick       # 仅运行关键检查（hardcoded + mobile）
#   bash tools/audit/run-all.sh --json        # 输出 JSON 格式
#   bash tools/audit/run-all.sh --help        # 帮助
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPORT_DIR="$SCRIPT_DIR/reports"
TIMESTAMP=$(date "+%Y%m%d_%H%M%S")
REPORT_FILE="$REPORT_DIR/ui-audit-$TIMESTAMP.md"
QUICK_MODE=false
JSON_MODE=false

for arg in "$@"; do
  case "$arg" in
    --quick) QUICK_MODE=true ;;
    --json) JSON_MODE=true ;;
    --help)
      echo "用法: bash tools/audit/run-all.sh [选项]"
      echo ""
      echo "选项:"
      echo "  --quick    仅运行关键检查 (hardcoded + mobile)"
      echo "  --json     同时输出 JSON 格式报告"
      echo "  --help     显示此帮助"
      exit 0
      ;;
  esac
done

mkdir -p "$REPORT_DIR"

echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║       教学管理系统 — UI 一致性审计                       ║"
echo "║       $(date "+%Y-%m-%d %H:%M:%S")                                   ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

# 捕获各脚本输出到临时文件
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# ── 1. 硬编码颜色 ────────────────────────────────────────
echo "┌─────────────────────────────────────────────────────────┐"
echo "│  检查 1/6: 硬编码颜色                                    │"
echo "└─────────────────────────────────────────────────────────┘"
bash "$SCRIPT_DIR/check-hardcoded-colors.sh" 2>&1 | tee "$TEMP_DIR/check-hardcoded.txt"
echo ""

if $QUICK_MODE; then
  echo "快速模式：跳过 AI 模式、渐变、内联样式检查"
else
  # ── 2. 孤儿色 ──────────────────────────────────────────
  echo "┌─────────────────────────────────────────────────────────┐"
  echo "│  检查 2/6: 孤儿色                                        │"
  echo "└─────────────────────────────────────────────────────────┘"
  bash "$SCRIPT_DIR/check-orphan-colors.sh" 2>&1 | tee "$TEMP_DIR/check-orphan.txt"
  echo ""

  # ── 3. AI 模式 ─────────────────────────────────────────
  echo "┌─────────────────────────────────────────────────────────┐"
  echo "│  检查 3/6: AI 生成模式                                   │"
  echo "└─────────────────────────────────────────────────────────┘"
  bash "$SCRIPT_DIR/check-ai-patterns.sh" 2>&1 | tee "$TEMP_DIR/check-ai.txt"
  echo ""

  # ── 4. 移动端 ──────────────────────────────────────────
  echo "┌─────────────────────────────────────────────────────────┐"
  echo "│  检查 4/6: 移动端适配                                    │"
  echo "└─────────────────────────────────────────────────────────┘"
  bash "$SCRIPT_DIR/check-mobile-gaps.sh" 2>&1 | tee "$TEMP_DIR/check-mobile.txt"
  echo ""

  # ── 5. 内联样式 ────────────────────────────────────────
  echo "┌─────────────────────────────────────────────────────────┐"
  echo "│  检查 5/6: 内联样式                                      │"
  echo "└─────────────────────────────────────────────────────────┘"
  bash "$SCRIPT_DIR/check-inline-styles.sh" 2>&1 | tee "$TEMP_DIR/check-inline.txt"
  echo ""

  # ── 6. 渐变 ────────────────────────────────────────────
  echo "┌─────────────────────────────────────────────────────────┐"
  echo "│  检查 6/6: 渐变使用                                      │"
  echo "└─────────────────────────────────────────────────────────┘"
  bash "$SCRIPT_DIR/check-gradient-legacy.sh" 2>&1 | tee "$TEMP_DIR/check-gradient.txt"
  echo ""
fi

# ── 生成汇总报告 ──────────────────────────────────────────
echo "┌─────────────────────────────────────────────────────────┐"
echo "│  生成报告...                                             │"
echo "└─────────────────────────────────────────────────────────┘"

# 从各检查输出提取关键数字
HARDCODED_COUNT=$(grep -oP '需关注总和（非 mapped）: \K\d+' "$TEMP_DIR/check-hardcoded.txt" 2>/dev/null || echo "?")
ORPHAN_COUNT=$(grep -oP '发现 \K\d+' "$TEMP_DIR/check-orphan.txt" 2>/dev/null || echo "?")
AI_COUNT=$(grep -oP '发现 \K\d+' "$TEMP_DIR/check-ai.txt" 2>/dev/null || echo "0")
MOBILE_GAP_COUNT=$(grep -oP '无移动端适配: \K\d+' "$TEMP_DIR/check-mobile.txt" 2>/dev/null || echo "?")
INLINE_COUNT=$(grep -oP '合计: *\K\d+' "$TEMP_DIR/check-inline.txt" 2>/dev/null || echo "?")
GRADIENT_ISSUE=$(grep -oP '紫色渐变.*\K\d+' "$TEMP_DIR/check-gradient.txt" 2>/dev/null || echo "?")

cat > "$REPORT_FILE" << EOF
# UI 一致性审计报告

**生成时间**: $(date "+%Y-%m-%d %H:%M:%S")
**运行模式**: $([ "$QUICK_MODE" = true ] && echo "快速" || echo "完整")

---

## 📊 审计结果摘要

| 检查项 | 发现数量 | 严重程度 |
|--------|---------|---------|
| 硬编码颜色（非 mapped） | $HARDCODED_COUNT | ⚠️ |
| 其中孤儿色（无映射） | $ORPHAN_COUNT | 🚩 |
| AI 模式残留 | $AI_COUNT | 🚩 |
| 无移动端适配的目录 | $MOBILE_GAP_COUNT | ⚠️ |
| 内联样式问题 | $INLINE_COUNT | ⚠️ |
| 紫色渐变 | $GRADIENT_ISSUE | 🚩 |

## 📋 详细报告

### 1. 硬编码颜色
详见 \`check-hardcoded-colors.sh\` 输出。

### 2. 孤儿色
详见 \`check-orphan-colors.sh\` 输出。

### 3. AI 模式
详见 \`check-ai-patterns.sh\` 输出。

### 4. 移动端适配
详见 \`check-mobile-gaps.sh\` 输出。

### 5. 内联样式
详见 \`check-inline-styles.sh\` 输出。

### 6. 渐变
详见 \`check-gradient-legacy.sh\` 输出。

---

## 📌 下一步建议

请参考映射规范文档确定优先级：
\`docs/superpowers/specs/ui-color-mapping.md\`

EOF

echo ""
echo "  报告已生成: $REPORT_FILE"
echo ""

# 输出到控制台
echo "╔══════════════════════════════════════════════════════════╗"
echo "║  审计完成                                               ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""
echo "  📄 完整报告: $REPORT_FILE"
echo "  📋 映射规范: docs/superpowers/specs/ui-color-mapping.md"
echo ""
