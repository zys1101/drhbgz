<template>
  <div class="nep-page">
    <div class="nep-card">
      <div class="nep-card-header">
        <div>
          <h2 class="nep-card-title">
            <span class="nep-title-icon"><i class="fa-solid fa-clipboard-list"></i></span>
            我的检测任务
          </h2>
          <p class="nep-card-sub">
            由系统管理员指派的公众反馈任务，实地检测三项污染物后提交AQI数据
            <el-tag v-if="mock" type="warning" size="small" effect="plain" style="margin-left:8px">演示数据</el-tag>
          </p>
        </div>
        <el-button :icon="Refresh" circle @click="fetchTasks" />
      </div>

      <div v-if="loading" v-loading="loading" element-loading-text="任务加载中..." class="loading-wrap"></div>

      <div v-else-if="tasks.length" class="task-list">
        <div v-for="task in tasks" :key="task.afId" class="task-item">
          <div class="task-grade">
            <GradeTag :grade="task.estimatedGrade" />
            <span class="task-city">{{ task.cityName || task.cityId }}</span>
          </div>
          <div class="task-main">
            <div class="task-title">
              <i class="fa-solid fa-location-dot task-pin"></i>
              {{ task.provinceName || task.provinceId }} · {{ task.cityName || task.cityId }}
              <span class="task-addr">{{ task.address }}</span>
            </div>
            <div class="task-sub">
              <span><i class="fa-solid fa-mobile-screen"></i> 反馈人 {{ maskTel(task.telId) }}</span>
              <span><i class="fa-solid fa-calendar"></i> {{ task.afDate }} {{ task.afTime }}</span>
              <span class="task-info-text" :title="task.information">“{{ task.information }}”</span>
            </div>
          </div>
          <div class="task-actions">
            <el-button type="primary" class="nep-btn-gradient" @click="goMeasure(task)">
              <i class="fa-solid fa-vials" style="margin-right:6px"></i>录入实测数据
            </el-button>
          </div>
        </div>
      </div>

      <div v-else class="nep-empty">
        <span class="nep-empty-icon"><i class="fa-solid fa-mug-hot"></i></span>
        <span>暂无待处理任务，去喝杯茶休息一下吧</span>
      </div>
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
      mock: false
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
        this.mock = res.mock
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

.task-list {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 16px 18px;
  border: 1px solid var(--nep-border);
  border-radius: 12px;
  background: #fff;
  transition: all 0.2s;
}

.task-item:hover {
  border-color: #a7f3d0;
  box-shadow: 0 6px 18px rgba(16, 185, 129, 0.10);
  transform: translateY(-2px);
}

.task-grade {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  width: 96px;
  flex-shrink: 0;
}

.task-city {
  font-size: 12px;
  color: #94a3b8;
}

.task-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.task-title {
  font-size: 14.5px;
  font-weight: 600;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.task-pin {
  color: #10b981;
  font-size: 13px;
}

.task-addr {
  font-weight: 400;
  color: #64748b;
  font-size: 13px;
}

.task-sub {
  display: flex;
  align-items: center;
  gap: 18px;
  font-size: 12.5px;
  color: #94a3b8;
  flex-wrap: wrap;
}

.task-sub i {
  margin-right: 4px;
  color: #b6c2c7;
}

.task-info-text {
  max-width: 420px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #64748b;
}

.task-actions {
  flex-shrink: 0;
}

@media (max-width: 860px) {
  .task-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .task-grade {
    flex-direction: row;
    width: auto;
  }
}
</style>
