<template>
  <span class="state-tag" :class="cls">{{ label }}</span>
</template>

<script>
// 反馈任务状态标签：0待指派 1已指派 2待确认 3已确认
export default {
  name: 'StateTag',
  props: {
    state: { type: [Number, String], default: 0 }
  },
  computed: {
    label() {
      return ['待指派', '已指派', '待确认', '已确认'][Number(this.state)] || '未知'
    },
    cls() {
      return 's' + this.state
    }
  }
}
</script>

<style scoped>
.state-tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  border: 1px solid transparent;
}

.state-tag.s0 {
  color: #b45309;
  background: #fffbeb;
  border-color: #fde68a;
}

.state-tag.s1 {
  color: #1d4ed8;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.state-tag.s2 {
  color: #7c3aed;
  background: #f5f3ff;
  border-color: #ddd6fe;
}

.state-tag.s3 {
  color: #047857;
  background: #ecfdf5;
  border-color: #a7f3d0;
}
</style>
