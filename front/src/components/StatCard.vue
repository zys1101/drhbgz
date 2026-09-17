<template>
  <div class="stat-card" :style="{ '--accent': color }">
    <div class="stat-icon">
      <i :class="icon"></i>
    </div>
    <div class="stat-meta">
      <span class="stat-value">{{ value }}</span>
      <span class="stat-label">{{ label }}</span>
    </div>
    <i v-if="deco" class="deco" :class="deco"></i>
  </div>
</template>

<script>
// KPI 统计卡片（首页/统计页共用）
export default {
  name: 'StatCard',
  props: {
    icon: { type: String, required: true },       // font-awesome 类名
    label: { type: String, required: true },
    value: { type: [Number, String], default: '—' },
    color: { type: String, default: '#10b981' },
    deco: { type: String, default: '' }           // 背景装饰图标（可选）
  }
}
</script>

<style scoped>
.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border: 1px solid var(--nep-border, #e8eef0);
  border-radius: 14px;
  padding: 18px 20px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04), 0 4px 16px rgba(15, 23, 42, 0.06);
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.10);
}

.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: var(--accent);
  background: color-mix(in srgb, var(--accent) 12%, #ffffff);
  flex-shrink: 0;
}

.stat-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 12.5px;
  color: #94a3b8;
}

.deco {
  position: absolute;
  right: -8px;
  bottom: -12px;
  font-size: 64px;
  color: var(--accent);
  opacity: 0.07;
  pointer-events: none;
}
</style>
