<template>
  <div class="app-root" :class="'app-layout--' + roleKey">
    <!-- ========== 顶部胶囊导航：默认透明融入页面，滚动后变白色毛玻璃胶囊（deepseek 风格） ========== -->
    <header class="app-header" :class="{ 'is-scrolled': isScrolled }">
      <div class="header-bar">
        <div class="brand" @click="$router.push(roleHome)">
          <span class="brand-logo"><i class="fa-solid fa-leaf"></i></span>
          <span class="brand-text">
            <b>东软环保</b>
            <small>公众监督系统</small>
          </span>
        </div>

        <nav class="app-nav" aria-label="主导航">
          <router-link
            v-for="tab in tabs"
            :key="tab.path"
            :to="tab.path"
            class="nav-item"
            :class="{ 'is-active': isActive(tab.path) }"
          >
            <i :class="tab.icon"></i>
            <span>{{ tab.title }}</span>
          </router-link>
        </nav>

        <button class="avatar-btn" title="退出登录" @click="handleLogout">
          <span class="avatar">{{ avatarChar }}</span>
        </button>
      </div>
    </header>

    <!-- ========== 内容区 ========== -->
    <main class="app-content">
      <!-- 欢迎横幅：光标反色光斑只在这个区域内出现，页面其他位置不显示 -->
      <section ref="heroRef" class="app-hero" @mousemove="onHeroMove" @mouseleave="onHeroLeave">
        <div class="hero-text">
          <div class="hero-title">{{ greetTitle }}</div>
          <div class="hero-sub">{{ greetSub }}</div>
        </div>
        <div class="hero-side">
          <span class="hero-badge"><i :class="roleIcon"></i></span>
          <span class="hero-date">{{ todayText }}</span>
        </div>
        <!-- 光斑画布：只覆盖 hero 区域，mix-blend-mode: difference 让白色渐变圆
             在此区域内呈现为黑色圆形跟随鼠标（deepseek 首页同款，仅限横幅） -->
        <canvas ref="cursorCanvas" class="cursor-canvas" aria-hidden="true"></canvas>
      </section>

      <slot />
    </main>
    <!-- AI 助手（公众端：网格员/公众监督员各自角色助手） -->
    <AiAssistant />
  </div>
</template>

<script>
import { ElMessageBox } from 'element-plus'
import AiAssistant from '../components/AiAssistant.vue'
import { getProfile } from '../api/auth'
import { roleHome } from '../constants/aqi'

// 两个公众端的导航项（≤5 项，图标 + 文字）
const TABS = {
  supervisor: [
    { path: '/sf/submit', icon: 'fa-solid fa-pen-to-square', title: '提交反馈' },
    { path: '/sf/history', icon: 'fa-solid fa-clock-rotate-left', title: '历史反馈' },
    { path: '/sf/address', icon: 'fa-solid fa-location-dot', title: '网格地址' }
  ],
  grid: [
    { path: '/gw/tasks', icon: 'fa-solid fa-clipboard-list', title: '我的任务' },
    { path: '/gw/measure', icon: 'fa-solid fa-vials', title: '录入实测' },
    { path: '/gw/leave', icon: 'fa-solid fa-calendar-days', title: '请假申请' }
  ]
}

const GREET = {
  supervisor: {
    title: name => `你好，${name}`,
    sub: () => '发现异常空气，随手提交一份监督反馈，为蓝天出一份力',
    icon: 'fa-solid fa-pen-to-square'
  },
  grid: {
    title: () => '网格员工作台',
    sub: name => `${name}，收到新任务请及时实地检测`,
    icon: 'fa-solid fa-vials'
  }
}

export default {
  name: 'AppLayout',
  components: { AiAssistant },
  data() {
    return {
      isScrolled: false,
      // 光标在欢迎横幅内的位置（null = 鼠标不在横幅内）
      cursorTarget: null
    }
  },
  computed: {
    roleKey() {
      return this.$store.getters.role
    },
    tabs() {
      return TABS[this.roleKey] || []
    },
    username() {
      return this.$store.state.user ? this.$store.state.user.username : ''
    },
    avatarChar() {
      return this.username ? this.username.charAt(0).toUpperCase() : '?'
    },
    roleHome() {
      return roleHome(this.roleKey)
    },
    greetTitle() {
      const g = GREET[this.roleKey]
      return g ? g.title(this.username) : '欢迎回来'
    },
    greetSub() {
      const g = GREET[this.roleKey]
      return g ? g.sub(this.username) : ''
    },
    roleIcon() {
      const g = GREET[this.roleKey]
      return g ? g.icon : 'fa-solid fa-leaf'
    },
    todayText() {
      const d = new Date()
      const pad = n => String(n).padStart(2, '0')
      const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
      return `${d.getFullYear()}年${pad(d.getMonth() + 1)}月${pad(d.getDate())}日 · 星期${week}`
    }
  },
  mounted() {
    window.addEventListener('scroll', this.onScroll, { passive: true })
    this.onScroll()
    this.$nextTick(() => this.initCursor())
    this.ensureSupervisorProfile()
  },
  beforeUnmount() {
    window.removeEventListener('scroll', this.onScroll)
  },
  methods: {
    isActive(path) {
      return this.$route.path === path
    },
    // 滞回处理：超过阈值才折叠，回到顶部内才展开，避免在临界处来回抖动
    onScroll() {
      const y = window.scrollY
      if (y > 36) {
        this.isScrolled = true
      } else if (y < 16) {
        this.isScrolled = false
      }
    },
    // 光标反色光斑只覆盖欢迎横幅区域（deepseek 同款 mix-blend-mode: difference）
    onHeroMove(e) {
      const hero = this.$refs.heroRef
      if (!hero) return
      const rect = hero.getBoundingClientRect()
      this.cursorTarget = { x: e.clientX - rect.left, y: e.clientY - rect.top }
    },
    onHeroLeave() {
      this.cursorTarget = null
    },
    initCursor() {
      const canvas = this.$refs.cursorCanvas
      const hero = this.$refs.heroRef
      if (!canvas || !hero) return
      const ctx = canvas.getContext('2d')
      const dpr = Math.min(window.devicePixelRatio || 1, 2)
      const R = 300 // 光斑直径，与横幅高度相当
      let w = 0
      let h = 0

      const resize = () => {
        w = hero.offsetWidth
        h = hero.offsetHeight
        canvas.width = w * dpr
        canvas.height = h * dpr
        canvas.style.width = w + 'px'
        canvas.style.height = h + 'px'
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      }
      resize()
      const ro = new ResizeObserver(resize)
      ro.observe(hero)

      let x = -9999
      let y = -9999
      const tick = () => {
        // 每帧清空；光标在横幅内才绘制（鼠标离开横幅时不留残影）
        ctx.clearRect(0, 0, w, h)
        const t = this.cursorTarget
        if (t) {
          // 惯性跟随：向鼠标位置柔和靠近
          x += (t.x - x) * 0.1
          y += (t.y - y) * 0.1
          const g = ctx.createRadialGradient(x, y, 0, x, y, R / 2)
          g.addColorStop(0, 'rgba(255,255,255,0.9)')
          g.addColorStop(0.35, 'rgba(255,255,255,0.4)')
          g.addColorStop(1, 'rgba(255,255,255,0)')
          ctx.fillStyle = g
          ctx.beginPath()
          ctx.arc(x, y, R / 2, 0, Math.PI * 2)
          ctx.fill()
        }
        requestAnimationFrame(tick)
      }
      tick()
    },
    /**
     * 公众监督员：进入监督员端时补载“绑定的网格地址”。
     * 该档案此前只在“网格地址”页读取并缓存在 localStorage，一旦换浏览器/清缓存
     * （自动化测试每次登录都会清 localStorage），已绑定地址的监督员会被误判为
     * “未绑定地址”，导致提交反馈页不可用。这里按后端档案补齐。
     */
    async ensureSupervisorProfile() {
      if (this.roleKey !== 'supervisor') return
      if (this.$store.state.profile) return
      const account = this.$store.state.user ? this.$store.state.user.account : ''
      if (!account) return
      try {
        // 注意：getProfile 已解包，返回 { data: <档案对象>, mock }
        const res = await getProfile(account)
        const sup = res && res.data
        if (sup && sup.provinceId) {
          this.$store.dispatch('saveProfile', sup)
        }
      } catch (e) {
        // 后端未连接时保持“未绑定地址”引导态
      }
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
.app-root {
  min-height: 100vh;
  background: var(--app-bg);
  display: flex;
  flex-direction: column;
}

/* ---------------- 顶部胶囊导航 ---------------- */
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 50;
  display: flex;
  justify-content: center;
  padding: 16px 24px 0;
}

/* 胶囊条本体：始终圆角胶囊，默认透明；滚动后浮现白色毛玻璃 */
.header-bar {
  width: 100%;
  max-width: 1280px;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 8px 22px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: transparent;
  transition: background-color 0.35s ease, border-color 0.35s ease,
    box-shadow 0.35s ease, backdrop-filter 0.35s ease;
}

.app-header.is-scrolled .header-bar {
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-color: rgba(226, 232, 240, 0.9);
  box-shadow: 0 12px 36px rgba(6, 78, 59, 0.14);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  cursor: pointer;
}

.brand-logo {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--app-grad);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  box-shadow: 0 4px 12px rgba(4, 120, 87, 0.25);
  flex-shrink: 0;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.brand-text b {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 1px;
}

.brand-text small {
  font-size: 11px;
  color: #64748b;
}

.app-nav {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 18px;
  border-radius: 999px;
  color: #475569;
  font-size: 14px;
  text-decoration: none;
  transition: all 0.2s ease;
}

.nav-item i {
  font-size: 13.5px;
}

.nav-item:hover {
  background: rgba(15, 23, 42, 0.06);
  color: var(--app-strong);
}

.nav-item.is-active {
  background: var(--app-soft);
  color: var(--app-strong);
  font-weight: 700;
}

.avatar-btn {
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.1);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}

.avatar-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.16);
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--app-grad);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
}

/* ---------------- 内容区 ---------------- */
.app-content {
  flex: 1;
  width: 100%;
  padding: 96px 24px 48px;
}

/* 欢迎横幅（deepseek 首页 slogan 风格）；position: relative 供光斑画布定位 */
.app-hero {
  position: relative;
  max-width: 1240px;
  margin: 0 auto 26px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 30px 40px;
  background: linear-gradient(135deg, #ffffff 0%, var(--app-soft) 100%);
  border: 1px solid var(--app-ring);
  border-radius: 26px;
  box-shadow: 0 12px 34px rgba(6, 78, 59, 0.08);
}

.hero-text {
  min-width: 0;
}

.hero-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--app-strong);
  letter-spacing: 0.5px;
}

.hero-sub {
  margin-top: 8px;
  font-size: 14px;
  color: #64748b;
}

.hero-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.hero-badge {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--app-grad);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  box-shadow: 0 10px 24px rgba(4, 120, 87, 0.3);
}

.hero-date {
  font-size: 12px;
  color: #94a3b8;
}

/* 光标反色光斑画布：仅覆盖欢迎横幅区域，其余页面不受影响 */
.cursor-canvas {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  z-index: 2;
  pointer-events: none;
  display: block;
  mix-blend-mode: difference;
}

/* 触屏 / 无精确指针设备不显示 */
@media (hover: none), (pointer: coarse) {
  .cursor-canvas {
    display: none;
  }
}

/* ---------------- 移动端：导航转为底部悬浮胶囊（保留，不影响电脑演示） ---------------- */
@media (max-width: 768px) {
  .app-header {
    padding: 12px 14px 0;
  }

  .header-bar {
    gap: 10px;
    padding: 7px 14px;
  }

  .brand-text {
    display: none;
  }

  .app-nav {
    position: fixed;
    left: 50%;
    bottom: 18px;
    transform: translateX(-50%);
    z-index: 60;
    width: min(520px, calc(100% - 28px));
    margin: 0;
    padding: 8px 10px;
    background: rgba(255, 255, 255, 0.96);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    border: 1px solid rgba(255, 255, 255, 0.6);
    border-radius: 24px;
    box-shadow: 0 10px 34px rgba(6, 78, 59, 0.18);
    gap: 4px;
  }

  .nav-item {
    flex: 1;
    flex-direction: column;
    gap: 4px;
    padding: 8px 4px 7px;
    border-radius: 16px;
    font-size: 11px;
    color: #94a3b8;
  }

  .nav-item i {
    font-size: 17px;
    line-height: 1;
  }

  .nav-item:hover {
    background: transparent;
    color: #94a3b8;
  }

  .nav-item.is-active {
    background: var(--app-soft);
    color: var(--app-strong);
  }

  .app-content {
    padding: 80px 14px 110px;
  }

  .app-hero {
    padding: 22px 24px;
    border-radius: 20px;
  }

  .hero-title {
    font-size: 21px;
  }

  .hero-badge {
    width: 52px;
    height: 52px;
    font-size: 20px;
  }
}

/* 尊重系统减弱动效偏好 */
@media (prefers-reduced-motion: reduce) {
  .header-bar {
    transition: none;
  }
}
</style>
