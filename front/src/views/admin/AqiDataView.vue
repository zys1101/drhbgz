<template>
  <div class="page">
    <transition name="toast">
      <div v-if="toast.show" class="toast" :class="toast.type">
        <span class="toast-msg">{{ toast.message }}</span>
      </div>
    </transition>

    <div class="card">
      <div class="card-header">
        <div>
          <h2 class="card-title">确认AQI数据管理</h2>
          <p class="card-sub">浏览网格员提交的实测AQI数据，确认后纳入统计；异常数据可退回重新检测</p>
        </div>
        <span v-if="mock" class="mock-badge">演示数据（后端未连接）</span>
      </div>

      <!-- 查询条件 -->
      <div class="filter-bar">
        <input v-model.trim="filters.keyword" class="filter-input" placeholder="地区 / 地址 / 网格员">
        <select v-model="filters.grade" class="filter-input">
          <option value="">全部AQI等级</option>
          <option v-for="g in gradeOptions" :key="g.value" :value="g.value">{{ g.label }}</option>
        </select>
        <select v-model="filters.state" class="filter-input">
          <option value="">全部状态</option>
          <option :value="0">待确认</option>
          <option :value="1">已确认</option>
          <option :value="2">已退回</option>
        </select>
        <button class="btn-filter" @click="applyFilter">查询</button>
        <button class="btn-filter reset" @click="resetFilter">重置</button>
      </div>

      <div v-if="loading" class="loading-wrap">
        <div class="spinner"></div>
        <span class="loading-text">数据加载中...</span>
      </div>

      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th width="110">数据编号</th>
              <th>省份</th>
              <th>城市</th>
              <th>地址</th>
              <th>SO₂</th>
              <th>CO</th>
              <th>PM2.5</th>
              <th>最终AQI等级</th>
              <th>网格员</th>
              <th>提交时间</th>
              <th>状态</th>
              <th width="170">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filteredList" :key="item.dataId">
              <td class="cell-id">{{ item.dataId }}</td>
              <td>{{ item.provinceName }}</td>
              <td>{{ item.cityName }}</td>
              <td class="cell-ellipsis" :title="item.address">{{ item.address }}</td>
              <td><span class="mini-tag" :class="'g' + item.so2Grade">{{ item.so2Grade }}级</span></td>
              <td><span class="mini-tag" :class="'g' + item.coGrade">{{ item.coGrade }}级</span></td>
              <td><span class="mini-tag" :class="'g' + item.pm25Grade">{{ item.pm25Grade }}级</span></td>
              <td>
                <span class="grade-tag" :class="'grade-' + item.aqiGrade">{{ gradeText(item.aqiGrade) }}</span>
              </td>
              <td>{{ item.gridCode }}</td>
              <td class="cell-nowrap">{{ item.submitTime }}</td>
              <td>
                <span class="state-tag" :class="'st-' + item.state">
                  {{ item.state === 0 ? '待确认' : item.state === 1 ? '已确认' : '已退回' }}
                </span>
              </td>
              <td>
                <div class="action-buttons">
                  <button class="btn-text btn-detail" @click="showDetail(item)">详情</button>
                  <button v-if="item.state === 0" class="btn-text btn-ok" @click="confirmData(item)">确认</button>
                  <button v-if="item.state === 0" class="btn-text btn-danger" @click="handleReject(item)">退回</button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredList.length === 0">
              <td colspan="12" class="empty-cell">
                <span class="empty-icon">📭</span>
                <span>暂无符合条件的数据</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div v-if="detail" class="dialog-mask" @click.self="detail = null">
      <div class="dialog">
        <div class="dialog-header">
          <h3>实测AQI数据详情</h3>
          <button class="dialog-close" @click="detail = null">✕</button>
        </div>
        <div class="dialog-body">
          <div class="detail-row"><span class="detail-label">数据编号</span><span>{{ detail.dataId }}</span></div>
          <div class="detail-row"><span class="detail-label">关联反馈</span><span>#{{ detail.afId }}</span></div>
          <div class="detail-row"><span class="detail-label">网格区域</span><span>{{ detail.provinceName }} · {{ detail.cityName }}</span></div>
          <div class="detail-row"><span class="detail-label">具体地址</span><span>{{ detail.address }}</span></div>
          <div class="detail-row"><span class="detail-label">SO₂等级</span><span>{{ gradeText(detail.so2Grade) }}</span></div>
          <div class="detail-row"><span class="detail-label">CO等级</span><span>{{ gradeText(detail.coGrade) }}</span></div>
          <div class="detail-row"><span class="detail-label">PM2.5等级</span><span>{{ gradeText(detail.pm25Grade) }}</span></div>
          <div class="detail-row"><span class="detail-label">最终AQI等级</span><span>{{ gradeText(detail.aqiGrade) }}（AQI = MAX 三项取最大）</span></div>
          <div class="detail-row"><span class="detail-label">检测网格员</span><span>{{ detail.gridCode }}</span></div>
          <div class="detail-row"><span class="detail-label">提交时间</span><span>{{ detail.submitTime }}</span></div>
          <div class="detail-row"><span class="detail-label">当前状态</span><span>{{ detail.state === 0 ? '待确认' : detail.state === 1 ? '已确认' : '已退回' }}</span></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getAqiDataList, rejectAqiData } from '../../api/task'
import { AQI_GRADES, gradeText } from '../../constants/aqi'

export default {
  name: 'AqiDataView',
  data() {
    return {
      list: [],
      loading: true,
      mock: false,
      filters: { keyword: '', grade: '', state: '' },
      applied: null,
      detail: null,
      gradeOptions: AQI_GRADES,
      toast: { show: false, type: 'success', message: '' },
      toastTimer: null
    }
  },
  computed: {
    filteredList() {
      const f = this.applied || { keyword: '', grade: '', state: '' }
      return this.list.filter(item => {
        if (f.keyword) {
          const hay = [item.provinceName, item.cityName, item.address, item.gridCode].join('|')
          if (hay.indexOf(f.keyword) === -1) return false
        }
        if (f.grade !== '' && item.aqiGrade !== Number(f.grade)) return false
        if (f.state !== '' && item.state !== Number(f.state)) return false
        return true
      })
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    gradeText,
    showToast(type, message) {
      this.toast = { show: true, type, message }
      if (this.toastTimer) clearTimeout(this.toastTimer)
      this.toastTimer = setTimeout(() => { this.toast.show = false }, 2500)
    },
    async fetchList() {
      this.loading = true
      try {
        const res = await getAqiDataList()
        this.list = res.list
        this.mock = res.mock
      } finally {
        this.loading = false
      }
    },
    applyFilter() {
      this.applied = { ...this.filters }
    },
    resetFilter() {
      this.filters = { keyword: '', grade: '', state: '' }
      this.applied = null
    },
    showDetail(item) {
      this.detail = item
    },
    confirmData(item) {
      item.state = 1
      this.showToast('success', '数据已确认，纳入统计范围')
    },
    async handleReject(item) {
      if (!confirm('退回后该任务将重新变为待指派，需要重新指派检测。确定退回吗？')) return
      await rejectAqiData(item.dataId)
      item.state = 2
      this.showToast('success', '已退回，任务重新进入待指派状态')
    }
  }
}
</script>

<style scoped>
.page {
  padding: 28px 32px;
  text-align: left;
}

.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.card-header {
  padding: 22px 28px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  margin: 0;
  font-size: 19px;
  color: #2c3e50;
}

.card-sub {
  margin: 5px 0 0;
  font-size: 13px;
  color: #a8abb2;
}

.mock-badge {
  font-size: 12px;
  color: #e6a23c;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  padding: 4px 12px;
  border-radius: 12px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 16px 28px;
  border-bottom: 1px solid #ebeef5;
  background: #fafbfc;
}

.filter-input {
  height: 34px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
  min-width: 140px;
  background: #fff;
  color: #303133;
}

.filter-input:focus {
  border-color: #42b983;
}

.btn-filter {
  height: 34px;
  padding: 0 18px;
  border: none;
  border-radius: 6px;
  background: linear-gradient(135deg, #42b983, #2e8565);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.btn-filter.reset {
  background: #fff;
  color: #606266;
  border: 1px solid #dcdfe6;
}

.table-wrap {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table th,
.data-table td {
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid #ebeef5;
}

.data-table th {
  background: #fafbfc;
  color: #909399;
  font-weight: 600;
  white-space: nowrap;
}

.data-table tbody tr:hover {
  background: #f4faf7;
}

.cell-id {
  color: #909399;
  font-family: monospace;
  font-size: 12px;
}

.cell-nowrap {
  white-space: nowrap;
}

.cell-ellipsis {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mini-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 12px;
}

.mini-tag.g1 { background: #95e8a7; color: #1a6b32; }
.mini-tag.g2 { background: #e1f3d8; color: #529b2e; }
.mini-tag.g3 { background: #faecd8; color: #b88230; }
.mini-tag.g4 { background: #fde2e2; color: #c45656; }
.mini-tag.g5 { background: #f89898; color: #fff; }
.mini-tag.g6 { background: #7b4a12; color: #fff; }

.grade-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.grade-tag.grade-1 { background: #95e8a7; color: #1a6b32; }
.grade-tag.grade-2 { background: #e1f3d8; color: #529b2e; }
.grade-tag.grade-3 { background: #faecd8; color: #b88230; }
.grade-tag.grade-4 { background: #fde2e2; color: #c45656; }
.grade-tag.grade-5 { background: #f89898; color: #fff; }
.grade-tag.grade-6 { background: #7b4a12; color: #fff; }

.state-tag.st-0 { color: #e6a23c; font-weight: 600; }
.state-tag.st-1 { color: #67c23a; font-weight: 600; }
.state-tag.st-2 { color: #f56c6c; }

.action-buttons {
  display: flex;
  gap: 6px;
}

.btn-text {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 10px;
  font-size: 12px;
  border-radius: 6px;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-detail { color: #606266; background: #f4f4f5; }
.btn-detail:hover { background: #e9e9eb; }

.btn-ok { color: #67c23a; background: #f0f9eb; }
.btn-ok:hover { background: #e1f3d8; }

.btn-danger { color: #f56c6c; background: #fef0f0; }
.btn-danger:hover { background: #fde2e2; }

.empty-cell {
  text-align: center !important;
  padding: 60px 0 !important;
}

.empty-cell > span {
  display: block;
  color: #c0c4cc;
  font-size: 14px;
}

.empty-icon {
  font-size: 34px;
  margin-bottom: 8px;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  width: 540px;
  max-width: 92%;
  max-height: 86vh;
  overflow-y: auto;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
}

.dialog-header {
  padding: 18px 22px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dialog-header h3 {
  margin: 0;
  font-size: 16px;
  color: #2c3e50;
}

.dialog-close {
  background: none;
  border: none;
  font-size: 16px;
  color: #909399;
  cursor: pointer;
}

.dialog-body {
  padding: 20px 22px;
}

.detail-row {
  display: flex;
  margin-bottom: 12px;
  font-size: 14px;
  line-height: 1.6;
}

.detail-label {
  width: 90px;
  flex-shrink: 0;
  color: #909399;
}

.loading-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
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
  padding: 12px 22px;
  border-radius: 8px;
  font-size: 14px;
  z-index: 9999;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  white-space: nowrap;
}

.toast.success { background: #f0f9eb; color: #67c23a; border: 1px solid #e1f3d8; }
.toast.error   { background: #fef0f0; color: #f56c6c; border: 1px solid #fde2e2; }
.toast.warning { background: #fdf6ec; color: #e6a23c; border: 1px solid #faecd8; }

.toast-enter-active, .toast-leave-active { transition: all 0.3s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translate(-50%, -20px); }
</style>
