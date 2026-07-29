/**
 * 评分等级标签阈值 — 前后端统一使用
 * CLAUDE.md #36: 不使用 AI 返回的 developmentalLabel，保证同分学生标签一致
 */
export const GRADE_LABEL_THRESHOLDS = [
  { key: '已达标', min: 85, label: '≥85分', color: '#67c23a' },
  { key: '成长中', min: 70, label: '70-84分', color: '#409eff' },
  { key: '发展中', min: 60, label: '60-69分', color: '#e6a23c' },
  { key: '起步期', min: 0,  label: '<60分',  color: '#f56c6c' },
]

/**
 * 根据分数返回等级标签
 * @param {number} score
 * {{@link GRADE_LABEL_THRESHOLDS}} 的 key
 */
export function getGradeLabel(score) {
  for (const t of GRADE_LABEL_THRESHOLDS) {
    if (score >= t.min) return t.key
  }
  return '起步期'
}
