#!/bin/bash
# ============================================================
# generate-report.sh
# 汇总所有审计结果，输出可读报告 + JSON 数据
# 被 run-all.sh 调用
# ============================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib/colors.sh"

REPORT_DIR="$SCRIPT_DIR/reports"
TIMESTAMP=$(date "+%Y%m%d_%H%M%S")
REPORT_FILE="$REPORT_DIR/ui-audit-$TIMESTAMP.md"
JSON_FILE="$REPORT_DIR/ui-audit-$TIMESTAMP.json"

mkdir -p "$REPORT_DIR"

echo "═══════════════════════════════════════════════════════"
echo "  生成审计报告..."
echo "═══════════════════════════════════════════════════════"

# 收集各检查脚本的输出
# 实际使用 run-all.sh 时，各脚本的输出会传递给此脚本
# 这里定义报告模板

cat > "$REPORT_FILE" << EOF
# UI 一致性审计报告

**生成时间**: $(date "+%Y-%m-%d %H:%M:%S")
**项目**: 教学管理系统
**依据**: DESIGN.md / theme.css / index.scss

---

## 1. 硬编码颜色审计

待填充 — 运行 \`check-hardcoded-colors.sh\`

## 2. 孤儿色审计

待填充 — 运行 \`check-orphan-colors.sh\`

## 3. AI 模式审计

待填充 — 运行 \`check-ai-patterns.sh\`

## 4. 移动端适配审计

待填充 — 运行 \`check-mobile-gaps.sh\`

## 5. 内联样式审计

待填充 — 运行 \`check-inline-styles.sh\`

## 6. 渐变审计

待填充 — 运行 \`check-gradient-legacy.sh\`

---

## 总体评估

| 维度 | 状态 | 优先级 |
|------|------|--------|
| 硬编码颜色 | - | - |
| AI 模式残留 | - | - |
| 移动端适配 | - | - |
| 内联样式 | - | - |
| 渐变规范 | - | - |

## 建议行动

1. ...
2. ...
3. ...
EOF

echo ""
echo "  报告模板已生成: $REPORT_FILE"
echo "  运行 run-all.sh 获取完整报告"
echo ""
