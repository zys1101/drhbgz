import axios from 'axios'

// 任务流转相关接口（NEPG 网格员端 / NEPM 指派）。
// TODO: 后端就绪后删除 fallback 部分，直接使用真实接口。
const request = axios.create({
  baseURL: 'http://localhost:9000/',
  timeout: 3000
})

const ASSIGN_KEY = 'nepm_assignments'   // 指派记录（演示阶段本地存储）
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

// ---------- 后端就绪后启用的真实接口 ----------
// 指派网格员
function apiAssign(data) {
  return request({ url: '/task/assign', method: 'post', data })
}
// 网格员任务列表
function apiMyTasks(gridCode) {
  return request({ url: `/task/list/${gridCode}`, method: 'get' })
}
// 提交实测AQI数据
function apiSubmitMeasure(data) {
  return request({ url: '/task/measure', method: 'post', data })
}
// 确认AQI数据列表
function apiAqiDataList() {
  return request({ url: '/aqiData/list', method: 'get' })
}
// 退回异常AQI数据
function apiRejectAqiData(dataId) {
  return request({ url: `/aqiData/reject/${dataId}`, method: 'get' })
}

// ---------- 演示兜底（后端未连接时使用，均标记 mock: true） ----------

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

// 指派网格员：优先写本地演示记录（后端就绪后切换为 apiAssign）
export async function assignTask(afId, gridCode, feedback) {
  try {
    await apiAssign({ afId, gridCode })
    return { mock: false }
  } catch (err) {
    const list = lsGet(ASSIGN_KEY)
    list.push({
      taskId: 'T-' + Date.now(),
      afId,
      gridCode,
      assignTime: new Date().toLocaleString('zh-CN', { hour12: false }),
      // 冗余反馈信息，便于网格员端直接展示
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
}

// 网格员任务列表 = 后端接口 + 指派记录 + 示例数据
export async function getMyTasks(gridCode) {
  try {
    const res = await apiMyTasks(gridCode)
    if (res.data.code === 200) {
      return { list: res.data.data || [], mock: false }
    }
    throw new Error('bad code')
  } catch (err) {
    const assigned = lsGet(ASSIGN_KEY).filter(t => t.gridCode === gridCode && t.state === 1)
    return { list: [...assigned, ...demoTasks()], mock: true }
  }
}

// 提交实测AQI数据
export async function submitMeasure(task, measure) {
  const payload = {
    ...measure,
    taskId: task.taskId,
    afId: task.afId,
    provinceName: task.provinceName,
    cityName: task.cityName,
    address: task.address,
    gridCode: task.gridCode,
    submitTime: new Date().toLocaleString('zh-CN', { hour12: false })
  }
  try {
    await apiSubmitMeasure(payload)
    return { mock: false }
  } catch (err) {
    const list = lsGet(MEASURE_KEY)
    list.push({ dataId: 'D-' + Date.now(), ...payload, state: 0 })
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
}

// 管理端：确认AQI数据列表
export async function getAqiDataList() {
  try {
    const res = await apiAqiDataList()
    if (res.data.code === 200) {
      return { list: res.data.data || [], mock: false }
    }
    throw new Error('bad code')
  } catch (err) {
    const local = lsGet(MEASURE_KEY)
    return { list: [...local, ...demoMeasured()], mock: true }
  }
}

function demoMeasured() {
  return [
    {
      dataId: 'D-2026-0101', afId: 101, provinceName: '辽宁省', cityName: '沈阳市',
      address: '浑南区智慧二街 200 号', so2Grade: 2, coGrade: 1, pm25Grade: 3, aqiGrade: 3,
      gridCode: 'grid001', submitTime: '2026-09-07 14:20:11', state: 0
    },
    {
      dataId: 'D-2026-0102', afId: 104, provinceName: '黑龙江省', cityName: '哈尔滨市',
      address: '松北区世纪大道 1 号', so2Grade: 1, coGrade: 1, pm25Grade: 2, aqiGrade: 2,
      gridCode: 'grid002', submitTime: '2026-09-06 11:05:33', state: 0
    },
    {
      dataId: 'D-2026-0103', afId: 105, provinceName: '河北省', cityName: '石家庄市',
      address: '长安区中山东路 300 号', so2Grade: 4, coGrade: 3, pm25Grade: 5, aqiGrade: 5,
      gridCode: 'grid003', submitTime: '2026-09-05 17:42:08', state: 0
    }
  ]
}

// 退回异常数据：任务重新置为待指派
export async function rejectAqiData(dataId) {
  try {
    await apiRejectAqiData(dataId)
    return { mock: false }
  } catch (err) {
    const list = lsGet(MEASURE_KEY)
    const hit = list.find(d => d.dataId === dataId)
    if (hit) {
      hit.state = 2 // 已退回
      lsSet(MEASURE_KEY, list)
    }
    return { mock: true }
  }
}

// 网格员名单（工作状态由"东软HR系统"统一管理，此处为演示数据）
// TODO: 后端就绪后替换为 GET /gridWorker/list
export async function getGridWorkers() {
  try {
    const res = await request({ url: '/gridWorker/list', method: 'get' })
    if (res.data.code === 200) {
      return { list: res.data.data || [], mock: false }
    }
    throw new Error('bad code')
  } catch (err) {
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
}
