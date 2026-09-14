<template>
  <div class="layout">
    <!-- ========== 侧边栏 ========== -->
    <aside class="sidebar">
      <div class="brand" @click="$router.push(roleHome(role))">
        <div class="brand-logo"><i class="fa-solid fa-leaf"></i></div>
        <div class="brand-text">
          <span class="brand-name">东软环保</span>
          <span class="brand-sub">公众监督系统</span>
        </div>
      </div>

      <nav class="menu">
        <div v-for="group in menuGroups" :key="group.caption" class="menu-group">
          <div class="menu-caption">{{ group.caption }}</div>
          <router-link
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="menu-item"
            :class="{ active: isActive(item.path) }"
          >
            <i :class="item.icon"></i>
            <span>{{ item.title }}</span>
          </router-link>
        </div>
      </nav>

      <div class="sidebar-foot">
        <div class="user-chip">
          <div class="avatar">{{ avatarChar }}</div>
          <div class="user-meta">
            <span class="user-name">{{ username }}</span>
            <span class="user-role">{{ roleText }}</span>
          </div>
          <button class="logout-btn" title="退出登录" @click="handleLogout">
            <i class="fa-solid fa-right-from-bracket"></i>
          </button>
        </div>
      </div>
    </aside>

    <!-- ========== 主区域 ========== -->
    <div class="main">
      <header class="topbar">
        <div class="topbar-title">
          <span class="crumb">{{ roleText }}</span>
          <i class="fa-solid fa-angle-right crumb-sep"></i>
          <span class="crumb-current">{{ pageTitle }}</span>
        </div>
        <div class="topbar-actions">
          <el-tooltip content="可视化大屏" placement="bottom" v-if="role === 'admin' || role === 'viewer'">
            <button class="icon-btn" @click="$router.push('/screen')">
              <i class="fa-solid fa-tv"></i>
            </button>
          </el-tooltip>
          <el-tooltip content="退出登录" placement="bottom">
            <button class="icon-btn" @click="handleLogout"><i class="fa-solid fa-arrow-right-from-bracket"></i></button>
          </el-tooltip>
        </div>
      </header>

      <main class="content">
        <slot />
      </main>
    </div>
    <!-- AI 助手（后台端：管理员/决策者角色助手） -->
    <AiAssistant />
  </div>
</template>

<script>
import { ElMessageBox } from 'element-plus'
import AiAssistant from '../components/AiAssistant.vue'
import { roleLabel, roleHome } from '../constants/aqi'

// 后台角色（管理员 / 决策者）的侧边栏菜单
const MENUS = {
  admin: [
    {
      caption: '总览',
      items: [
        { path: '/', icon: 'fa-solid fa-gauge-high', title: '首页看板' }
      ]
    },
    {
      caption: '公众监督数据管理',
      items: [
        { path: '/admin/feedback', icon: 'fa-solid fa-inbox', title: '监督数据列表' },
        { path: '/admin/aqiData', icon: 'fa-solid fa-flask-vial', title: '确认AQI数据' }
      ]
    },
    {
      caption: '统计数据管理',
      items: [
        { path: '/admin/stats', icon: 'fa-solid fa-chart-column', title: '统计报表' }
      ]
    },
    {
      caption: '人员管理（HR）',
      items: [
        { path: '/admin/hr/grid', icon: 'fa-solid fa-user-gear', title: '网格员管理' },
        { path: '/admin/hr/supervisor', icon: 'fa-solid fa-users', title: '公众监督员管理' }
      ]
    },
    {
      caption: '基础数据',
      items: [
        { path: '/aqi', icon: 'fa-solid fa-table-cells', title: 'AQI级别管理' }
      ]
    }
  ],
  viewer: [
    {
      caption: '决策支持',
      items: [
        { path: '/screen', icon: 'fa-solid fa-tv', title: '可视化大屏' }
      ]
    }
  ]
}

export default {
  name: 'BackofficeLayout',
  components: { AiAssistant },
  computed: {
    menuGroups() {
      return MENUS[this.$store.getters.role] || []
    },
    role() {
      return this.$store.getters.role
    },
    roleText() {
      return roleLabel(this.role)
    },
    roleHome() {
      return roleHome(this.role)
    },
    username() {
      return this.$store.state.user ? this.$store.state.user.username : ''
    },
    avatarChar() {
      return this.username ? this.username.charAt(0).toUpperCase() : '?'
    },
    pageTitle() {
      const all = this.menuGroups.flatMap(g => g.items)
      const hit = all.find(i => i.path === this.$route.path)
      return hit ? hit.title : '工作台'
    }
  },
  methods: {
    isActive(path) {
      return this.$route.path === path
    },
    handleLogout() {
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          this.$store.dispatch('logout')
          this.$router.push({ name: 'login' })
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
}

/* ---------------- 侧边栏 ---------------- */
.sidebar {
  width: 232px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #052e22 0%, #0a3d2e 55%, #0d4a37 100%);
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 20px 18px 18px;
  cursor: pointer;
}

.brand-logo {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #34d399, #059669);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 17px;
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.45);
  flex-shrink: 0;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.brand-name {
  color: #fff;
  font-size: 15.5px;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-sub {
  color: rgba(255, 255, 255, 0.55);
  font-size: 11px;
  letter-spacing: 0.5px;
}

.menu {
  flex: 1;
  padding: 4px 12px 12px;
}

.menu-caption {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.38);
  padding: 14px 10px 6px;
  letter-spacing: 1px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 10.5px 12px;
  margin-bottom: 3px;
  border-radius: 10px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 13.5px;
  text-decoration: none;
  transition: all 0.18s ease;
  position: relative;
}

.menu-item i {
  width: 17px;
  text-align: center;
  font-size: 13.5px;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.menu-item.active {
  background: linear-gradient(135deg, rgba(52, 211, 153, 0.24), rgba(5, 150, 105, 0.32));
  color: #ffffff;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px rgba(52, 211, 153, 0.35);
}

.menu-item.active::before {
  content: '';
  position: absolute;
  left: -12px;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 20px;
  border-radius: 0 4px 4px 0;
  background: #34d399;
}

.sidebar-foot {
  padding: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  padding: 10px 12px;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #34d399, #0d9488);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 15px;
  flex-shrink: 0;
}

.user-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.user-name {
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  color: rgba(255, 255, 255, 0.5);
  font-size: 11px;
}

.logout-btn {
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.55);
  cursor: pointer;
  font-size: 14px;
  padding: 6px;
  border-radius: 8px;
  transition: all 0.18s;
}

.logout-btn:hover {
  color: #fda4af;
  background: rgba(255, 255, 255, 0.08);
}

/* ---------------- 主区域 ---------------- */
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 60px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--nep-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.topbar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.crumb {
  color: var(--nep-muted);
}

.crumb-sep {
  color: #cbd5e1;
  font-size: 11px;
}

.crumb-current {
  color: var(--nep-ink);
  font-weight: 600;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid var(--nep-border);
  background: #fff;
  color: #64748b;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.18s;
}

.icon-btn:hover {
  color: #059669;
  border-color: #a7f3d0;
  background: #ecfdf5;
}

.content {
  flex: 1;
  padding: 20px 24px 30px;
}

@media (max-width: 860px) {
  .sidebar {
    width: 68px;
  }

  .brand-text,
  .menu-caption,
  .menu-item span,
  .user-meta {
    display: none;
  }

  .menu-item {
    justify-content: center;
  }
}
</style>
