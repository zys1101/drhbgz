<template>
  <div class="list-page">
    <transition name="toast">
      <div v-if="toast.show" class="toast" :class="toast.type">
        <span class="toast-icon">{{ toastIcon }}</span>
        <span class="toast-msg">{{ toast.message }}</span>
      </div>
    </transition>

    <div class="list-card">
      <div class="list-header">
        <h2 class="list-title">空气质量指数级别管理</h2>
        <button class="btn btn-primary" @click="$router.push('/aqiForm')">+ 新增级别</button>
      </div>

      <div v-if="loading" class="loading-wrap">
        <div class="spinner"></div>
        <span class="loading-text">数据加载中...</span>
      </div>

      <div v-else class="table-wrap">
        <table class="aqi-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>级别</th>
              <th>描述</th>
              <th>颜色</th>
              <th>SO₂ 范围</th>
              <th>CO 范围</th>
              <th>SPM 范围</th>
              <th>健康影响</th>
              <th>建议措施</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in list" :key="item.aqiId">
              <td>{{ item.aqiId }}</td>
              <td>
                <span class="grade-tag" :style="{ background: item.color, color: getContrastColor(item.color) }">
                  {{ item.chineseExplain }}
                </span>
              </td>
              <td>{{ item.aqiExplain }}</td>
              <td>
                <span class="color-cell">
                  <span class="color-block" :style="{ background: item.color }"></span>
                  <span class="color-value">{{ item.color }}</span>
                </span>
              </td>
              <td>{{ item.so2Min }} ~ {{ item.so2Max }}</td>
              <td>{{ item.coMin }} ~ {{ item.coMax }}</td>
              <td>{{ item.spmMin }} ~ {{ item.spmMax }}</td>
              <td class="text-ellipsis" :title="item.healthImpact">{{ item.healthImpact }}</td>
              <td class="text-ellipsis" :title="item.takeSteps">{{ item.takeSteps }}</td>
              <td class="action-cell">
                <button class="link-btn edit" @click="$router.push('/aqiForm?aqiId=' + item.aqiId)">修改</button>
                <button class="link-btn delete" @click="handleDelete(item.aqiId)">删除</button>
              </td>
            </tr>
            <tr v-if="list.length === 0">
              <td colspan="10" class="empty-cell">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
import { getAqiList, deleteAqi } from '../api/aqi.js'

export default {
  name: 'AqiList',
  data() {
    return {
      list: [],
      loading: true,
      toast: {
        show: false,
        type: 'success',
        message: ''
      },
      toastTimer: null
    }
  },
  computed: {
    toastIcon() {
      const map = { success: '✓', error: '✕', warning: '!', info: 'i' }
      return map[this.toast.type] || 'i'
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    showToast(type, message) {
      this.toast = { show: true, type, message }
      if (this.toastTimer) clearTimeout(this.toastTimer)
      this.toastTimer = setTimeout(() => {
        this.toast.show = false
      }, 2500)
    },

    async fetchList() {
      this.loading = true
      try {
        const res = await getAqiList()
        if (res.data.code === 200) {
          this.list = res.data.data || []
        } else {
          // 后端暂无级别接口时，使用国家标准AQI级别表作为演示数据
          this.list = this.demoLevels()
          this.showToast('warning', '后端级别接口未连接，当前为标准AQI级别演示数据')
        }
      } catch (err) {
        this.list = this.demoLevels()
        this.showToast('warning', '后端未连接，当前为标准AQI级别演示数据')
      } finally {
        this.loading = false
      }
    },

    demoLevels() {
      return [
        { aqiId: 1, chineseExplain: '一级', aqiExplain: '优', color: '#00e400', so2Min: 0, so2Max: 150, coMin: 0, coMax: 2, spmMin: 0, spmMax: 50, healthImpact: '空气质量令人满意，基本无空气污染', takeSteps: '各类人群可正常活动' },
        { aqiId: 2, chineseExplain: '二级', aqiExplain: '良', color: '#ffff00', so2Min: 151, so2Max: 500, coMin: 3, coMax: 4, spmMin: 51, spmMax: 100, healthImpact: '空气质量可接受，但某些污染物可能对极少数异常敏感人群健康有较弱影响', takeSteps: '极少数异常敏感人群应减少户外活动' },
        { aqiId: 3, chineseExplain: '三级', aqiExplain: '轻度污染', color: '#ff7e00', so2Min: 501, so2Max: 650, coMin: 5, coMax: 10, spmMin: 101, spmMax: 150, healthImpact: '症状易感人群症状有轻度加剧，健康人群出现刺激症状', takeSteps: '儿童、老年人及心脏病、呼吸系统疾病患者应减少长时间户外锻炼' },
        { aqiId: 4, chineseExplain: '四级', aqiExplain: '中度污染', color: '#ff0000', so2Min: 651, so2Max: 800, coMin: 11, coMax: 24, spmMin: 151, spmMax: 200, healthImpact: '进一步加剧易感人群症状，可能对健康人群心脏、呼吸系统有影响', takeSteps: '儿童、老年人及心脏病、呼吸系统疾病患者避免长时间高强度户外锻炼' },
        { aqiId: 5, chineseExplain: '五级', aqiExplain: '重度污染', color: '#99004c', so2Min: 801, so2Max: 1600, coMin: 25, coMax: 36, spmMin: 201, spmMax: 300, healthImpact: '心脏病和肺病患者症状显著加剧，运动耐受力降低，健康人群普遍出现症状', takeSteps: '儿童、老年人及心脏病、肺病患者应停止户外运动，一般人群减少户外运动' },
        { aqiId: 6, chineseExplain: '六级', aqiExplain: '严重污染', color: '#7f0021', so2Min: 1601, so2Max: 2100, coMin: 37, coMax: 48, spmMin: 301, spmMax: 500, healthImpact: '健康人群运动耐受力降低，有明显强烈症状，提前出现某些疾病', takeSteps: '儿童、老年人和病人应留在室内，避免体力消耗，一般人群避免户外活动' }
      ]
    },

    getContrastColor(hex) {
      if (!hex || hex.length < 7) return '#fff'
      const r = parseInt(hex.substr(1, 2), 16)
      const g = parseInt(hex.substr(3, 2), 16)
      const b = parseInt(hex.substr(5, 2), 16)
      const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
      return luminance > 0.6 ? '#333' : '#fff'
    },

    async handleDelete(aqiId) {
      if (!confirm('确定要删除该级别吗？删除后不可恢复。')) return
      try {
        const res = await deleteAqi(aqiId)
        if (res.data.code === 200) {
          this.showToast('success', '删除成功')
          this.fetchList()
        } else {
          this.showToast('error', res.data.message || '删除失败')
        }
      } catch (err) {
        this.showToast('error', '网络异常，请稍后重试')
      }
    }
  }
}
</script>

<style scoped>
.list-page {
  min-height: 100vh;
  padding: 28px 32px;
  box-sizing: border-box;
  text-align: left;
}

.list-card {
  max-width: 1400px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.list-header {
  padding: 24px 32px;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fafbfc;
}

.list-title {
  margin: 0;
  font-size: 20px;
  color: #2c3e50;
}

.btn {
  padding: 9px 22px;
  font-size: 14px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.btn-primary {
  background: linear-gradient(135deg, #42b983, #2e8565);
  color: #fff;
  box-shadow: 0 4px 12px rgba(66, 185, 131, 0.3);
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(66, 185, 131, 0.4);
}

.table-wrap {
  padding: 0 32px 32px;
  overflow-x: auto;
}

.aqi-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  margin-top: 0;
}

.aqi-table thead th {
  background: #f5f7fa;
  color: #606266;
  font-weight: 600;
  padding: 14px 12px;
  text-align: left;
  border-bottom: 2px solid #ebeef5;
  white-space: nowrap;
  position: sticky;
  top: 0;
}

.aqi-table tbody td {
  padding: 14px 12px;
  border-bottom: 1px solid #ebeef5;
  color: #333;
  vertical-align: middle;
}

.aqi-table tbody tr:hover {
  background: #f8f9ff;
}

.grade-tag {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 14px;
  font-weight: 600;
  font-size: 13px;
  white-space: nowrap;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.12);
}

.color-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.color-block {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  display: inline-block;
}

.color-value {
  font-family: monospace;
  font-size: 13px;
  color: #666;
}

.text-ellipsis {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  vertical-align: middle;
}

.action-cell {
  white-space: nowrap;
}

.link-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 14px;
  padding: 4px 10px;
  border-radius: 4px;
  transition: all 0.2s;
}

.link-btn.edit {
  color: #42b983;
}

.link-btn.edit:hover {
  background: #e8f7f0;
}

.link-btn.delete {
  color: #f56c6c;
}

.link-btn.delete:hover {
  background: #fef0f0;
}

.empty-cell {
  text-align: center !important;
  color: #909399;
  padding: 60px 0 !important;
  font-size: 15px;
}

.loading-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e4e7ed;
  border-top-color: #42b983;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-text {
  color: #909399;
  font-size: 14px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.toast {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 22px;
  border-radius: 8px;
  font-size: 14px;
  z-index: 9999;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  max-width: 90%;
  white-space: nowrap;
}

.toast.success { background: #f0f9eb; color: #67c23a; border: 1px solid #e1f3d8; }
.toast.error   { background: #fef0f0; color: #f56c6c; border: 1px solid #fde2e2; }
.toast.warning { background: #fdf6ec; color: #e6a23c; border: 1px solid #faecd8; }
.toast.info    { background: #edf2fc; color: #909399; border: 1px solid #ebeef5; }

.toast-icon {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: currentColor;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

.toast-enter-active, .toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translate(-50%, -20px);
}

.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, -20px);
}
</style>