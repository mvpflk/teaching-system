/**
 * 学生状态共享工具
 * 全系统统一来源，StudentList / StudentFormDialog 共用
 */

/** 状态 → 中文标签映射 */
export const STATUS_MAP = {
  active: '正常在读',
  leave: '请假',
  withdraw: '退学',
  transfer: '转学',
  retain: '留级',
  graduated: '已毕业'
}

/** 状态 → ElementPlus tag type */
export const STATUS_TAG_TYPE_MAP = {
  active: 'success',
  leave: 'warning',
  withdraw: 'danger',
  transfer: 'info',
  retain: 'warning',
  graduated: 'info'
}

/** 状态选项（用于 el-select） */
export const STATUS_OPTIONS = Object.entries(STATUS_MAP).map(([value, label]) => ({ value, label }))

/** 获取状态中文标签 */
export const statusLabel = (s) => STATUS_MAP[s] || s || '-'

/** 获取状态对应的 Element Plus tag 类型 */
export const statusTagType = (s) => STATUS_TAG_TYPE_MAP[s] || 'info'

/** 判断是否为非活跃状态（不可参加考试/提交作业） */
export const isNonActive = (s) => s && s !== 'active'
