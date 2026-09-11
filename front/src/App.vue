<template>
  <!-- 后台端（管理员 / 决策者）：侧边栏 + 顶栏布局 -->
  <BackofficeLayout v-if="showLayout && isBackoffice">
    <router-view v-slot="{ Component }">
      <transition name="fade-slide" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </BackofficeLayout>

  <!-- 公众端（公众监督员 / 网格员）：App 风格布局 -->
  <AppLayout v-else-if="showLayout">
    <router-view v-slot="{ Component }">
      <transition name="fade-slide" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </AppLayout>

  <!-- 全屏 / 登录页：不套布局 -->
  <router-view v-else />
</template>

<script>
import BackofficeLayout from './layouts/BackofficeLayout.vue'
import AppLayout from './layouts/AppLayout.vue'

export default {
  name: 'App',
  components: { BackofficeLayout, AppLayout },
  computed: {
    showLayout() {
      const name = this.$route.name
      return name !== 'login' && name !== 'register' && !this.$route.meta.fullscreen
    },
    role() {
      return this.$store.getters.role
    },
    // 管理 / 决策者走后台布局；监督员 / 网格员走公众 App 布局
    isBackoffice() {
      return this.role === 'admin' || this.role === 'viewer'
    }
  }
}
</script>
