/** 统一提交状态标签和颜色映射 */
export function useSubmissionStatus() {
  const STATUS_MAP = {
    PENDING:       { label: '未交',  tag: 'info' },
    SUBMITTED:     { label: '已交',  tag: 'warning' },
    GRADED:        { label: '已批',  tag: 'success' },
    RETURNED:      { label: '退回',  tag: 'danger' },
    EXEMPTED:      { label: '豁免',  tag: '' },
    NOT_STARTED:   { label: '未开始', tag: 'info' },
    TERMINATED:    { label: '作弊终止', tag: 'danger' },
  }

  const statusLabel = (s) => STATUS_MAP[s]?.label || s
  const statusTag = (s) => STATUS_MAP[s]?.tag || ''

  const statusDotClass = (s) => STATUS_MAP[s]?.tag || ''

  return { STATUS_MAP, statusLabel, statusTag, statusDotClass }
}
