import request from '@/utils/request'
export function logSkip(data) { return request.post('/question-skip-log', data) }
