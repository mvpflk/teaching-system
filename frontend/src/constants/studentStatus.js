// 学生状态：统一常量，供 StudentList / StudentFormDialog 使用
export const STUDENT_STATUS = {
  active: { label: '正常在读', tag: 'success' },
  leave: { label: '请假', tag: 'warning' },
  withdraw: { label: '退学', tag: 'danger' },
  transfer: { label: '转学', tag: 'info' },
  retain: { label: '留级', tag: 'warning' },
  graduated: { label: '已毕业', tag: 'info' },
}

export const STUDENT_STATUS_OPTIONS = Object.entries(STUDENT_STATUS).map(([value, { label }]) => ({ value, label }))

export function statusLabel(status) { return STUDENT_STATUS[status]?.label || status || '-' }
export function statusTagType(status) { return STUDENT_STATUS[status]?.tag || 'info' }
