import request, { isNetworkError } from './request'

/**
 * 认证相关接口（对应用例3-1注册、3-2/3-6登录、3-3网格地址）
 * 后端：POST /api/auth/register  POST /api/auth/login  GET/POST /api/auth/profile
 * 后端未连接时降级为本地演示账号，保证页面可独立演示
 */
const USERS_KEY = 'neps_users'

// 预置账号：网格员/管理员/决策者由“东软HR系统”统一管理（本地演示数据，与数据库种子一致）
const PRESET_USERS = [
  { account: 'grid001', password: '123456', role: 'grid', realName: '王铁柱' },
  { account: 'grid002', password: '123456', role: 'grid', realName: '李雪松' },
  { account: 'grid003', password: '123456', role: 'grid', realName: '赵敏' },
  { account: 'admin', password: '123456', role: 'admin', realName: '系统管理员' },
  { account: 'viewer', password: '123456', role: 'viewer', realName: '王决策' }
]

function loadUsers() {
  try {
    return JSON.parse(localStorage.getItem(USERS_KEY)) || []
  } catch (e) {
    return []
  }
}

// 公众监督员注册（用例3-1）
export async function register(data) {
  try {
    const res = await request.post('/auth/register', data)
    if (res.data.code === 200) return { success: true }
    throw new Error(res.data.message || '注册失败')
  } catch (err) {
    if (isNetworkError(err)) {
      // 后端未连接：降级为本地演示注册
      const users = loadUsers()
      if (users.some(u => u.account === data.telId)) {
        throw new Error('该手机号已存在')
      }
      users.push({
        account: data.telId,
        password: data.password,
        role: 'supervisor',
        realName: data.realName,
        age: data.age,
        gender: data.gender
      })
      localStorage.setItem(USERS_KEY, JSON.stringify(users))
      return { success: true, mock: true }
    }
    throw err
  }
}

// 登录（用例3-2/3-6）。监督员用手机号，其余端用登录编码
export async function login(account, password, role) {
  try {
    const res = await request.post('/auth/login', { account, password, role })
    if (res.data.code === 200) return res.data.data
    throw new Error(res.data.message || '登录失败')
  } catch (err) {
    if (isNetworkError(err)) {
      // 后端未连接：降级为本地演示账号
      const all = [...PRESET_USERS, ...loadUsers()]
      const user = all.find(u => u.account === account && u.password === password)
      if (!user) throw new Error('账号或密码错误（后端未连接，仅支持演示账号）')
      if (role && user.role !== role) throw new Error('该账号不属于当前选择的用户类型')
      return { username: user.realName || user.account, account: user.account, role: user.role }
    }
    throw err
  }
}

// 查询监督员档案（含绑定的网格地址）
export async function getProfile(account) {
  try {
    const res = await request.get('/auth/profile', { params: { account } })
    if (res.data.code === 200) return { data: res.data.data, mock: false }
    throw new Error(res.data.message || '查询失败')
  } catch (err) {
    if (isNetworkError(err)) return { data: null, mock: true }
    throw err
  }
}

// 保存监督员绑定的网格地址（用例3-3）
export async function saveProfile(payload) {
  try {
    const res = await request.post('/auth/profile', payload)
    if (res.data.code === 200) return { data: res.data.data, mock: false }
    throw new Error(res.data.message || '保存失败')
  } catch (err) {
    if (isNetworkError(err)) return { data: payload, mock: true }
    throw err
  }
}
