import request, { isNetworkError } from './request'
import { getAqiFeedbackList } from './aqiFeedback'

/**
 * 统计数据接口（NEPM 统计数据管理 / NEPV 可视化大屏）
 * 五项统计：省分组超标统计、AQI指数分布、12个月趋势、实时统计、网格覆盖率
 * 两项决策依据：网格员人力与增员需求、反馈覆盖度环比
 * 后端：GET /api/stats/province | /distribution | /trend | /realtime | /coverage
 *       | /workforce | /feedbackCoverage
 * 后端未连接时降级为前端聚合/演示数据
 */

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
    const res = await request.get(url)
    if (res.data.code === 200) {
      return { data: res.data.data, mock: false }
    }
    throw new Error('bad code')
  } catch (err) {
    if (isNetworkError(err)) {
      return { data: await fallback(), mock: true }
    }
    throw err
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
    // 固定形态的演示曲线（后端未连接时使用）
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

/* ===================== 决策者（NEPV）新增的两项决策依据 ===================== */

// 网格员人力与增员需求：后端未连接时的演示数据
const WORKFORCE_MOCK = {
  total: 120,
  working: 108,
  onLeave: 12,
  busy: 41,
  idle: 67,
  capacity: 8,
  pendingTasks: 23,
  pendingDemands: 3,
  suggestAdd: 6,
  needMore: true,
  needReasons: [
    '3 个区域存在未处理的增员请求，共需补充 6 人',
    '大连市、沈阳市待指派任务 23 条，人均在办接近上限 8 条',
    '吉林省长春市在岗网格员仅 4 人，低于区域平均在岗水平'
  ],
  regions: [
    { provinceName: '辽宁省', cityName: '沈阳市', total: 18, working: 15, busy: 9, idle: 6 },
    { provinceName: '辽宁省', cityName: '大连市', total: 15, working: 12, busy: 7, idle: 5 },
    { provinceName: '吉林省', cityName: '长春市', total: 10, working: 7, busy: 5, idle: 2 },
    { provinceName: '黑龙江省', cityName: '哈尔滨市', total: 12, working: 11, busy: 6, idle: 5 },
    { provinceName: '河北省', cityName: '石家庄市', total: 14, working: 12, busy: 4, idle: 8 },
    { provinceName: '北京市', cityName: '北京城区', total: 16, working: 15, busy: 6, idle: 9 },
    { provinceName: '上海市', cityName: '上海城区', total: 13, working: 12, busy: 3, idle: 9 },
    { provinceName: '广东省', cityName: '广州市', total: 12, working: 11, busy: 5, idle: 6 }
  ],
  lackRegions: [
    { provinceName: '吉林省', cityName: '长春市', reason: '在岗网格员 4 人，待指派任务 9 条，人均在办超过上限', afId: 1 },
    { provinceName: '辽宁省', cityName: '大连市', reason: '在岗网格员 12 人中 7 人忙碌，近期反馈量上升 18%', afId: 2 },
    { provinceName: '黑龙江省', cityName: '哈尔滨市', reason: '1 名网格员请假中，覆盖网格出现空缺', afId: 3 }
  ]
}

// 反馈覆盖度环比：后端未连接时的演示数据
const FEEDBACK_COVERAGE_MOCK = {
  currentMonth: '2025-05',
  previousMonth: '2025-04',
  currentTotal: 486,
  previousTotal: 432,
  cities: [
    { cityId: 1, provinceName: '辽宁省', cityName: '大连市', current: 68, previous: 55, delta: 13, deltaPercent: 23.6, working: 12, avgGrade: 1.6, measured: 14, pendingTasks: 9, reason: '反馈增加：本月反馈较上月增加 13 条，公众参与度提升' },
    { cityId: 2, provinceName: '辽宁省', cityName: '沈阳市', current: 61, previous: 58, delta: 3, deltaPercent: 5.2, working: 15, avgGrade: 2.1, measured: 18, pendingTasks: 5, reason: '基本持平：本月与上月反馈量差异在 5% 以内' },
    { cityId: 3, provinceName: '北京市', cityName: '北京城区', current: 57, previous: 49, delta: 8, deltaPercent: 16.3, working: 15, avgGrade: 1.9, measured: 12, pendingTasks: 4, reason: '反馈增加：公众参与度提高，反馈量同步上升' },
    { cityId: 4, provinceName: '广东省', cityName: '广州市', current: 44, previous: 47, delta: -3, deltaPercent: -6.4, working: 11, avgGrade: 1.8, measured: 16, pendingTasks: 2, reason: '基本持平：反馈量小幅波动，整体稳定' },
    { cityId: 5, provinceName: '上海市', cityName: '上海城区', current: 39, previous: 41, delta: -2, deltaPercent: -4.9, working: 12, avgGrade: 1.7, measured: 15, pendingTasks: 1, reason: '基本持平：反馈量小幅波动，整体稳定' },
    { cityId: 6, provinceName: '四川省', cityName: '成都市', current: 31, previous: 33, delta: -2, deltaPercent: -6.1, working: 9, avgGrade: 2.0, measured: 11, pendingTasks: 6, reason: '基本持平：反馈量小幅波动，整体稳定' },
    { cityId: 7, provinceName: '湖北省', cityName: '武汉市', current: 24, previous: 31, delta: -7, deltaPercent: -22.6, working: 10, avgGrade: 2.2, measured: 13, pendingTasks: 7, reason: '反馈减少：本月反馈较上月减少 7 条，公众参与度不足' },
    { cityId: 8, provinceName: '陕西省', cityName: '西安市', current: 19, previous: 22, delta: -3, deltaPercent: -13.6, working: 8, avgGrade: 1.8, measured: 12, pendingTasks: 3, reason: '反馈减少：本月反馈较上月减少 3 条，宣传覆盖不足' },
    { cityId: 9, provinceName: '吉林省', cityName: '长春市', current: 8, previous: 12, delta: -4, deltaPercent: -33.3, working: 0, avgGrade: 0, measured: 0, pendingTasks: 9, reason: '覆盖不足：该区域无在岗网格员，反馈无法被及时受理' },
    { cityId: 10, provinceName: '黑龙江省', cityName: '哈尔滨市', current: 6, previous: 11, delta: -5, deltaPercent: -45.5, working: 2, avgGrade: 1.5, measured: 6, pendingTasks: 5, reason: '反馈减少：在岗网格员不足，反馈受理与回访能力受限' },
    { cityId: 11, provinceName: '河北省', cityName: '石家庄市', current: 5, previous: 6, delta: -1, deltaPercent: -16.7, working: 12, avgGrade: 1.8, measured: 12, pendingTasks: 2, reason: '环境良好：已确认实测平均 AQI 等级 1.8（优/良，样本 12），反馈自然较少' },
    { cityId: 12, provinceName: '山东省', cityName: '青岛市', current: 4, previous: 6, delta: -2, deltaPercent: null, working: 7, avgGrade: 1.9, measured: 9, pendingTasks: 1, reason: '环境良好：已确认实测平均 AQI 等级 1.9（优/良，样本 9），反馈自然较少' }
  ],
  more: [],
  less: []
}

// 榜单结构与 cities 一致，演示数据按后端口径派生：较多按本月降序、较少按本月升序，各取 5 条
FEEDBACK_COVERAGE_MOCK.more = FEEDBACK_COVERAGE_MOCK.cities.slice().sort((a, b) => b.current - a.current).slice(0, 5)
FEEDBACK_COVERAGE_MOCK.less = FEEDBACK_COVERAGE_MOCK.cities.slice().sort((a, b) => a.current - b.current).slice(0, 5)

/**
 * 6. 网格员人力与增员需求（决策依据一）
 * 后端：GET /api/stats/workforce，data 字段含义：
 *   total          网格员总数      working        在岗人数（可派单）
 *   onLeave        非在岗人数（请假中）  busy      忙碌人数（在办任务较多）
 *   idle           空闲人数        capacity       人均在办任务上限
 *   pendingTasks   待指派任务数    pendingDemands 未处理增员请求数
 *   suggestAdd     建议增员人数    needMore       是否需要增员（boolean）
 *   needReasons    需要增员的可读依据（可能为空数组）
 *   regions        各区域人力分布（provinceName/cityName/total/working/busy/idle）
 *   lackRegions    缺员区域（来自未处理的增员请求：provinceName/cityName/reason/afId）
 * 说明：后端业务错误是 HTTP 200 + body.code != 200（不是抛异常），不属于网络异常，
 *      因此这里直接返回 axios 响应，由调用方判断 res.data.code === 200 后再取值。
 */
export function getWorkforce() {
  return request.get('/stats/workforce').catch(err => {
    // 只有后端未启动（网络不可达）时降级为演示数据，业务错误照实抛给调用方处理
    if (isNetworkError(err)) {
      return { data: { code: 200, data: WORKFORCE_MOCK, mock: true } }
    }
    throw err
  })
}

/**
 * 7. 反馈覆盖度环比统计（决策依据二：哪里反馈多、哪里少，以及为什么）
 * 后端：GET /api/stats/feedbackCoverage，data 字段含义：
 *   currentMonth   本月（yyyy-MM）   previousMonth  上月（yyyy-MM）
 *   currentTotal   本月反馈总量      previousTotal  上月反馈总量
 *   cities         各城市环比明细（current/previous/delta/deltaPercent/working/avgGrade/measured/pendingTasks/reason）
 *   more           反馈较多的地区（按本月反馈数降序，最多 5 条）
 *   less           反馈较少的地区（按本月反馈数升序，最多 5 条）
 *   reason         后端给出的中文结论，如「环境良好：…」「覆盖不足：…」「反馈减少：…」
 * 同样直接返回 axios 响应，由调用方判断 res.data.code。
 */
export function getFeedbackCoverage() {
  return request.get('/stats/feedbackCoverage').catch(err => {
    if (isNetworkError(err)) {
      return { data: { code: 200, data: FEEDBACK_COVERAGE_MOCK, mock: true } }
    }
    throw err
  })
}
