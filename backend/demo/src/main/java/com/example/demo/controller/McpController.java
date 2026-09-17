package com.example.demo.controller;

import com.example.demo.service.AiAssistantService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 协议端点（Model Context Protocol over HTTP，JSON-RPC 2.0）
 *
 * POST /api/mcp
 * 支持方法：initialize / tools/list / tools/call
 * 三角色权限由工具 allowedRoles 在 AiAssistantService 内强校验（tools/call 越权返回 isError=true）
 */
@RestController
@RequestMapping("/mcp")
@CrossOrigin
public class McpController {

    private final AiAssistantService aiAssistantService;

    public McpController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping
    public Map<String, Object> handle(@RequestBody Map<String, Object> body) {
        Object id = body.get("id");
        String method = body.get("method") == null ? "" : String.valueOf(body.get("method"));
        @SuppressWarnings("unchecked")
        Map<String, Object> params = body.get("params") instanceof Map
                ? (Map<String, Object>) body.get("params") : new LinkedHashMap<>();

        switch (method) {
            case "initialize":
                return result(id, Map.of(
                        "protocolVersion", "2024-11-05",
                        "capabilities", Map.of("tools", Map.of()),
                        "serverInfo", Map.of("name", "nep-ai-assistant", "version", "1.0.0")));
            case "tools/list": {
                String role = str(params.get("role"));
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("tools", aiAssistantService.toolsForRole(role));
                return result(id, r);
            }
            case "tools/call": {
                String name = str(params.get("name"));
                @SuppressWarnings("unchecked")
                Map<String, Object> args = params.get("arguments") instanceof Map
                        ? (Map<String, Object>) params.get("arguments") : new LinkedHashMap<>();
                String role = str(params.get("role"));
                try {
                    Object value = aiAssistantService.execTool(name, args, role);
                    String text = aiAssistantService.prettyToolResult(name, value);
                    return result(id, Map.of(
                            "content", List.of(Map.of("type", "text", "text", text)),
                            "isError", false));
                } catch (Exception e) {
                    String msg = e.getMessage() == null ? String.valueOf(e) : e.getMessage();
                    return result(id, Map.of(
                            "content", List.of(Map.of("type", "text", "text", msg)),
                            "isError", true));
                }
            }
            default:
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("jsonrpc", "2.0");
                err.put("id", id);
                err.put("error", Map.of("code", -32601, "message", "方法不存在: " + method));
                return err;
        }
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static Map<String, Object> result(Object id, Object resultValue) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("jsonrpc", "2.0");
        r.put("id", id);
        r.put("result", resultValue);
        return r;
    }
}
