/**
 * 用户API
 */
import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/auth/actions/login',
    method: 'post',
    data
  })
}

export function logout() {
  return request({
    url: '/auth/actions/logout',
    method: 'post'
  })
}

export function getUserInfo() {
  return request({
    url: '/auth/actions/info',
    method: 'get'
  })
}
