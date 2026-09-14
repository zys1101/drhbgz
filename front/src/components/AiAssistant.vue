<template>
  <div class="ai-assistant">
    <!-- 悬浮按钮 -->
    <button class="ai-fab" :class="{ 'is-open': open }" @click="toggle" title="AI 助手">
      <i v-if="!open" class="fa-solid fa-robot"></i>
      <i v-else class="fa-solid fa-xmark"></i>
    </button>

    <!-- 聊天面板 -->
    <transition name="ai-pop">
      <div v-if="open" class="ai-panel">
        <header class="ai-header">
          <span class="ai-title"><i class="fa-solid fa-robot"></i>{{ title }}</span>
          <span class="ai-badge">{{ roleLabel }}</span>
        </header>

        <div class="ai-tools" v-if="tools.length">
          <div class="ai-tools-label">可用工具（MCP）</div>
          <button v-for="t in tools" :key="t.name" class="ai-tool" @click="askTool(t)">
            <i class="fa-solid fa-wand-magic-sparkles"></i>{{ t.name }}
          </button>
        </div>

        <div ref="bodyRef" class="ai-body">
          <div v-for="(m, i) in messages" :key="i" class="ai-msg" :class="m.role">
            <div class="ai-msg-bubble">
              <div v-if="m.toolName" class="ai-toolline"><i class="fa-solid fa-link"></i>已调用工具：{{ m.toolName }}</div>
              <div class="ai-msg-text">{{ m.text }}</div>
            </div>
          </div>
          <div v-if="thinking" class="ai-msg ai-role">
            <div class="ai-msg-bubble"><i class="fa-solid fa-spinner fa-spin"></i> 思考中…</div>
          </div>
          <div v-if="!messages.length" class="ai-empty">
            <i class="fa-solid fa-robot"></i><span>您好，我是{{ title }}，问我天气、任务或审批吧</span>
          </div>
        </div>

        <footer class="ai-foot">
          <input v-model="input" class="ai-input" placeholder="输入问题，例如：今天沈阳天气怎么样"
            @keyup.enter="send" />
          <button class="ai-send" :disabled="thinking || !input.trim()" @click="send">
            <i class="fa-solid fa-paper-plane"></i>
          </button>
        </footer>
      </div>
    </transition>
  </div>
</template>

<script>
import { getAiTools, chatWithAi } from '../api/ai'

// 三角色助手标题（权限由服务端按角色强校验）
const TITLES = {
  admin: '管理员智能助手',
  viewer: '决策智能助手',
  grid: '网格员智能助手',
  supervisor: '公众监督智能助手'
}
const ROLE_LABELS = {
  admin: '管理员端',
  viewer: '决策者端',
  grid: '网格员端',
  supervisor: '公众监督端'
}

export default {
  name: 'AiAssistant',
  data() {
    return {
      open: false,
      tools: [],
      messages: [],
      input: '',
      thinking: false,
      loaded: false
    }
  },
  computed: {
    role() {
      return this.$store.getters.role
    },
    account() {
      return this.$store.state.user ? this.$store.state.user.account : ''
    },
    title() {
      return TITLES[this.role] || 'AI 助手'
    },
    roleLabel() {
      return ROLE_LABELS[this.role] || ''
    }
  },
  methods: {
    toggle() {
      this.open = !this.open
      if (this.open && !this.loaded) {
        this.loadTools()
      }
    },
    async loadTools() {
      try {
        const res = await getAiTools(this.role)
        if (res.data.code === 200) this.tools = res.data.data
      } catch (e) {
        console.error('AI 工具列表加载失败', e)
      } finally {
        this.loaded = true
      }
    },
    async send() {
      const text = this.input.trim()
      if (!text || this.thinking) return
      this.messages.push({ role: 'user', text })
      this.input = ''
      this.thinking = true
      try {
        const res = await chatWithAi({ role: this.role, account: this.account, message: text })
        if (res.data.code === 200) {
          const d = res.data.data
          const calls = d.toolCalls || []
          this.messages.push({
            role: 'ai',
            text: d.reply,
            toolName: calls.length ? calls.map(c => c.name).join('、') : ''
          })
        } else {
          this.messages.push({ role: 'ai', text: res.data.message || '操作失败' })
        }
      } catch (err) {
        this.messages.push({ role: 'ai', text: '网络异常，请确认后端服务已启动' })
      } finally {
        this.thinking = false
        this.$nextTick(() => {
          if (this.$refs.bodyRef) this.$refs.bodyRef.scrollTop = this.$refs.bodyRef.scrollHeight
        })
      }
    },
    askTool(t) {
      this.input = '帮我' + t.description
      this.send()
    }
  }
}
</script>

<style scoped>
.ai-assistant {
  position: fixed;
  right: 22px;
  bottom: 22px;
  z-index: 3000;
  font-family: inherit;
}

.ai-fab {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  font-size: 20px;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(5, 150, 105, 0.45);
  transition: transform 0.2s;
}

.ai-fab:hover {
  transform: scale(1.06);
}

.ai-fab.is-open {
  background: #334155;
  box-shadow: 0 6px 18px rgba(51, 65, 85, 0.4);
}

.ai-panel {
  position: absolute;
  right: 0;
  bottom: 64px;
  width: 340px;
  height: 460px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.ai-header {
  padding: 12px 14px;
  background: linear-gradient(135deg, #065f46, #059669);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ai-title {
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 7px;
}

.ai-badge {
  font-size: 11px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  padding: 2px 9px;
}

.ai-tools {
  padding: 8px 12px;
  border-bottom: 1px solid #f1f5f9;
  max-height: 90px;
  overflow-y: auto;
}

.ai-tools-label {
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 5px;
}

.ai-tool {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin: 0 5px 5px 0;
  padding: 3px 9px;
  font-size: 11px;
  color: #047857;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 999px;
  cursor: pointer;
}

.ai-tool:hover {
  background: #d1fae5;
}

.ai-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: #f8faf9;
}

.ai-empty {
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
  margin-top: 60px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.ai-empty i {
  font-size: 30px;
  color: #a7f3d0;
}

.ai-msg {
  margin-bottom: 10px;
  display: flex;
}

.ai-msg.user {
  justify-content: flex-end;
}

.ai-msg-bubble {
  max-width: 82%;
  padding: 8px 11px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.55;
  background: #fff;
  border: 1px solid #e2e8f0;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-all;
}

.ai-msg.user .ai-msg-bubble {
  background: #059669;
  border-color: #059669;
  color: #fff;
}

.ai-toolline {
  font-size: 11px;
  color: #059669;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.ai-msg.user .ai-toolline {
  color: #d1fae5;
}

.ai-foot {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px;
  border-top: 1px solid #f1f5f9;
}

.ai-input {
  flex: 1;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 13px;
  outline: none;
}

.ai-input:focus {
  border-color: #10b981;
}

.ai-send {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: #059669;
  color: #fff;
  cursor: pointer;
}

.ai-send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ai-pop-enter-active,
.ai-pop-leave-active {
  transition: all 0.18s ease;
}

.ai-pop-enter-from,
.ai-pop-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
