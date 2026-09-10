<template>
  <div class="app-shell" v-if="showLayout">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-icon">🌿</span>
        <div class="brand-text">
          <span class="brand-name">空气质量监测</span>
          <span class="brand-sub">{{ roleText }}端</span>
        </div>
      </div>

      <nav class="side-nav">
        <router-link
          v-for="menu in menus"
          :key="menu.path"
          :to="menu.path"
        >{{ menu.icon }} <span>{{ menu.title }}</span></router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="user-chip">
          <span class="user-avatar">{{ avatarChar }}</span>
          <div class="user-meta">
            <span class="user-name">{{ username }}</span>
            <span class="user-role">{{ roleText }}</span>
          </div>
        </div>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </aside>

    <main class="main-content">
      <router-view/>
    </main>
  </div>

  <router-view v-else/>
</template>

<script>
import { roleLabel, ROLES } from './constants/aqi'

// 各角色侧边栏菜单
const MENUS = {
  supervisor: [
    { path: '/sf/address', icon: '📍', title: '网格地址' },
    { path: '/sf/submit', icon: '📝', title: '提交反馈' },
    { path: '/sf/history', icon: '🕘', title: '历史反馈' }
  ],
  grid: [
    { path: '/gw/tasks', icon: '📋', title: '我的任务' }
  ],
  admin: [
    { path: '/', icon: '🏠', title: '首页看板' },
    { path: '/admin/feedback', icon: '📝', title: '公众监督数据' },
    { path: '/admin/aqiData', icon: '🧪', title: '确认AQI数据' },
    { path: '/admin/stats', icon: '📈', title: '统计数据管理' },
    { path: '/aqi', icon: '📊', title: 'AQI级别管理' },
    { path: '/screen', icon: '🖥️', title: '可视化大屏' }
  ],
  viewer: [
    { path: '/screen', icon: '🖥️', title: '可视化大屏' }
  ]
}

export default {
  computed: {
    showLayout() {
      const name = this.$route.name
      return name !== 'login' && name !== 'register' && !this.$route.meta.fullscreen
    },
    menus() {
      return MENUS[this.$store.getters.role] || []
    },
    username() {
      return this.$store.state.user ? this.$store.state.user.username : ''
    },
    roleText() {
      return roleLabel(this.$store.getters.role)
    },
    avatarChar() {
      return this.username ? this.username.charAt(0).toUpperCase() : '?'
    }
  },
  methods: {
    handleLogout() {
      this.$store.dispatch('logout')
      this.$router.push({ name: 'login' })
    }
  }
}
</script>

<style>
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  font-family: Avenir, Helvetica, Arial, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  background: #f0f4f5;
}

.app-shell {
  display: flex;
  min-height: 100vh;
}

/* ---------- 侧边栏 ---------- */
.sidebar {
  width: 230px;
  flex-shrink: 0;
  background: linear-gradient(180deg, #2c3e50 0%, #22313f 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  height: 100vh;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-icon {
  font-size: 26px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.brand-name {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-sub {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
  letter-spacing: 0.5px;
}

.side-nav {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.side-nav a {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.65);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.side-nav a:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.side-nav a.router-link-active {
  color: #fff;
  background: linear-gradient(135deg, #42b983, #2e8565);
  box-shadow: 0 4px 12px rgba(66, 185, 131, 0.35);
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #42b983;
  color: #fff;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  flex-shrink: 0;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.user-name {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

.user-role {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
}

.logout-btn {
  width: 100%;
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  color: #fff;
  border-color: #f56c6c;
  background: rgba(245, 108, 108, 0.15);
}

/* ---------- 主内容区 ---------- */
.main-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.main-content > * {
  flex: 1;
}
</style>
