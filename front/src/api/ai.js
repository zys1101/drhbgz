import request from './request'

/**
 * AI 助手接口（MCP 协议 + 三角色助手 + SSE 流式）
 *  - GET  /api/ai/tools?role=xx  当前角色可用工具（按角色权限过滤后返回）
 *  - POST /api/ai/chat/stream    SSE 流式对话（事件：tool / delta / done）
 *  - POST /api/mcp               标准 MCP JSON-RPC（initialize / tools/list / tools/call）
 */

// 当前角色可用工具列表
export function getAiTools(role) {
  return request.get('/ai/tools', { params: { role } })
}

// 一次性对话（兼容保留；前端主流程使用 streamChat）
export function chatWithAi(payload) {
  return request.post('/ai/chat', payload)
}

// MCP JSON-RPC 调用
export function mcpCall(payload) {
  return request.post('/mcp', payload)
}

// 流式对话：POST /ai/chat/stream（SSE），onEvent 逐个回调 {type:'tool'|'delta'|'done', ...}
export async function streamChat(payload, onEvent) {
  const resp = await fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  if (!resp.ok) throw new Error('HTTP ' + resp.status)
  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buf = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buf += decoder.decode(value, { stream: true })
    let idx
    while ((idx = buf.indexOf('\n\n')) >= 0) {
      const raw = buf.slice(0, idx)
      buf = buf.slice(idx + 2)
      const line = raw.split('\n').find(l => l.startsWith('data: '))
      if (!line) continue
      let payloadObj
      try {
        payloadObj = JSON.parse(line.slice(6))
      } catch (e) {
        continue
      }
      onEvent && onEvent(payloadObj)
    }
  }
}
