/**
 * 通用API请求封装
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建axios实例
const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 标记重试次数（最多 1 次自动重试）
    config._retryCount = config._retryCount || 0
    // 添加token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    // 全局深拷贝请求体：消除 Vue reactive Proxy 包装，确保 axios JSON.stringify 产出稳定
    // 仅处理普通对象，跳过 FormData/Blob/File 等二进制类型
    if (config.data && typeof config.data === 'object' && !(config.data instanceof FormData)) {
      try { config.data = JSON.parse(JSON.stringify(config.data)) } catch { /* 序列化失败保持原样 */ }
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    // 非 JSON 响应（text/html / blob 等）直接透传原始数据，不进行 JSON 解析
    const respType = response.config.responseType
    if (respType === 'text' || respType === 'blob' || respType === 'arraybuffer') {
      return response.data
    }

    const res = response.data

    // 成功响应
    if (res.code === 200) {
      return res
    }

    // Token过期
    if (res.code === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      router.push('/login')
      return Promise.reject(new Error('登录已过期'))
    }

    // 其他错误（silent 请求不弹 toast，交由调用方自行处理）
    if (!response.config?.silent) {
      ElMessage.error(res.message || '操作失败')
    }
    return Promise.reject(new Error(res.message || '操作失败'))
  },
  error => {
    console.error('响应错误:', error)

    if (error.response) {
      const status = error.response.status
      const message = error.response.data?.message || '服务器错误'

      switch (status) {
        case 401:
          ElMessage.error('未登录或登录已过期')
          localStorage.removeItem('token')
          router.push('/login')
          break
        case 403:
          // 强制改密
          if (error.response.data?.data?.mustChangePassword) {
            ElMessage.warning('请先修改默认密码后再使用系统')
            router.push('/profile?tab=password')
            return Promise.reject(new Error('请先修改默认密码'))
          }
          // 不弹窗，由各页面自行处理或忽略
          console.warn('403 权限拒绝:', error.response.config?.url)
          break
        case 404:
          ElMessage.error(message === '服务器错误' ? '请求的资源不存在' : message)
          break
        case 409:
          // 冲突状态（如抽问池已空），不弹 toast → 由调用方按需处理
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(message)
      }
    } else if (error.request) {
      // 网络错误 — GET 请求自动重试一次（静默）
      if (error.config?.method?.toLowerCase() === 'get' && error.config._retryCount < 1) {
        error.config._retryCount = (error.config._retryCount || 0) + 1
        return service(error.config)
      }
      ElMessage.error('网络错误，请检查网络连接')
    } else {
      ElMessage.error('请求配置错误')
    }

    return Promise.reject(error)
  }
)

/**
 * 带认证的文件下载工具
 * 替代 window.open(url) 和手动 XMLHttpRequest + token 的不安全模式
 */
export function downloadFile(url, filename) {
  return service({ url, method: 'get', responseType: 'blob' }).then(data => {
    const blob = data instanceof Blob ? data : new Blob([data])
    const objectUrl = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = objectUrl
    a.download = filename || url.split('/').pop() || 'download'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(objectUrl)
  })
}

export default service
