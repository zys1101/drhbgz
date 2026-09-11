<template>
  <div class="app-page">
    <!-- 任务统计条 -->
    <div class="app-stat-row">
      <div class="app-stat">
        <div class="stat-num">{{ tasks.length }}</div>
        <div class="stat-label">待检测任务</div>
      </div>
      <div class="app-stat">
        <div class="stat-num">{{ highCount }}</div>
        <div class="stat-label">较高污染反馈</div>
      </div>
      <el-button :icon="Refresh" circle class="stat-refresh" title="刷新任务" @click="fetchTasks" />
    </div>

    <div v-if="loading" class="loading-wrap" v-loading="loading" element-loading-text="任务加载中..."></div>

    <div v-else-if="tasks.length" class="task-list">
      <div v-for="task in tasks" :key="task.afId" class="task-card">
        <div class="task-top">
          <GradeTag :grade="task.estimatedGrade" />
          <span class="task-city">{{ task.cityName || task.cityId }}</span>
        </div>
        <div class="task-loc">
          <i class="fa-solid fa-location-dot task-pin"></i>
          {{ task.provinceName || task.provinceId }} · {{ task.cityName || task.cityId }}
        </div>
        <div class="task-addr">{{ task.address }}</div>
        <p class="task-info" :title="task.information">“{{ task.information }}”</p>
        <div class="task-foot">
          <span><i class="fa-solid fa-mobile-screen"></i> 反馈人 {{ maskTel(task.telId) }}</span>
          <span><i class="fa-solid fa-calendar"></i> {{ task.afDate }} {{ task.afTime }}</span>
        </div>
        <button class="app-btn-primary app-btn-block" @click="goMeasure(task)">
          <i class="fa-solid fa-vials"></i>录入实测数据
        </button>
      </div>
    </div>

    <div v-else class="app-empty">
      <span class="app-empty-icon"><i class="fa-solid fa-mug-hot"></i></span>
      <p>暂无待处理任务，去喝杯茶休息一下吧</p>
    </div>
  </div>
</template>

<script>
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import GradeTag from '../../components/GradeTag.vue'
import { getMyTasks } from '../../api/task'

export default {
  name: 'TaskListView',
  components: { GradeTag },
  data() {
    return {
      Refresh,
      tasks: [],
      loading: true,
      highCount: 0
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
    maskTel(tel) {
      if (!tel || tel.length < 7) return tel
      return tel.slice(0, 3) + '****' + tel.slice(-4)
    },
    async fetchTasks() {
      this.loading = true
      try {
        const res = await getMyTasks(this.gridCode)
        this.tasks = res.list
        this.highCount = this.tasks.filter(t => Number(t.estimatedGrade) >= 3).length
      } catch (err) {
        ElMessage.error((err && err.message) || '任务列表加载失败')
      } finally {
        this.loading = false
      }
    },
    goMeasure(task) {
      this.$router.push({
        path: '/gw/measure',
        query: { taskId: task.taskId, data: encodeURIComponent(JSON.stringify(task)) }
      })
    }
  }
}
</script>

<style scoped>
.loading-wrap {
  min-height: 280px;
}

/* 统计条 */
.app-stat-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.app-stat {
  flex: 1;
  background: #fff;
  border: 1px solid var(--app-ring);
  border-radius: 18px;
  padding: 14px 16px;
  display: flex;
  align-items: baseline;
  gap: 8px;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.05);
}

.stat-num {
  font-size: 24px;
  font-weight: 800;
  color: var(--app-strong);
  font-family: 'Segoe UI', 'PingFang SC', sans-serif;
}

.stat-label {
  font-size: 12.5px;
  color: #64748b;
}

.stat-refresh {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid var(--app-ring);
  color: var(--app-strong);
}

/* 任务卡片 */
.task-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}

@media (max-width: 768px) {
  .task-list {
    grid-template-columns: 1fr;
  }
}

.task-card {
  background: #fff;
  border: 1px solid var(--app-ring);
  border-radius: 20px;
  padding: 18px;
  box-shadow: 0 6px 22px rgba(15, 23, 42, 0.06);
  transition: all 0.2s;
}

.task-card:hover {
  border-color: rgba(20, 184, 166, 0.45);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.10);
  transform: translateY(-2px);
}

.task-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.task-city {
  font-size: 12px;
  color: #94a3b8;
}

.task-loc {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.task-pin {
  color: var(--app-strong);
  font-size: 13px;
}

.task-addr {
  margin: 4px 0 0 19px;
  font-size: 13px;
  color: #64748b;
}

.task-info {
  margin: 10px 0 4px 19px;
  font-size: 12.5px;
  color: #475569;
  font-style: italic;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-foot {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: #94a3b8;
  margin: 10px 0 14px;
  flex-wrap: wrap;
}

.task-foot i {
  margin-right: 4px;
  color: #b6c2c7;
}

@media (max-width: 640px) {
  .app-stat-row {
    gap: 8px;
  }

  .app-stat {
    padding: 12px;
  }

  .stat-num {
    font-size: 20px;
  }
}
</style>
