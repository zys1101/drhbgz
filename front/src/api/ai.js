import request from './request'

/**
 * AI 助手接口（MCP 协议 + 三角色助手）
 *  - GET  /api/ai/tools?role=xx  当前角色可用工具（按角色权限过滤）
 *  - POST /api/ai/chat           对话（服务端规则意图引擎 / 可配置大模型）
 *  - POST /api/mcp               MCP JSON-RPC：initialize / tools/list / tools/call
 */

// 当前角色可用工具列表（MCP tools/list 过滤后）
export function getAiTools(role) {
  return request.get('/ai/tools', { params: { role } })
}

// 与助手对话
export function chatWithAi(payload) {
  return request.post('/ai/chat', payload)
}

// MCP JSON-RPC 调用（标准协议：initialize/tools/list/tools/call）
export function mcpCall(payload) {
  return request.post('/mcp', payload)
}
