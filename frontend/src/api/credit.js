import request from '@/utils/request'

// ── 学生端 ──
export function getCreditInfo() { return request({ url: '/credit/actions/info', method: 'get' }) }
export function getCreditTransactions(params) { return request({ url: '/credit/actions/transactions', method: 'get', params }) }
export function getCreditRanking(params) { return request({ url: '/credit/actions/ranking', method: 'get', params }) }
export function getMoralRanking(params) { return request({ url: '/credit/actions/moral-ranking', method: 'get', params }) }
export function getShopItems(params) { return request({ url: '/credit/shop', method: 'get', params }) }
export function redeemItem(data) { return request({ url: '/credit/actions/redeem', method: 'post', data }) }
export function signIn() { return request({ url: '/credit/actions/sign', method: 'post' }) }
export function getAchievements() { return request({ url: '/credit/actions/achievements', method: 'get' }) }
export function getTitleLevels() { return request({ url: '/credit/titles', method: 'get' }) }

// ── 管理端 ──
export function adjustCredit(data) { return request({ url: '/credit/actions/adjust-credit', method: 'post', data }) }
export function getAdminRules() { return request({ url: '/credit/admin/rules', method: 'get' }) }
export function createAdminRule(data) { return request({ url: '/credit/admin/rules', method: 'post', data }) }
export function updateAdminRule(id, data) { return request({ url: `/credit/admin/rules/${id}`, method: 'put', data }) }
export function deleteAdminRule(id) { return request({ url: `/credit/admin/rules/${id}`, method: 'delete' }) }
export function setCustomTitle(title) { return request({ url: '/credit/actions/custom-title', method: 'put', data: { titleCode: title } }) }
export function adminSetCustomTitle(studentId, title) { return request({ url: `/credit/admin/students/${studentId}/actions/custom-title`, method: 'put', data: { titleCode: title } }) }
export function getCustomTitle() { return request({ url: '/credit/actions/custom-title', method: 'get' }) }
export function getAdminShop() { return request({ url: '/credit/admin/shop', method: 'get' }) }
export function adminCreateShop(data) { return request({ url: '/credit/admin/shop', method: 'post', data }) }
export function adminUpdateShop(id, data) { return request({ url: `/credit/admin/shop/${id}`, method: 'put', data }) }
export function adminDeleteShop(id) { return request({ url: `/credit/admin/shop/${id}`, method: 'delete' }) }
export function getAdminDeliveries(params) { return request({ url: '/credit/admin/deliveries', method: 'get', params }) }
export function deliverItem(id) { return request({ url: `/credit/admin/deliveries/${id}/actions/deliver`, method: 'put' }) }
export function getAdminStudents(keyword) { return request({ url: '/credit/admin/students', method: 'get', params: { keyword } }) }
export function getAdminTitles() { return request({ url: '/credit/admin/titles', method: 'get' }) }
export function updateAdminTitle(id, data) { return request({ url: `/credit/admin/titles/${id}`, method: 'put', data }) }
