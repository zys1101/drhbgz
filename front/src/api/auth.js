import axios from 'axios'

// 认证相关接口。
// TODO: 后端就绪后，将 mock 部分替换为真实接口调用（/auth/login、/auth/register）。
const request = axios.create({
  baseURL: 'http://localhost:9000/',
  timeout: 3000
})

const USERS_KEY = 'neps_users'

// 预置账号：网格员/管理员/决策者由"东软HR系统"统一管理（此处为演示数据）
const PRESET_USERS = [
  { account: 'grid001', password: '123456', role: 'grid', realName: '王网格' },
  { account: 'admin', password: '123456', role: 'admin', realName: '系统管理员' },
  { account: 'viewer', password: '123456', role: 'viewer', realName: '决策者' }
]

function loadUsers() {
  try {
    return JSON.parse(localStorage.getItem(USERS_KEY)) || []
  } catch (e) {
    return []
  }
}

function saveUsers(users) {
  localStorage.setItem(USERS_KEY, JSON.stringify(users))
}

// 公众监督员注册（用例 3-1）
export async function register(data) {
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
  saveUsers(users)
  return { success: true }
}

// 登录（用例 3-2 / 3-6）。监督员用手机号，其余端用登录编码
export async function login(account, password, role) {
  const all = [...PRESET_USERS, ...loadUsers()]
  const user = all.find(u => u.account === account && u.password === password)
  if (!user) {
    throw new Error('账号或密码错误')
  }
  if (role && user.role !== role) {
    throw new Error('该账号不属于当前选择的用户类型')
  }
  return {
    username: user.realName || user.account,
    account: user.account,
    role: user.role
  }
}
