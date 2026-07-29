 import request from '@/utils/request'
 
 export function getMyActivities() {
   return request({ url: '/teacher/activity/me', method: 'get' })
 }
