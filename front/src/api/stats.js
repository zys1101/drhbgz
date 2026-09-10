import axios from 'axios'
import { getAqiFeedbackList } from './aqiFeedback.js'

// 统计数据接口（NEPM 统计数据管理 / NEPV 可视化大屏）。
// 五项统计：省分组超标统计、AQI指数分布、12个月趋势、实时统计、网格覆盖率。
// TODO: 后端就绪后删除 fallback 部分，直接使用真实接口。
const request = axios.create({
  baseURL: 'http://localhost:9000/',
  timeout: 3000
})

// 全国 106 个大城市中优先覆盖的示例网格（演示数据）
const COVERED_CITIES = [
  { province: '辽宁省', city: '沈阳市' }, { province: '辽宁省', city: '大连市' },
  { province: '吉林省', city: '长春市' }, { province: '黑龙江省', city: '哈尔滨市' },
  { province: '河北省', city: '石家庄市' }, { province: '北京市', city: '北京城区' },
  { province: '上海市', city: '上海城区' }, { province: '广东省', city: '广州市' },
  { province: '广东省', city: '深圳市' }, { province: '四川省', city: '成都市' },
  { province: '湖北省', city: '武汉市' }, { province: '陕西省', city: '西安市' }
]

const TOTAL_PROVINCES = 34
const TOTAL_BIG_CITIES = 106

async function tryApi(url, fallback) {
  try {
    const res = await request({ url, method: 'get' })
    if (res.data.code === 200) {
      return { data: res.data.data, mock: false }
    }
    throw new Error('bad code')
  } catch (err) {
    return { data: await fallback(), mock: true }
  }
}

// 1. 省分组超标统计
export function getProvinceStats() {
  return tryApi('/stats/province', async () => {
    // 优先从真实反馈列表里聚合，取不到再用示例数据
    try {
      const res = await getAqiFeedbackList()
      const list = res.data.code === 200 ? (res.data.data || []) : []
      if (!list.length) throw new Error('empty')
      const map = {}
      list.forEach(f => {
        const key = f.provinceName || ('省份' + f.provinceId)
        map[key] = map[key] || { province: key, so2: 0, co: 0, pm25: 0, aqi: 0, total: 0 }
        map[key].total += 1
        if (f.estimatedGrade >= 3) map[key].aqi += 1
      })
      return Object.values(map).sort((a, b) => b.total - a.total)
    } catch (e) {
      return [
        { province: '河北省', so2: 12, co: 8, pm25: 26, aqi: 30 },
        { province: '辽宁省', so2: 9, co: 6, pm25: 18, aqi: 21 },
        { province: '山东省', so2: 7, co: 5, pm25: 15, aqi: 17 },
        { province: '河南省', so2: 6, co: 7, pm25: 13, aqi: 16 },
        { province: '山西省', so2: 11, co: 4, pm25: 20, aqi: 24 },
        { province: '江苏省', so2: 4, co: 3, pm25: 9, aqi: 10 },
        { province: '四川省', so2: 3, co: 2, pm25: 8, aqi: 9 },
        { province: '广东省', so2: 2, co: 1, pm25: 5, aqi: 6 }
      ]
    }
  })
}

// 2. AQI 指数分布统计（按级别分组）
export function getDistributionStats() {
  return tryApi('/stats/distribution', async () => {
    try {
      const res = await getAqiFeedbackList()
      const list = res.data.code === 200 ? (res.data.data || []) : []
      if (!list.length) throw new Error('empty')
      const names = ['', '一级(优)', '二级(良)', '三级(轻度污染)', '四级(中度污染)', '五级(重度污染)', '六级(严重污染)']
      const counts = [0, 0, 0, 0, 0, 0, 0]
      list.forEach(f => counts[f.estimatedGrade >= 0 && f.estimatedGrade <= 6 ? f.estimatedGrade : 0]++)
      return names.slice(1).map((name, i) => ({ name, value: counts[i + 1] }))
    } catch (e) {
      return [
        { name: '一级(优)', value: 152 },
        { name: '二级(良)', value: 264 },
        { name: '三级(轻度污染)', value: 138 },
        { name: '四级(中度污染)', value: 56 },
        { name: '五级(重度污染)', value: 18 },
        { name: '六级(严重污染)', value: 6 }
      ]
    }
  })
}

// 3. 近 12 个月 AQI 超标趋势
export function getTrendStats() {
  return tryApi('/stats/trend', async () => {
    const now = new Date()
    const months = []
    for (let i = 11; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
      months.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
    }
    // 固定形态的演示曲线（后端就绪后替换为真实统计）
    const values = [86, 102, 95, 78, 64, 58, 49, 52, 61, 74, 90, 83]
    return months.map((m, i) => ({ month: m, exceed: values[i] }))
  })
}

// 4. 空气质量检测数量实时统计
export function getRealtimeStats() {
  return tryApi('/stats/realtime', async () => {
    try {
      const res = await getAqiFeedbackList()
      const list = res.data.code === 200 ? (res.data.data || []) : []
      if (list.length) {
        const good = list.filter(f => f.estimatedGrade > 0 && f.estimatedGrade <= 2).length
        return { total: list.length, good, exceed: list.length - good }
      }
    } catch (e) { /* fallthrough */ }
    return { total: 634, good: 416, exceed: 218 }
  })
}

// 5. 全国网格覆盖率统计
export function getCoverageStats() {
  return tryApi('/stats/coverage', async () => {
    const provinces = new Set(COVERED_CITIES.map(c => c.province))
    return {
      provinceCovered: provinces.size,
      provinceTotal: TOTAL_PROVINCES,
      cityCovered: COVERED_CITIES.length,
      cityTotal: TOTAL_BIG_CITIES,
      coveredList: COVERED_CITIES
    }
  })
}
