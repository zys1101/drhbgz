package com.example.demo.controller;

import com.example.demo.common.ResultVO;
import com.example.demo.service.AiAssistantService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * AI 助手接口
 *
 * GET  /api/ai/tools?role=xx  当前角色可用工具列表（MCP 过滤后）
 * POST /api/ai/chat           对话：配置了 spring.ai.openai（application.yml）时走大模型；
 *                             未配置时使用规则意图引擎（与 preview 预览服务契约一致）
 *
 * 三大角色助手：网格员 / 公众监督员 / 管理员；权限不同、可调用工具不同（由 AiAssistantService 强校验）。
 */
@RestController
@RequestMapping("/ai")
@CrossOrigin
public class AiController {

    private final AiAssistantService aiAssistantService;

    @Autowired(required = false)
    private ChatClient chatClient;

    public AiController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    /** 当前角色可用工具列表（MCP tools/list 过滤） */
    @GetMapping("/tools")
    public ResultVO tools(@RequestParam(required = false) String role) {
        List<Map<String, Object>> tools = aiAssistantService.toolsForRole(role == null ? "" : role);
        return new ResultVO(200, "查询成功", tools);
    }

    /** 对话入口：LLM 可配置 + 规则引擎兜底 */
    @PostMapping("/chat")
    public ResultVO chat(@RequestBody Map<String, Object> body) {
        String role = str(body.get("role"));
        String account = str(body.get("account"));
        String message = str(body.get("message"));
        if (role.isEmpty()) {
            return new ResultVO(400, "缺少角色参数", null);
        }
        if (message.isEmpty()) {
            return new ResultVO(400, "请输入您的问题", null);
        }

        // 1) 规则引擎先识别并执行工具（得到真实业务数据）
        Map<String, Object> ruleResult = aiAssistantService.chat(role, account, message);
        // 2) 若配置了大模型，则将工具结果作为背景数据交给模型组织回答
        if (chatClient != null) {
            try {
                String system = "你是东软环保公众监督系统的" + roleLabel(role) + "智能助手。"
                        + "你的可调用工具（MCP）如下：\n" + toolDescriptions(role)
                        + "\n当用户询问数据类问题（天气/任务/反馈/审批/名单/统计）时，系统已调用工具并给出背景数据，"
                        + "请基于背景数据组织简洁回答；不要编造数据。";
                String user = "背景数据：" + ruleResult.get("reply") + "\n\n用户问题：" + message;
                String content = chatClient.prompt().system(system).user(user).call().content();
                ruleResult.put("reply", content);
                ruleResult.put("llm", true);
            } catch (Exception e) {
                logWarn(e);
            }
        }
        return new ResultVO(200, "操作成功", ruleResult);
    }

    private String roleLabel(String role) {
        switch (role) {
            case "grid": return "网格员";
            case "supervisor": return "公众监督员";
            case "admin": return "管理员";
            case "viewer": return "决策者";
            default: return role;
        }
    }

    private String toolDescriptions(String role) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> t : aiAssistantService.toolsForRole(role)) {
            sb.append("- ").append(t.get("name")).append("：").append(t.get("description")).append("\n");
        }
        return sb.toString();
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static void logWarn(Exception e) {
        System.err.println("[ai] LLM 调用失败，已回退规则引擎答复: " + e.getMessage());
    }
}
