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
      resizeObserver: null
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
      this.chart && this.chart.resize()
    })
    this.resizeObserver.observe(this.$refs.chartEl)
  },
  beforeUnmount() {
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
