import request from './request'

/**
 * 人员管理（HR）接口
 * 网格员：账号注册 / 信息维护（地区、工作状态）/ 请假申请与审批 / 销假
 * 公众监督员：列表查询 / 信息修改
 * 后端：GET /api/employee/list | POST /employee/save|update
 *       POST /api/leave/apply|approve|back | GET /api/leave/list
 *       GET /api/supervisor/list | POST /api/supervisor/update
 */

// 网格员列表
export function getEmployeeList() {
  return request.get('/employee/list')
}

// 新增网格员账号
export function saveEmployee(data) {
  return request.post('/employee/save', data)
}

// 修改网格员（姓名/负责地区/工作状态）
export function updateEmployee(data) {
  return request.post('/employee/update', data)
}

// 网格员发起请假申请
export function applyLeave(data) {
  return request.post('/leave/apply', data)
}

// 请假记录列表（可按网格员编码/状态筛选）
export function getLeaveList(params) {
  return request.get('/leave/list', { params })
}

// 审批请假（agree: true 同意 / false 驳回）
export function approveLeave(data) {
  return request.post('/leave/approve', data)
}

// 销假（恢复工作状态）
export function backLeave(data) {
  return request.post('/leave/back', data)
}

// 公众监督员列表
export function getSupervisorList() {
  return request.get('/supervisor/list')
}

// 修改公众监督员信息
export function updateSupervisor(data) {
  return request.post('/supervisor/update', data)
}
