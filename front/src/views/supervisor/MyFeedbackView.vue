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
          <h2 class="card-title">历史反馈信息</h2>
          <p class="card-sub">仅显示您本人提交的反馈，按提交时间倒序</p>
        </div>
        <button class="btn btn-primary" @click="$router.push('/sf/submit')">+ 新增反馈</button>
      </div>

      <div v-if="loading" class="loading-wrap">
        <div class="spinner"></div>
        <span class="loading-text">数据加载中...</span>
      </div>

      <div v-else-if="list.length" class="feed-list">
        <div
          v-for="item in list"
          :key="item.afId"
          class="feed-item"
          @click="showDetail(item)"
        >
          <div class="feed-main">
            <span class="feed-title">📍 {{ item.provinceName || '省份' + item.provinceId }} · {{ item.cityName || '城市' + item.cityId }}</span>
            <span class="feed-sub">{{ item.afDate }} {{ item.afTime }}　{{ item.address }}</span>
          </div>
          <div class="feed-right">
            <span class="grade-tag" :class="'grade-' + item.estimatedGrade">{{ gradeText(item.estimatedGrade) }}</span>
            <span class="state-text" :class="'state-' + item.state">{{ stateText(item.state) }}</span>
          </div>
        </div>
      </div>

      <div v-else class="empty-wrap">
        <span class="empty-icon">📭</span>
        <span>暂无反馈记录</span>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div v-if="detail" class="dialog-mask" @click.self="detail = null">
      <div class="dialog">
        <div class="dialog-header">
          <h3>反馈详情</h3>
          <button class="dialog-close" @click="detail = null">✕</button>
        </div>
        <div class="dialog-body">
          <div class="detail-row"><span class="detail-label">反馈编号</span><span>{{ detail.afId }}</span></div>
          <div class="detail-row"><span class="detail-label">网格区域</span><span>{{ detail.provinceName || detail.provinceId }} · {{ detail.cityName || detail.cityId }}</span></div>
          <div class="detail-row"><span class="detail-label">具体地址</span><span>{{ detail.address }}</span></div>
          <div class="detail-row"><span class="detail-label">提交时间</span><span>{{ detail.afDate }} {{ detail.afTime }}</span></div>
          <div class="detail-row"><span class="detail-label">预估等级</span><span>{{ gradeText(detail.estimatedGrade) }}</span></div>
          <div class="detail-row"><span class="detail-label">当前状态</span><span>{{ stateText(detail.state) }}</span></div>
          <div class="detail-row"><span class="detail-label">反馈信息</span><span class="pre-wrap">{{ detail.information }}</span></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getAqiFeedbackList } from '../../api/aqiFeedback'
import { gradeText, stateText } from '../../constants/aqi'

export default {
  name: 'MyFeedbackView',
  data() {
    return {
      list: [],
      loading: true,
      detail: null,
      toast: { show: false, type: 'success', message: '' },
      toastTimer: null
    }
  },
  computed: {
    myAccount() {
      return this.$store.state.user ? this.$store.state.user.account : ''
    }
  },
  created() {
    this.fetchList()
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
          const all = res.data.data || []
          // 仅显示本人反馈（手机号即身份识别）
          this.list = all
            .filter(f => f.telId === this.myAccount)
            .sort((a, b) => (b.afDate + (b.afTime || '')).localeCompare(a.afDate + (a.afTime || '')))
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
    showDetail(item) {
      this.detail = item
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

/* 反馈列表 */
.feed-list {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feed-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #fafbfc;
  cursor: pointer;
  transition: all 0.2s;
}

.feed-item:hover {
  background: #f0f9f4;
  transform: translateX(2px);
}

.feed-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.feed-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.feed-sub {
  font-size: 12px;
  color: #a8abb2;
}

.feed-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.grade-tag {
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

.state-text {
  font-size: 12px;
  color: #909399;
}

.state-text.state-0 { color: #e6a23c; }
.state-text.state-3 { color: #67c23a; font-weight: 600; }

/* 空状态 */
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
  width: 520px;
  max-width: 92%;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
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
  width: 80px;
  flex-shrink: 0;
  color: #909399;
}

.pre-wrap {
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
