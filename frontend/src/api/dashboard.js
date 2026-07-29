import request from '@/utils/request'

export function getTeacherDashboard(params) {
  return request({ url: '/dashboard/actions/teacher', method: 'get', params })
}
