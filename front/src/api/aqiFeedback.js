import request, { isNetworkError } from './request'

/**
 * 公众监督反馈信息接口
 * 后端：/api/aqiFeedback/select | /query | /find/{afId} | /save | /update | /delete/{afId}
 *       /api/province/list | /api/province/getCitys
 */

// 请求反馈列表的方法
export async function getAqiFeedbackList() {
  try {
    const res = await request.get('/aqiFeedback/select')
    return res
  } catch (err) {
    if (isNetworkError(err)) {
      // 后端未连接时返回空列表（各页面均有空态处理）
      return { data: { code: 200, message: '后端未连接（演示空数据）', data: [] }, mock: true }
    }
    throw err
  }
}

// 条件查询反馈（NEPM列表查询 / NEPS历史反馈传 telId）
export function queryAqiFeedback(params) {
  return request.get('/aqiFeedback/query', { params })
}

// 根据 afId 查询反馈详情
export function getAqiFeedbackById(afId) {
  return request.get(`/aqiFeedback/find/${afId}`)
}

// 新增反馈（NEPS提交空气质量监督信息，用例3-4）
export async function saveAqiFeedback(data) {
  const res = await request.post('/aqiFeedback/save', data)
  if (res.data.code !== 200) throw new Error(res.data.message || '提交失败')
  return res
}

// 修改反馈
export function updateAqiFeedback(data) {
  return request.post('/aqiFeedback/update', data)
}

// 删除反馈
export function deleteByAfid(afId) {
  return request.delete(`/aqiFeedback/delete/${afId}`)
}

// 获取所有省份
export async function getProvinces() {
  return request.get('/province/list')
}

// 根据省 id 获取城市列表
export function getCityByPid(provinceId) {
  return request.get('/province/getCitys', { params: { provinceId } })
}
