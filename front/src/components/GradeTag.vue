<template>
  <span class="grade-tag" :style="style">
    <span class="dot" :style="{ background: color }"></span>{{ label }}
  </span>
</template>

<script>
// AQI 等级标签（依据《空气质量指数（AQI）范围及相应类别表》配色）
const GRADES = {
  0: { label: '未评级', color: '#94a3b8', bg: '#f1f5f9' },
  1: { label: '一级·优', color: '#16a34a', bg: '#f0fdf4' },
  2: { label: '二级·良', color: '#65a30d', bg: '#f7fee7' },
  3: { label: '三级·轻度', color: '#d97706', bg: '#fffbeb' },
  4: { label: '四级·中度', color: '#dc2626', bg: '#fef2f2' },
  5: { label: '五级·重度', color: '#be123c', bg: '#fff1f2' },
  6: { label: '六级·严重', color: '#7f1d1d', bg: '#fde8e8' }
}

export default {
  name: 'GradeTag',
  props: {
    grade: { type: [Number, String], default: 0 },
    plain: { type: Boolean, default: false } // plain=true 时只显示文字（如"优/良"）
  },
  computed: {
    info() {
      return GRADES[Number(this.grade)] || GRADES[0]
    },
    label() {
      const g = Number(this.grade)
      if (this.plain) {
        return ['未知', '优', '良', '轻度污染', '中度污染', '重度污染', '严重污染'][g] || '未知'
      }
      return this.info.label
    },
    color() {
      return this.info.color
    },
    style() {
      return { color: this.info.color, background: this.info.bg, borderColor: this.info.bg }
    }
  }
}
</script>

<style scoped>
.grade-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
  white-space: nowrap;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
