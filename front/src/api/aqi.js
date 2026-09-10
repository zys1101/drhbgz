import request, { isNetworkError } from './request'

/**
 * AQI级别表接口（空气质量指数（AQI）范围及相应类别表）
 * 后端：GET /api/aqi/list | /find/{aqiId} | POST /save | /update | DELETE /delete/{aqiId}
 * 后端未连接时使用国家标准AQI级别表作为演示数据
 */

export function demoLevels() {
  return [
    { aqiId: 1, chineseExplain: '一级', aqiExplain: '优', aqiRange: '0~50', color: '#00e400', so2Min: 0, so2Max: 50, coMin: 0, coMax: 2, spmMin: 0, spmMax: 35, healthImpact: '空气质量令人满意，基本无空气污染', takeSteps: '各类人群可正常活动' },
    { aqiId: 2, chineseExplain: '二级', aqiExplain: '良', aqiRange: '51~100', color: '#ffff00', so2Min: 51, so2Max: 150, coMin: 3, coMax: 4, spmMin: 36, spmMax: 75, healthImpact: '空气质量可接受，但某些污染物可能对极少数异常敏感人群健康有较弱影响', takeSteps: '极少数异常敏感人群应减少户外活动' },
    { aqiId: 3, chineseExplain: '三级', aqiExplain: '轻度污染', aqiRange: '101~150', color: '#ff7e00', so2Min: 151, so2Max: 475, coMin: 5, coMax: 14, spmMin: 76, spmMax: 115, healthImpact: '易感人群症状有轻度加剧，健康人群出现刺激症状', takeSteps: '儿童、老人及心脏病、呼吸系统疾病患者应减少长时间户外剧烈运动' },
    { aqiId: 4, chineseExplain: '四级', aqiExplain: '中度污染', aqiRange: '151~200', color: '#ff0000', so2Min: 476, so2Max: 800, coMin: 15, coMax: 24, spmMin: 116, spmMax: 150, healthImpact: '进一步加剧易感人群症状，可能对健康人群心脏、呼吸系统有影响', takeSteps: '儿童、老人及心脏病、呼吸系统疾病患者避免长时间高强度户外锻炼' },
    { aqiId: 5, chineseExplain: '五级', aqiExplain: '重度污染', aqiRange: '201~300', color: '#99004c', so2Min: 801, so2Max: 1600, coMin: 25, coMax: 36, spmMin: 151, spmMax: 250, healthImpact: '心脏病和肺病患者症状显著加剧，运动耐受力降低，健康人群普遍出现症状', takeSteps: '儿童、老人和心脏病、肺病患者应留在室内，停止户外运动' },
    { aqiId: 6, chineseExplain: '六级', aqiExplain: '严重污染', aqiRange: '300以上', color: '#7f0023', so2Min: 1601, so2Max: 2100, coMin: 37, coMax: 48, spmMin: 251, spmMax: 350, healthImpact: '健康人群运动耐受力降低，有明显强烈症状，提前出现某些疾病', takeSteps: '儿童、老人和病人应留在室内，避免体力消耗' }
  ]
}

// 查询所有空气质量指数级别
export async function getAqiList() {
  try {
    return await request.get('/aqi/list')
  } catch (err) {
    if (isNetworkError(err)) {
      return { data: { code: 200, message: '后端未连接（标准级别演示数据）', data: demoLevels() }, mock: true }
    }
    throw err
  }
}

// 根据 aqiId 查询
export function getAqiById(aqiId) {
  return request.get(`/aqi/find/${aqiId}`)
}

// 新增
export function saveAqi(data) {
  return request.post('/aqi/save', data)
}

// 修改
export function updateAqi(data) {
  return request.post('/aqi/update', data)
}

// 删除
export function deleteAqi(aqiId) {
  return request.delete(`/aqi/delete/${aqiId}`)
}
