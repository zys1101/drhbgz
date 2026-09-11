<template>
  <div ref="chartEl" class="v-chart" :style="{ height }"></div>
</template>

<script>
import * as echarts from 'echarts'

export default {
  name: 'VChart',
  props: {
    option: {
      type: Object,
      required: true
    },
    height: {
      type: String,
      default: '320px'
    }
  },
  data() {
    return {
      chart: null,
      resizeObserver: null,
      resizeRaf: null
    }
  },
  watch: {
    option: {
      deep: true,
      handler(val) {
        if (this.chart) {
          this.chart.setOption(val, true)
        }
      }
    }
  },
  mounted() {
    this.chart = echarts.init(this.$refs.chartEl)
    this.chart.setOption(this.option)
    this.resizeObserver = new ResizeObserver(() => {
      // 推迟到下一帧再 resize：避免在同一帧内因布局变化再次触发观察，
      // 造成 "ResizeObserver loop completed with undelivered notifications" 运行时报错
      if (this.resizeRaf) return
      this.resizeRaf = requestAnimationFrame(() => {
        this.resizeRaf = null
        const el = this.$refs.chartEl
        if (this.chart && el && el.clientWidth > 0 && el.clientHeight > 0) {
          this.chart.resize()
        }
      })
    })
    this.resizeObserver.observe(this.$refs.chartEl)
  },
  beforeUnmount() {
    if (this.resizeRaf) cancelAnimationFrame(this.resizeRaf)
    if (this.resizeObserver) {
      this.resizeObserver.disconnect()
    }
    if (this.chart) {
      this.chart.dispose()
      this.chart = null
    }
  }
}
</script>

<style scoped>
.v-chart {
  width: 100%;
}
</style>
