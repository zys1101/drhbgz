import { createStore } from 'vuex'

const savedUserRaw = localStorage.getItem('user')
// 兼容清理：旧版本登录态没有 role 字段，视为未登录
let savedUser = null
if (savedUserRaw) {
  try {
    const parsed = JSON.parse(savedUserRaw)
    if (parsed && parsed.role) savedUser = parsed
  } catch (e) { /* 忽略损坏数据 */ }
}
if (savedUserRaw && !savedUser) {
  localStorage.removeItem('user')
}
const PROFILE_KEY = 'neps_profile' // 公众监督员绑定的网格地址

function loadProfile() {
  try {
    return JSON.parse(localStorage.getItem(PROFILE_KEY)) || null
  } catch (e) {
    return null
  }
}

export default createStore({
  state: {
    user: savedUser ? JSON.parse(savedUser) : null,
    profile: loadProfile()
  },
  getters: {
    isLoggedIn: state => !!state.user,
    role: state => (state.user ? state.user.role : null)
  },
  mutations: {
    setUser(state, user) {
      state.user = user
      if (user) {
        localStorage.setItem('user', JSON.stringify(user))
      } else {
        localStorage.removeItem('user')
      }
    },
    setProfile(state, profile) {
      state.profile = profile
      if (profile) {
        localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
      } else {
        localStorage.removeItem(PROFILE_KEY)
      }
    }
  },
  actions: {
    login({ commit }, user) {
      commit('setUser', { ...user, loginTime: Date.now() })
    },
    logout({ commit }) {
      commit('setUser', null)
    },
    saveProfile({ commit }, profile) {
      commit('setProfile', profile)
    }
  },
  modules: {
  }
})
