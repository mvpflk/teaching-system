import request from '@/utils/request'

// ── 试卷导入 ──────────────────────────────────────
/** 上传文件解析（不入库） */
export function parsePaper(data)         { return request({ url: '/paper-import/actions/parse', method: 'post', data }) }

/** 创建任务（试卷导入结果+赋分+配置+目标班级） */
export function createPaperTask(data)    { return request({ url: '/paper-import/actions/create', method: 'post', data }) }

// ── 试卷库 ────────────────────────────────────────
/** 我的试卷列表 */
export function listPapers(params)       { return request({ url: '/paper-import/library', method: 'get', params }) }

/** 删除试卷 */
export function deletePaper(id)          { return request({ url: `/paper-import/library/${id}`, method: 'delete' }) }

/** 从试卷库快速创建任务 */
export function createTaskFromPaper(id, data) { return request({ url: `/paper-import/library/${id}/actions/create-task`, method: 'post', data }) }
