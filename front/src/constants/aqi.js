// AQI 相关公共常量（依据《空气质量指数（AQI）范围及相应类别表》）

// AQI 等级选项：1-6 级
export const AQI_GRADES = [
  { value: 1, label: '一级（优）', short: '优' },
  { value: 2, label: '二级（良）', short: '良' },
  { value: 3, label: '三级（轻度污染）', short: '轻度污染' },
  { value: 4, label: '四级（中度污染）', short: '中度污染' },
  { value: 5, label: '五级（重度污染）', short: '重度污染' },
  { value: 6, label: '六级（严重污染）', short: '严重污染' }
]

export function gradeText(grade) {
  const map = {
    0: '未评级', 1: '一级（优）', 2: '二级（良）', 3: '三级（轻度污染）',
    4: '四级（中度污染）', 5: '五级（重度污染）', 6: '六级（严重污染）'
  }
  return map[grade] || '未知'
}

export function gradeShort(grade) {
  const item = AQI_GRADES.find(g => g.value === grade)
  return item ? item.short : '未知'
}

// 反馈状态：0未分配(待指派) 1已指派 2处理中 3已处理(已确认)
export const FEEDBACK_STATES = [
  { value: 0, label: '待指派' },
  { value: 1, label: '已指派' },
  { value: 2, label: '检测中' },
  { value: 3, label: '已确认' }
]

export function stateText(state) {
  const item = FEEDBACK_STATES.find(s => s.value === state)
  return item ? item.label : '未知'
}

// 系统角色
export const ROLES = {
  supervisor: { key: 'supervisor', label: '公众监督员', home: '/sf/submit' },
  grid: { key: 'grid', label: 'AQI检测网格员', home: '/gw/tasks' },
  admin: { key: 'admin', label: '系统管理员', home: '/' },
  viewer: { key: 'viewer', label: '决策者', home: '/screen' }
}

export function roleLabel(role) {
  return ROLES[role] ? ROLES[role].label : '未知角色'
}

export function roleHome(role) {
  return ROLES[role] ? ROLES[role].home : '/login'
}

// AQI = MAX(SO2AQI, COAQI, PM2.5AQI)
export function calcAqiGrade(so2, co, pm25) {
  const vals = [so2, co, pm25].filter(v => v > 0)
  if (vals.length < 3) return 0
  return Math.max(...vals)
}
