/**
 * 学科配色常量
 * 所有学科色集中管理，禁止在各页面中硬编码
 */

export const SUBJECT_COLORS = {
  '语文': '#d4584a',   // 红色系
  '数学': '#2d7dd2',   // 蓝色系
  '英语': '#4a9d5a',   // 绿色系
  '物理': '#6b4fa0',   // 紫色系
  '化学': '#c97d2d',   // 橙色系
  '生物': '#3d9a7a',   // 青色系
  '历史': '#b07a4a',   // 棕色系
  '地理': '#5a8a6a',   // 墨绿系
  '政治': '#c44040',   // 红色系
  '计算机': '#2d7dd2',  // 蓝色系
  '体育': '#4a8a3a',   // 深绿系
  '音乐': '#b05a8a',   // 玫红系
}

export const DEFAULT_SUBJECT_COLOR = '#6b7a8a'

export function getSubjectColor(subject) {
  return SUBJECT_COLORS[subject] || DEFAULT_SUBJECT_COLOR
}
