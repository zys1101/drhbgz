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
          <h2 class="card-title">我的检测任务</h2>
          <p class="card-sub">由系统管理员指派的公众反馈任务，共 {{ tasks.length }} 条</p>
        </div>
        <span v-if="mock" class="mock-badge">演示数据（后端未连接）</span>
      </div>

      <div v-if="loading" class="loading-wrap">
        <div class="spinner"></div>
        <span class="loading-text">数据加载中...</span>
      </div>

      <div v-else-if="tasks.length" class="task-list">
        <div v-for="t in tasks" :key="t.taskId" class="task-item">
          <div class="task-main">
            <div class="task-title">
              <span class="task-id">{{ t.taskId }}</span>
              <span class="grade-tag" :class="'grade-' + t.estimatedGrade">{{ gradeText(t.estimatedGrade) }}</span>
            </div>
            <div class="task-addr">📍 {{ t.provinceName }} · {{ t.cityName }}　{{ t.address }}</div>
            <div class="task-sub">反馈人：{{ maskTel(t.telId) }}　反馈时间：{{ t.afDate }} {{ t.afTime }}</div>
            <div class="task-info">{{ t.information }}</div>
          </div>
          <button class="btn btn-primary" @click="goMeasure(t)">开始检测 →</button>
        </div>
      </div>

      <div v-else class="empty-wrap">
        <span class="empty-icon">🎉</span>
        <span>暂无待处理任务</span>
      </div>
    </div>
  </div>
</template>

<script>
import { getMyTasks } from '../../api/task'
import { gradeText } from '../../constants/aqi'

export default {
  name: 'TaskListView',
  data() {
    return {
      tasks: [],
      loading: true,
      mock: false,
      toast: { show: false, type: 'success', message: '' },
      toastTimer: null
    }
  },
  computed: {
    gridCode() {
      return this.$store.state.user ? this.$store.state.user.account : ''
    }
  },
  created() {
    this.fetchTasks()
  },
  methods: {
    gradeText,
    maskTel(tel) {
      if (!tel || tel.length < 7) return tel
      return tel.slice(0, 3) + '****' + tel.slice(-4)
    },
    async fetchTasks() {
      this.loading = true
      try {
        const res = await getMyTasks(this.gridCode)
        this.tasks = res.list
        this.mock = res.mock
      } finally {
        this.loading = false
      }
    },
    goMeasure(task) {
      this.$router.push({ path: '/gw/measure', query: { taskId: task.taskId, data: encodeURIComponent(JSON.stringify(task)) } })
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

.task-list {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 20px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  transition: all 0.2s;
}

.task-item:hover {
  border-color: #b7e4cf;
  box-shadow: 0 4px 14px rgba(66, 185, 131, 0.12);
}

.task-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.task-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.task-id {
  font-family: monospace;
  font-size: 13px;
  color: #909399;
}

.task-addr {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.task-sub {
  font-size: 12px;
  color: #a8abb2;
}

.task-info {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 640px;
}

.btn {
  padding: 10px 22px;
  font-size: 14px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
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

.grade-tag {
  padding: 3px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.grade-tag.grade-0 { background: #f4f4f5; color: #909399; }
.grade-tag.grade-1 { background: #95e8a7; color: #1a6b32; }
.grade-tag.grade-2 { background: #e1f3d8; color: #529b2e; }
.grade-tag.grade-3 { background: #faecd8; color: #b88230; }
.grade-tag.grade-4 { background: #fde2e2; color: #c45656; }
.grade-tag.grade-5 { background: #f89898; color: #fff; }
.grade-tag.grade-6 { background: #7b4a12; color: #fff; }

.empty-wrap {
  padding: 80px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: #c0c4cc;
  font-size: 14px;
}

.empty-icon {
  font-size: 40px;
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
</style>
