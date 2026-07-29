import request from '@/utils/request'

export function getFeatureFlags() { return request({ url: '/settings/features', method: 'get' }) }
export function getSettings() { return request({ url: '/settings', method: 'get' }) }
export function getFeatures() { return request({ url: '/settings/features', method: 'get' }) }
export function getSettingsStatus() { return request({ url: '/settings/actions/status', method: 'get' }) }
export function updateSettings(data) { return request({ url: '/settings/actions/update-all', method: 'put', data }) }
export function getGrades(stageId) { return request({ url: '/settings/grades', method: 'get', params: stageId ? { stageId } : {} }) }
export function createGrade(data) { return request({ url: '/settings/grades', method: 'post', data }) }
export function updateGrade(id, data) { return request({ url: `/settings/grades/${id}`, method: 'put', data }) }
export function deleteGrade(id) { return request({ url: `/settings/grades/${id}`, method: 'delete' }) }
export function getSubjects() { return request({ url: '/settings/subjects', method: 'get' }) }
export function createSubject(data) { return request({ url: '/settings/subjects', method: 'post', data }) }
export function updateSubject(id, data) { return request({ url: `/settings/subjects/${id}`, method: 'put', data }) }
export function deleteSubject(id) { return request({ url: `/settings/subjects/${id}`, method: 'delete' }) }
export function getMajors() { return request({ url: '/settings/majors', method: 'get' }) }
export function createMajor(data) { return request({ url: '/settings/majors', method: 'post', data }) }
export function updateMajor(id, data) { return request({ url: `/settings/majors/${id}`, method: 'put', data }) }
export function deleteMajor(id) { return request({ url: `/settings/majors/${id}`, method: 'delete' }) }
export function getMajorSubjects(majorId) { return request({ url: `/settings/majors/${majorId}/subjects`, method: 'get' }) }
export function setMajorSubjects(majorId, data) { return request({ url: `/settings/majors/${majorId}/subjects`, method: 'put', data }) }
export function resetData(data) { return request({ url: '/settings/actions/reset', method: 'post', data }) }

export function getMySubjects() { return request({ url: '/dictionary/actions/my-subjects', method: 'get' }) }
export function getMyTeachingAssignments() { return request({ url: '/dictionary/actions/my-teaching-assignments', method: 'get' }) }
export function getAiConfig(provider = 'deepseek') { return request({ url: '/settings/ai-config', method: 'get', params: { provider } }) }
export function updateAiConfig(data, provider = 'deepseek') { return request({ url: '/settings/ai-config', method: 'put', params: { provider }, data }) }
export function switchAiProvider(provider) { return request({ url: '/settings/ai-config/provider', method: 'put', data: { provider } }) }
export function listAiProviders() { return request({ url: '/settings/ai-config/providers', method: 'get' }) }
export function deleteCustomProvider(name) { return request({ url: `/settings/ai-config/custom/${name}`, method: 'delete' }) }
export function getWuyuTags() { return request({ url: '/dictionary/wuyu-tags', method: 'get' }) }
export function getTermList(schoolId) { return request({ url: '/dictionary/terms', method: 'get', params: { schoolId } }) }
export function getTerms() { return request({ url: '/settings/terms', method: 'get' }) }
export function createTerm(data) { return request({ url: '/settings/terms', method: 'post', data }) }
export function updateTerm(id, data) { return request({ url: `/settings/terms/${id}`, method: 'put', data }) }
export function deleteTerm(id) { return request({ url: `/settings/terms/${id}`, method: 'delete' }) }
export function getRubricList(schoolId) { return request({ url: '/dictionary/rubrics', method: 'get', params: { schoolId } }) }

// 偏科提分班级配置
export function getRemedialClasses() { return request({ url: '/settings/remedial-classes', method: 'get' }) }
export function updateRemedialClasses(data) { return request({ url: '/settings/remedial-classes', method: 'put', data }) }
