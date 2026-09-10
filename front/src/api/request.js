import axios from 'axios'

/**
 * 统一的 axios 实例：
 * baseURL 使用相对路径 /api，由 vue.config.js 的 devServer 代理转发到后端服务
 * （开发环境 http://localhost:9000，生产环境由 Nginx 反向代理），避免跨域问题
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 8000
})

export default request

// 判断是否为网络异常（后端未启动/代理不可达），用于各接口的演示数据降级
export function isNetworkError(err) {
  return !err.response
}
