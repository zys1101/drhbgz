import request from './request'

/**
 * 网格员增员请求接口
 * 业务规则：管理员指派网格员只允许“本地指派”（网格员负责的省+市必须与反馈一致）。
 * 若该反馈所在网格区域没有可工作的本地网格员，后端会拒绝异地指派，
 * 此时由管理员发起“增员请求”，供管理员跟进、供决策者判断是否需要增员。
 * 后端：POST /api/gridDemand/apply | GET /api/gridDemand/list | POST /api/gridDemand/handle
 */

// 发起增员请求（该区域无可工作的本地网格员时）
export function applyGridDemand(data) {
  return request.post('/gridDemand/apply', data)
}

// 增员请求列表（state: 0待处理/1已处理/2已忽略，cityId 可选）
export function getGridDemandList(params) {
  return request.get('/gridDemand/list', { params })
}

// 处理增员请求（state: 1已处理 / 2已忽略）
export function handleGridDemand(data) {
  return request.post('/gridDemand/handle', data)
}
