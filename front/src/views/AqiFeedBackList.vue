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
        <div>
          <h2 class="list-title">空气质量反馈管理</h2>
          <p class="list-sub">共 {{ list.length }} 条反馈记录</p>
        </div>
        <button class="btn btn-primary" @click="$router.push('/aqiFeedbackForm')">+ 新增反馈</button>
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
              <th>日期</th>
              <th>时间</th>
              <th>等级</th>
              <th>状态</th>
              <th>手机号</th>
              <th>反馈信息</th>
              <th width="140">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in list" :key="item.afId">
              <td class="cell-id">{{ item.afId }}</td>
              <td>{{ item.provinceName }}</td>
              <td>{{ item.cityName }}</td>
              <td class="cell-ellipsis" :title="item.address">{{ item.address }}</td>
              <td class="cell-nowrap">{{ item.afDate }}</td>
              <td class="cell-nowrap">{{ item.afTime }}</td>
              <td>
                <span class="grade-tag" :class="'grade-' + item.estimatedGrade">
                  {{ getGradeText(item.estimatedGrade) }}
                </span>
              </td>
              <td>
                <span class="state-tag" :class="'state-' + item.state">
                  <span class="state-dot"></span>{{ getStateText(item.state) }}
                </span>
              </td>
              <td class="cell-nowrap">{{ item.telId }}</td>
              <td class="cell-ellipsis" :title="item.information">{{ item.information }}</td>
              <td>
                <div class="action-buttons">
                  <button @click="$router.push('/aqiFeedbackForm?afId=' + item.afId)" class="btn-text btn-edit">修改</button>
                  <button @click="handleDelete(item.afId)" class="btn-text btn-danger">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="list.length === 0">
              <td colspan="11" class="empty-cell">
                <span class="empty-icon">📭</span>
                <span>暂无反馈数据</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
import { getAqiFeedbackList, deleteByAfid } from '../api/aqiFeedback.js'

export default {
  name: 'AqiFeedBackList',
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
        const res = await getAqiFeedbackList()
        if (res.data.code === 200) {
          this.list = res.data.data || []
        } else {
          this.showToast('error', '获取列表失败：' + res.data.msg)
        }
      } catch (err) {
        console.error(err)
        this.showToast('error', '网络异常，请稍后重试')
      } finally {
        this.loading = false
      }
    },
    handleDelete(afId) {
      // 增加确认操作，防止误删
      if (confirm('确定要删除这条反馈吗？此操作不可恢复。')) {
        this.deleteItem(afId)
      }
    },
    async deleteItem(afId) {
      try {
        const res = await deleteByAfid(afId)
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
    },
    getGradeText(grade) {
      const map = {
        0: '未评级', 1: '一级', 2: '二级', 3: '三级',
        4: '四级', 5: '五级', 6: '六级'
      }
      return map[grade] || '未知'
    },
    getStateText(state) {
      const map = {
        0: '未分配', 1: '已分配', 2: '处理中', 3: '已处理'
      }
      return map[state] || '未知'
    }
  }
}
</script>

<style scoped>
.list-page {
  padding: 28px 32px;
  text-align: left;
}

.list-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.list-header {
  padding: 22px 28px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.list-title {
  margin: 0;
  font-size: 19px;
  color: #2c3e50;
}

.list-sub {
  margin: 4px 0 0;
  font-size: 13px;
  color: #a8abb2;
}

.btn {
  padding: 9px 20px;
  font-size: 14px;
  border: none;
  border-radius: 8px;
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

.data-table tbody tr {
  transition: background 0.15s;
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
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 等级标签 */
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

/* 状态标签 */
.state-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.state-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c0c4cc;
}

.state-tag.state-0 .state-dot { background: #c0c4cc; }
.state-tag.state-1 .state-dot { background: #409eff; }
.state-tag.state-2 .state-dot { background: #e6a23c; }
.state-tag.state-3 .state-dot { background: #67c23a; }

.state-tag.state-3 {
  color: #67c23a;
  font-weight: 600;
}

/* 操作按钮 */
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
}

.btn-danger {
  color: #f56c6c;
  background: #fef0f0;
}

.btn-danger:hover {
  background: #fde2e2;
}

.btn-edit {
  color: #42b983;
  background: #e8f7f0;
}

.btn-edit:hover {
  background: #d0efdf;
}

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

/* 加载态 */
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

/* Toast 提示 */
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
