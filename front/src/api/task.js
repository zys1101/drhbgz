import request, { isNetworkError } from './request'
import { getAqiFeedbackList } from './aqiFeedback'

/**
 * 任务流转接口（NEPM指派 / NEPG网格员 / NEPM确认AQI数据）
 * 后端：POST /api/task/assign   GET /api/task/list/{gridCode}   POST /api/task/measure
 *       GET /api/aqiData/list   GET /api/aqiData/confirm/{id}   GET /api/aqiData/reject/{id}
 *       GET /api/gridWorker/list
 * 后端未连接时降级为本地演示数据（localStorage），保证流程可独立演示
 */

const ASSIGN_KEY = 'nepm_assignments'  // 指派记录（演示阶段本地存储）
const MEASURE_KEY = 'nepm_measures'    // 网格员提交的实测数据（演示阶段本地存储）

function lsGet(key) {
  try {
    return JSON.parse(localStorage.getItem(key)) || []
  } catch (e) {
    return []
  }
}

function lsSet(key, val) {
  localStorage.setItem(key, JSON.stringify(val))
}

// 无真实后端时的示例任务
function demoTasks() {
  return [
    {
      taskId: 'T-2026-0001', afId: 101, gridCode: 'grid001',
      provinceName: '辽宁省', cityName: '沈阳市', address: '浑南区智慧二街 200 号',
      telId: '13800001111', estimatedGrade: 3, information: '附近工地施工，扬尘明显，异味较重。',
      afDate: '2026-09-07', afTime: '09:30:00', state: 1
    },
    {
      taskId: 'T-2026-0002', afId: 102, gridCode: 'grid001',
      provinceName: '辽宁省', cityName: '大连市', address: '甘井子区华北路 351 号',
      telId: '13900002222', estimatedGrade: 2, information: '早高峰车流量大，尾气味道明显。',
      afDate: '2026-09-07', afTime: '10:12:45', state: 1
    },
    {
      taskId: 'T-2026-0003', afId: 103, gridCode: 'grid001',
      provinceName: '吉林省', cityName: '长春市', address: '南关区人民大街 106 号',
      telId: '13700003333', estimatedGrade: 4, information: '造纸厂周边刺鼻气味，能见度低。',
      afDate: '2026-09-06', afTime: '16:40:20', state: 1
    }
  ]
}

function demoMeasured() {
  return [
    {
      dataId: 'D-2026-0101', afId: 101, provinceName: '辽宁省', cityName: '沈阳市',
      address: '浑南区智慧二街 200 号', so2Grade: 2, coGrade: 1, pm25Grade: 3, aqiGrade: 3,
      gridCode: 'grid001', submitDate: '2026-09-07', submitTime: '14:20:11', state: 0
    },
    {
      dataId: 'D-2026-0102', afId: 104, provinceName: '黑龙江省', cityName: '哈尔滨市',
      address: '松北区世纪大道 1 号', so2Grade: 1, coGrade: 1, pm25Grade: 2, aqiGrade: 2,
      gridCode: 'grid002', submitDate: '2026-09-06', submitTime: '11:05:33', state: 0
    },
    {
      dataId: 'D-2026-0103', afId: 105, provinceName: '河北省', cityName: '石家庄市',
      address: '长安区中山东路 300 号', so2Grade: 4, coGrade: 3, pm25Grade: 5, aqiGrade: 5,
      gridCode: 'grid003', submitDate: '2026-09-05', submitTime: '17:42:08', state: 0
    }
  ]
}

// 指派网格员（用例3-10）：优先调用后端，未连接时写本地演示记录
export async function assignTask(afId, gridCode, feedback) {
  try {
    const res = await request.post('/task/assign', { afId, gridCode })
    // 后端业务校验失败是 HTTP 200 + body.code!=200（GlobalExceptionHandler），
    // axios 不会抛异常，必须显式判断——否则“不允许异地指派/网格员非工作状态”等
    // 拒绝会被静默当成成功（界面提示指派成功，实际没指派）。
    if (res.data && res.data.code !== 200) {
      throw new Error(res.data.message || '指派失败')
    }
    return { mock: false, message: res.data && res.data.message }
  } catch (err) {
    // 业务错误（我们自己抛的 Error，不是 axios 异常）直接上抛，
    // 不能让 isNetworkError 把“无 response”误判成网络故障而走演示降级
    if (err && !err.isAxiosError) throw err
    if (isNetworkError(err)) {
      const list = lsGet(ASSIGN_KEY)
      list.push({
        taskId: 'T-' + Date.now(),
        afId,
        gridCode,
        assignDate: new Date().toISOString().slice(0, 10),
        assignTime: new Date().toTimeString().slice(0, 8),
        provinceName: feedback.provinceName,
        cityName: feedback.cityName,
        address: feedback.address,
        telId: feedback.telId,
        estimatedGrade: feedback.estimatedGrade,
        information: feedback.information,
        afDate: feedback.afDate,
        afTime: feedback.afTime,
        state: 1
      })
      lsSet(ASSIGN_KEY, list)
      return { mock: true }
    }
    throw err
  }
}

// 网格员任务列表（用例3-7）
export async function getMyTasks(gridCode) {
  try {
    const res = await request.get(`/task/list/${gridCode}`)
    if (res.data.code === 200) {
      return { list: res.data.data || [], mock: false }
    }
    throw new Error(res.data.message || '查询失败')
  } catch (err) {
    if (isNetworkError(err)) {
      const assigned = lsGet(ASSIGN_KEY).filter(t => t.gridCode === gridCode && t.state === 1)
      return { list: [...assigned, ...demoTasks()], mock: true }
    }
    throw err
  }
}

// 提交实测AQI数据（用例3-8）
export async function submitMeasure(task, measure) {
  const payload = {
    afId: task.afId,
    gridCode: task.gridCode,
    so2Grade: measure.so2Grade,
    coGrade: measure.coGrade,
    pm25Grade: measure.pm25Grade,
    aqiGrade: measure.aqiGrade
  }
  try {
    await request.post('/task/measure', payload)
    return { mock: false }
  } catch (err) {
    if (isNetworkError(err)) {
      const list = lsGet(MEASURE_KEY)
      list.push({
        dataId: 'D-' + Date.now(),
        afId: task.afId,
        provinceName: task.provinceName,
        cityName: task.cityName,
        address: task.address,
        gridCode: task.gridCode,
        ...measure,
        submitDate: new Date().toISOString().slice(0, 10),
        submitTime: new Date().toTimeString().slice(0, 8),
        state: 0
      })
      lsSet(MEASURE_KEY, list)
      // 本地任务标记完成
      const assigned = lsGet(ASSIGN_KEY)
      const hit = assigned.find(t => t.taskId === task.taskId)
      if (hit) {
        hit.state = 3
        lsSet(ASSIGN_KEY, assigned)
      }
      return { mock: true }
    }
    throw err
  }
}

// 管理端：确认AQI数据列表
export async function getAqiDataList() {
  try {
    const res = await request.get('/aqiData/list')
    if (res.data.code === 200) {
      return { list: res.data.data || [], mock: false }
    }
    throw new Error(res.data.message || '查询失败')
  } catch (err) {
    if (isNetworkError(err)) {
      const local = lsGet(MEASURE_KEY)
      return { list: [...local, ...demoMeasured()], mock: true }
    }
    throw err
  }
}

// 管理端：确认数据无误，纳入统计
export async function confirmAqiData(dataId) {
  try {
    await request.get(`/aqiData/confirm/${dataId}`)
    return { mock: false }
  } catch (err) {
    if (isNetworkError(err)) {
      const list = lsGet(MEASURE_KEY)
      const hit = list.find(d => String(d.dataId) === String(dataId))
      if (hit) {
        hit.state = 1
        lsSet(MEASURE_KEY, list)
      }
      return { mock: true }
    }
    throw err
  }
}

// 管理端：退回异常数据，任务重新置为待指派
export async function rejectAqiData(dataId) {
  try {
    await request.get(`/aqiData/reject/${dataId}`)
    return { mock: false }
  } catch (err) {
    if (isNetworkError(err)) {
      const list = lsGet(MEASURE_KEY)
      const hit = list.find(d => String(d.dataId) === String(dataId))
      if (hit) {
        hit.state = 2 // 已退回
        lsSet(MEASURE_KEY, list)
      }
      return { mock: true }
    }
    throw err
  }
}

// 网格员名单（工作状态由“东软HR系统”统一管理）
export async function getGridWorkers() {
  try {
    const res = await request.get('/gridWorker/list')
    if (res.data.code === 200) {
      return { list: res.data.data || [], mock: false }
    }
    throw new Error(res.data.message || '查询失败')
  } catch (err) {
    if (isNetworkError(err)) {
      return {
        mock: true,
        list: [
          { gridCode: 'grid001', realName: '王铁柱', region: '辽宁省-沈阳市', working: true },
          { gridCode: 'grid002', realName: '李雪松', region: '黑龙江省-哈尔滨市', working: true },
          { gridCode: 'grid003', realName: '赵敏', region: '河北省-石家庄市', working: true },
          { gridCode: 'grid004', realName: '陈海涛', region: '辽宁省-大连市', working: true },
          { gridCode: 'grid005', realName: '刘洋', region: '吉林省-长春市', working: false }
        ]
      }
    }
    throw err
  }
}
