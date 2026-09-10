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
          <h2 class="card-title">公众监督数据管理</h2>
          <p class="card-sub">浏览、查询公众监督员反馈数据，并为待指派数据指派网格员</p>
        </div>
      </div>

      <!-- 查询条件 -->
      <div class="filter-bar">
        <input v-model.trim="filters.keyword" class="filter-input" placeholder="地区 / 地址 / 手机号">
        <select v-model="filters.grade" class="filter-input">
          <option value="">全部等级</option>
          <option v-for="g in gradeOptions" :key="g.value" :value="g.value">{{ g.label }}</option>
        </select>
        <select v-model="filters.state" class="filter-input">
          <option value="">全部状态</option>
          <option v-for="s in stateOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
        </select>
        <input v-model="filters.dateFrom" type="date" class="filter-input" title="起始日期">
        <input v-model="filters.dateTo" type="date" class="filter-input" title="截止日期">
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
              <th width="70">ID</th>
              <th>省份</th>
              <th>城市</th>
              <th>地址</th>
              <th>提交时间</th>
              <th>预估等级</th>
              <th>状态</th>
              <th>手机号</th>
              <th width="220">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filteredList" :key="item.afId">
              <td class="cell-id">{{ item.afId }}</td>
              <td>{{ item.provinceName }}</td>
              <td>{{ item.cityName }}</td>
              <td class="cell-ellipsis" :title="item.address">{{ item.address }}</td>
              <td class="cell-nowrap">{{ item.afDate }} {{ item.afTime }}</td>
              <td>
                <span class="grade-tag" :class="'grade-' + item.estimatedGrade">{{ gradeText(item.estimatedGrade) }}</span>
              </td>
              <td>
                <span class="state-tag" :class="'state-' + item.state">{{ stateText(item.state) }}</span>
              </td>
              <td>{{ item.telId }}</td>
              <td>
                <div class="action-buttons">
                  <button class="btn-text btn-detail" @click="showDetail(item)">详情</button>
                  <button
                    v-if="item.state === 0"
                    class="btn-text btn-assign"
                    @click="openAssign(item)"
                  >指派网格员</button>
                  <button class="btn-text btn-danger" @click="handleDelete(item)">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredList.length === 0">
              <td colspan="9" class="empty-cell">
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
          <h3>反馈数据详情</h3>
          <button class="dialog-close" @click="detail = null">✕</button>
        </div>
        <div class="dialog-body">
          <div class="detail-row"><span class="detail-label">反馈编号</span><span>{{ detail.afId }}</span></div>
          <div class="detail-row"><span class="detail-label">网格区域</span><span>{{ detail.provinceName }} · {{ detail.cityName }}</span></div>
          <div class="detail-row"><span class="detail-label">具体地址</span><span>{{ detail.address }}</span></div>
          <div class="detail-row"><span class="detail-label">提交时间</span><span>{{ detail.afDate }} {{ detail.afTime }}</span></div>
          <div class="detail-row"><span class="detail-label">预估等级</span><span>{{ gradeText(detail.estimatedGrade) }}</span></div>
          <div class="detail-row"><span class="detail-label">当前状态</span><span>{{ stateText(detail.state) }}</span></div>
          <div class="detail-row"><span class="detail-label">反馈人</span><span>{{ detail.telId }}</span></div>
          <div class="detail-row"><span class="detail-label">反馈信息</span><span class="pre-wrap">{{ detail.information }}</span></div>
        </div>
      </div>
    </div>

    <!-- 指派弹窗 -->
    <div v-if="assigning" class="dialog-mask" @click.self="assigning = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3>指派网格员</h3>
          <button class="dialog-close" @click="assigning = false">✕</button>
        </div>
        <div class="dialog-body">
          <p class="assign-target">
            反馈编号 <b>#{{ assignItem.afId }}</b>　网格区域：<b>{{ assignItem.provinceName }} · {{ assignItem.cityName }}</b>
          </p>
          <p class="assign-mode">
            {{ localWorkers.length ? '该网格区域有可工作网格员，以下为本地指派名单：' : '该网格区域暂无可工作网格员，以下为就近异地指派名单：' }}
          </p>
          <div class="worker-list">
            <label
              v-for="w in orderedWorkers"
              :key="w.gridCode"
              class="worker-item"
              :class="{ disabled: !w.working }"
            >
              <input
                type="radio"
                name="worker"
                :value="w.gridCode"
                v-model="chosenWorker"
                :disabled="!w.working"
              >
              <span class="worker-name">{{ w.realName }}</span>
              <span class="worker-region">{{ w.region }}</span>
              <span class="worker-tag" :class="w.working ? 'on' : 'off'">{{ w.working ? '工作中' : '非工作' }}</span>
            </label>
          </div>
          <div class="form-actions">
            <button class="btn btn-default" @click="assigning = false">取 消</button>
            <button class="btn btn-primary" :disabled="!chosenWorker || assigning2" @click="confirmAssign">
              {{ assigning2 ? '指派中...' : '确认指派' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getAqiFeedbackList, deleteByAfid } from '../../api/aqiFeedback'
import { assignTask, getGridWorkers } from '../../api/task'
import { AQI_GRADES, FEEDBACK_STATES, gradeText, stateText } from '../../constants/aqi'

export default {
  name: 'FeedbackManageView',
  data() {
    return {
      list: [],
      loading: true,
      filters: {
        keyword: '',
        grade: '',
        state: '',
        dateFrom: '',
        dateTo: ''
      },
      applied: null, // 点击"查询"后生效的条件
      detail: null,
      assigning: false,
      assigning2: false,
      assignItem: null,
      workers: [],
      chosenWorker: '',
      gradeOptions: AQI_GRADES,
      stateOptions: FEEDBACK_STATES,
      toast: { show: false, type: 'success', message: '' },
      toastTimer: null
    }
  },
  computed: {
    filteredList() {
      const f = this.applied || { keyword: '', grade: '', state: '', dateFrom: '', dateTo: '' }
      return this.list.filter(item => {
        if (f.keyword) {
          const hay = [item.provinceName, item.cityName, item.address, item.telId, item.information]
            .join('|')
          if (hay.indexOf(f.keyword) === -1) return false
        }
        if (f.grade !== '' && item.estimatedGrade !== Number(f.grade)) return false
        if (f.state !== '' && item.state !== Number(f.state)) return false
        if (f.dateFrom && item.afDate < f.dateFrom) return false
        if (f.dateTo && item.afDate > f.dateTo) return false
        return true
      })
    },
    localWorkers() {
      if (!this.assignItem) return []
      const region = (this.assignItem.provinceName || '') + '-' + (this.assignItem.cityName || '')
      return this.workers.filter(w => w.region === region && w.working)
    },
    orderedWorkers() {
      // 本地指派优先，异地（其它区域）按就近原则排后
      const region = this.assignItem ? (this.assignItem.provinceName || '') + '-' + (this.assignItem.cityName || '') : ''
      return [...this.workers].sort((a, b) => {
        const localA = a.region === region ? 0 : 1
        const localB = b.region === region ? 0 : 1
        if (localA !== localB) return localA - localB
        return (b.working ? 1 : 0) - (a.working ? 1 : 0)
      })
    }
  },
  created() {
    this.fetchList()
    this.loadWorkers()
  },
  methods: {
    gradeText,
    stateText,
    showToast(type, message) {
      this.toast = { show: true, type, message }
      if (this.toastTimer) clearTimeout(this.toastTimer)
      this.toastTimer = setTimeout(() => { this.toast.show = false }, 2500)
    },
    async fetchList() {
      this.loading = true
      try {
        const res = await getAqiFeedbackList()
        if (res.data.code === 200) {
          this.list = res.data.data || []
        } else {
          this.showToast('error', '获取列表失败：' + res.data.msg || res.data.message)
        }
      } catch (err) {
        console.error(err)
        this.showToast('error', '网络异常，请确认后端服务已启动')
      } finally {
        this.loading = false
      }
    },
    async loadWorkers() {
      const res = await getGridWorkers()
      this.workers = res.list
    },
    applyFilter() {
      this.applied = { ...this.filters }
    },
    resetFilter() {
      this.filters = { keyword: '', grade: '', state: '', dateFrom: '', dateTo: '' }
      this.applied = null
    },
    showDetail(item) {
      this.detail = item
    },
    async openAssign(item) {
      this.assignItem = item
      this.chosenWorker = ''
      this.assigning = true
      if (!this.workers.length) {
        await this.loadWorkers()
      }
    },
    async confirmAssign() {
      if (!this.chosenWorker) return
      this.assigning2 = true
      try {
        const res = await assignTask(this.assignItem.afId, this.chosenWorker, this.assignItem)
        const worker = this.workers.find(w => w.gridCode === this.chosenWorker)
        const isLocal = worker && worker.region === (this.assignItem.provinceName || '') + '-' + (this.assignItem.cityName || '')
        // 本地更新列表状态（后端就绪后由列表刷新获得）
        this.assignItem.state = 1
        this.showToast('success', (isLocal ? '本地指派' : '异地指派') + '成功，已通知网格员' + (res.mock ? '（演示数据）' : ''))
        this.assigning = false
      } catch (err) {
        console.error(err)
        this.showToast('error', '指派失败，请重试')
      } finally {
        this.assigning2 = false
      }
    },
    async handleDelete(item) {
      if (!confirm('确定要删除这条反馈吗？此操作不可恢复。')) return
      try {
        const res = await deleteByAfid(item.afId)
        if (res.data.code === 200) {
          this.showToast('success', '删除成功')
          this.fetchList()
        } else {
          this.showToast('error', '删除失败')
        }
      } catch (err) {
        console.error(err)
        this.showToast('error', '删除请求出错')
      }
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

/* 查询条件栏 */
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
  min-width: 130px;
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

/* 表格 */
.table-wrap {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  padding: 13px 16px;
  text-align: left;
  border-bottom: 1px solid #ebeef5;
}

.data-table th {
  background: #fafbfc;
  color: #909399;
  font-weight: 600;
  font-size: 13px;
  white-space: nowrap;
}

.data-table tbody tr:hover {
  background: #f4faf7;
}

.cell-id {
  color: #909399;
  font-family: monospace;
}

.cell-nowrap {
  white-space: nowrap;
}

.cell-ellipsis {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.grade-tag {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.grade-tag.grade-0 { background: #f4f4f5; color: #909399; }
.grade-tag.grade-1 { background: #95e8a7; color: #1a6b32; }
.grade-tag.grade-2 { background: #e1f3d8; color: #529b2e; }
.grade-tag.grade-3 { background: #faecd8; color: #b88230; }
.grade-tag.grade-4 { background: #fde2e2; color: #c45656; }
.grade-tag.grade-5 { background: #f89898; color: #fff; }
.grade-tag.grade-6 { background: #7b4a12; color: #fff; }

.state-tag {
  font-size: 13px;
  white-space: nowrap;
}

.state-tag.state-0 { color: #e6a23c; font-weight: 600; }
.state-tag.state-1 { color: #409eff; }
.state-tag.state-2 { color: #909399; }
.state-tag.state-3 { color: #67c23a; font-weight: 600; }

.action-buttons {
  display: flex;
  gap: 8px;
}

.btn-text {
  background: none;
  border: none;
  cursor: pointer;
  padding: 5px 12px;
  font-size: 13px;
  border-radius: 6px;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-detail { color: #606266; background: #f4f4f5; }
.btn-detail:hover { background: #e9e9eb; }

.btn-assign { color: #fff; background: linear-gradient(135deg, #42b983, #2e8565); }
.btn-assign:hover { opacity: 0.85; }

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

/* 弹窗 */
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
  width: 560px;
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
  position: sticky;
  top: 0;
  background: #fff;
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
  width: 80px;
  flex-shrink: 0;
  color: #909399;
}

.pre-wrap {
  white-space: pre-wrap;
  word-break: break-all;
}

/* 指派 */
.assign-target {
  margin: 0 0 10px;
  font-size: 14px;
  color: #303133;
}

.assign-mode {
  margin: 0 0 12px;
  font-size: 13px;
  color: #e6a23c;
}

.worker-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 18px;
}

.worker-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  font-size: 14px;
}

.worker-item:hover {
  border-color: #b7e4cf;
}

.worker-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.worker-name {
  font-weight: 600;
  color: #303133;
}

.worker-region {
  color: #909399;
  font-size: 13px;
}

.worker-tag {
  margin-left: auto;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
}

.worker-tag.on { background: #f0f9eb; color: #67c23a; }
.worker-tag.off { background: #f4f4f5; color: #909399; }

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn {
  padding: 9px 24px;
  font-size: 14px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: linear-gradient(135deg, #42b983, #2e8565);
  color: #fff;
}

.btn-default {
  background: #fff;
  color: #606266;
  border: 1px solid #dcdfe6;
}

/* 加载态 */
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

/* Toast */
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
