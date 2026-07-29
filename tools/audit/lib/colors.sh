#!/bin/bash
# ============================================================
# 共享颜色映射库 — 被各审计脚本 source
# 定义 CSS 变量映射、颜色分类、规范化函数
# ============================================================

# ─── 精确映射表（确定性替换） ───────────────────────────────
# 格式：HEX|CSS_VARIABLE
declare -A COLOR_MAP
COLOR_MAP=(
  ["#4361ee"]="--primary-color"
  ["#3651d4"]="--primary-dark"
  ["#eef0ff"]="--primary-light"
  ["#1d1d1f"]="--text-primary"
  ["#3a3a3c"]="--text-regular"
  ["#86868b"]="--text-secondary"
  ["#bcbcc0"]="--text-disabled"
  ["#f5f5f7"]="--bg-page"
  ["#ffffff"]="--bg-card"
  ["#f0f0f2"]="--bg-secondary"
  ["#fafafa"]="--bg-hover"
  ["#2e7d32"]="--el-color-success"
  ["#ed6c02"]="--el-color-warning"
  ["#d32f2f"]="--el-color-danger"
  ["#e8f5e9"]="--bg-success-light"
  ["#ffebee"]="--bg-danger-light"
  ["#fff3e0"]="--bg-warning-light"
  ["#c8e6c9"]="--border-success-light"
  ["#ffcdd2"]="--border-danger-light"
  ["#667eea"]="--primary-gradient（次要色）"
)

# ─── Element Plus 内部色（不动源码） ─────────────────────────
declare -A EP_INTERNAL_COLORS
EP_INTERNAL_COLORS=(
  ["#67c23a"]="Element Plus 默认成功色，全局已覆写"
  ["#f56c6c"]="Element Plus 默认危险色，全局已覆写"
  ["#e6a23c"]="Element Plus 默认警告色，全局已覆写"
  ["#909399"]="Element Plus text-placeholder"
  ["#f5f7fa"]="Element Plus fill-light"
  ["#ebeef5"]="Element Plus fill"
  ["#303133"]="Element Plus text-primary"
  ["#606266"]="Element Plus text-regular"
  ["#e4e7ed"]="Element Plus border-light"
  ["#dcdfe6"]="Element Plus border-base"
  ["#c0c4cc"]="Element Plus border-lighter"
)

# ─── 需替换的孤儿色 ────────────────────────────────────────
declare -A ORPHAN_COLORS
ORPHAN_COLORS=(
  ["#409eff"]="--primary-color | Element 默认蓝→品牌蓝"
  ["#00b42a"]="--el-color-success | Arco Design 遗留绿"
  ["#f39c12"]="--el-color-warning | 标签色统一"
  ["#e74c3c"]="--el-color-danger | 错误态统一"
  ["#e8f0fe"]="--primary-light | 蓝色高亮统一（色号微调）"
  ["#fef0f0"]="--bg-danger-light | 红色高亮统一（色号微调）"
  ["#f6ffed"]="--bg-success-light | 绿色高亮统一（色号微调）"
  ["#f59e0b"]="--el-color-warning | 琥珀色统一"
  ["#bbb"]="--border-color | 分隔符"
  ["#888"]="--text-secondary | 辅助文字"
)

# ─── 紫色/AI 色（需清理） ──────────────────────────────────
declare -A AI_COLORS
AI_COLORS=(
  ["#7209b7"]="--accent-color | AI 紫色残留→收敛至强调色"
  ["#7c3aed"]="--accent-color | AI 紫色渐变残留→收敛至强调色"
  ["#6c5ce7"]="--accent-color | AI 紫色残留→收敛至强调色"
  ["#6366f1"]="--accent-color | AI 紫色残留→收敛至强调色"
  ["#8b5cf6"]="--accent-color | AI 紫色残留→收敛至强调色"
  ["#b37feb"]="chart 色盘需规范化"
  ["#f72585"]="chart 色盘需规范化"
  ["#6d28d9"]="--accent-color | AI 紫色残留→收敛至强调色"
)

# ─── 骨架屏色（需统一） ────────────────────────────────────
declare -A SKELETON_COLORS
SKELETON_COLORS=(
  ["#e8e8ed"]="--skeleton-bg"
  ["#e0e0e0"]="--skeleton-bg"
  ["#f0f0f0"]="--skeleton-highlight"
  ["#f5f5f5"]="--skeleton-highlight"
)

# ─── 函数：规范化十六进制颜色（统一为小写 6 位） ────────────
normalize_hex() {
  local color="$1"
  # 去掉 # 号
  color="${color#\#}"
  # 转小写
  color="$(echo "$color" | tr '[:upper:]' '[:lower:]')"
  # 3 位 → 6 位
  if [ ${#color} -eq 3 ]; then
    local r="${color:0:1}${color:0:1}"
    local g="${color:1:1}${color:1:1}"
    local b="${color:2:1}${color:2:1}"
    color="${r}${g}${b}"
  fi
  echo "#$color"
}

# ─── 函数：查询颜色映射分类 ────────────────────────────────
# 返回值：mapped | ep_internal | orphan | ai | skeleton | unknown
classify_color() {
  local hex="$(normalize_hex "$1")"

  if [[ -n "${COLOR_MAP[$hex]}" ]]; then
    echo "mapped:${COLOR_MAP[$hex]}"
    return 0
  fi
  if [[ -n "${EP_INTERNAL_COLORS[$hex]}" ]]; then
    echo "ep_internal:${EP_INTERNAL_COLORS[$hex]}"
    return 0
  fi
  if [[ -n "${ORPHAN_COLORS[$hex]}" ]]; then
    echo "orphan:${ORPHAN_COLORS[$hex]}"
    return 0
  fi
  if [[ -n "${AI_COLORS[$hex]}" ]]; then
    echo "ai:${AI_COLORS[$hex]}"
    return 0
  fi
  if [[ -n "${SKELETON_COLORS[$hex]}" ]]; then
    echo "skeleton:${SKELETON_COLORS[$hex]}"
    return 0
  fi
  echo "unknown"
  return 1
}

# ─── 函数：输出颜色统计摘要 ────────────────────────────────
print_color_summary() {
  local file="$1"
  echo "=== 颜色映射覆盖状态 ==="
  echo ""
  echo "精确映射（可自动替换）: ${#COLOR_MAP[@]} 种颜色"
  echo "Element Plus 内部色（不动）: ${#EP_INTERNAL_COLORS[@]} 种颜色"
  echo "孤儿色（需决策）: ${#ORPHAN_COLORS[@]} 种颜色"
  echo "AI 模式色（需清理）: ${#AI_COLORS[@]} 种颜色"
  echo "骨架屏色（需统一）: ${#SKELETON_COLORS[@]} 种颜色"
  echo ""
  echo "详情参见: docs/superpowers/specs/ui-color-mapping.md"
}

# 如果直接执行，打印摘要
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  print_color_summary
fi
