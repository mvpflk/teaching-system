import { computed, unref } from 'vue'
import { useRoute } from 'vue-router'

export function useExamPaper(subject) {
  const route = useRoute()
  const isTraining = computed(() => route.query.mode === 'training')

  const typeConfigs = {
    '语文': [
      { key: 'SINGLE_CHOICE', label: '单选题' }, { key: 'FILL_IN', label: '填空题' },
      { key: 'SHORT_ANSWER', label: '简答题' }, { key: 'COMPOSITION', label: '作文题' }
    ],
    '数学': [
      { key: 'SINGLE_CHOICE', label: '单选题' }, { key: 'FILL_IN', label: '填空题' },
      { key: 'CALCULATION', label: '解答题' }
    ],
    '英语': [
      { key: 'SINGLE_CHOICE', label: '单选题' }, { key: 'READING_COMPREHENSION', label: '阅读理解' },
      { key: 'FILL_IN', label: '翻译填空' }, { key: 'SHORT_ANSWER', label: '翻译题' },
      { key: 'COMPOSITION', label: '书面表达' }
    ],
    '计算机': [
      { key: 'SINGLE_CHOICE', label: '单选题' }, { key: 'MULTI_CHOICE', label: '多选题' },
      { key: 'TRUE_FALSE', label: '判断题' }, { key: 'FILL_IN', label: '填空题' },
      { key: 'SHORT_ANSWER', label: '简答题' }
    ]
  }

  const bareSubject = computed(() => (unref(subject) || '').replace(/\[.*?\]/g, '').trim())
  const availableTypes = computed(() => {
    const bs = bareSubject.value
    for (const [key, types] of Object.entries(typeConfigs)) {
      if (bs.includes(key)) return types
    }
    return [{ key: 'SINGLE_CHOICE', label: '单选题' }, { key: 'MULTI_CHOICE', label: '多选题' },
      { key: 'TRUE_FALSE', label: '判断题' }, { key: 'FILL_IN', label: '填空题' }, { key: 'SHORT_ANSWER', label: '简答题' }]
  })

  const defaultTypeCounts = computed(() => {
    const counts = {}
    for (const t of availableTypes.value) {
      // 按真题规格设默认
      if (t.key === 'COMPOSITION') counts[t.key] = bareSubject.value.includes('语文') ? 2 : 1  // 语文2篇 英语1篇
      else if (t.key === 'READING_COMPREHENSION') counts[t.key] = 5  // 英语5篇
      else if (t.key === 'SINGLE_CHOICE') counts[t.key] = bareSubject.value.includes('语文') ? 10 : bareSubject.value.includes('数学') ? 15 : bareSubject.value.includes('英语') ? 30 : 5
      else if (t.key === 'FILL_IN') counts[t.key] = bareSubject.value.includes('语文') ? 3 : 5  // 语文3 数学5
      else if (t.key === 'SHORT_ANSWER') counts[t.key] = bareSubject.value.includes('语文') ? 4 : 3  // 语文4 英语3
      else if (t.key === 'CALCULATION') counts[t.key] = 6  // 数学6道解答题
      else counts[t.key] = 5
    }
    return counts
  })

  const extraPlaceholder = computed(() => {
    const bs = bareSubject.value
    if (bs.includes('语文')) return '如：建议包含现代文和文言文素材，阅读选文难度适中'
    if (bs.includes('数学')) return '如：计算题数字结果应为整数或简分数，避免复杂小数'
    if (bs.includes('英语')) return '如：阅读文章约200-300词，加入多模态语篇素材'
    if (bs.includes('计算机')) return '如：结合实际操作场景，考察办公软件/网络应用/计算机基础知识点'
    return '如：请联系实际应用场景设计题目'
  })

  return { isTraining, availableTypes, defaultTypeCounts, extraPlaceholder }
}
