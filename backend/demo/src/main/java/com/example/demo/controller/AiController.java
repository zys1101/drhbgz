package com.example.demo.controller;

import com.example.demo.common.ResultVO;
import com.example.demo.service.AiAssistantService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;

import java.util.ArrayList;
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
    private ChatClient.Builder chatClientBuilder;

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
        ChatClient client = chatClient();
        if (client != null) {
            try {
                String system = "你是东软环保公众监督系统的" + roleLabel(role) + "智能助手。"
                        + "你的可调用工具（MCP）如下：\n" + toolDescriptions(role)
                        + "\n当用户询问数据类问题（天气/任务/反馈/审批/名单/统计）时，系统已调用工具并给出背景数据，"
                        + "请基于背景数据组织简洁回答；不要编造数据。";
                String user = "背景数据：" + ruleResult.get("reply") + "\n\n用户问题：" + message;
                String content = client.prompt().system(system).user(user).call().content();
                ruleResult.put("reply", content);
                ruleResult.put("llm", true);
            } catch (Exception e) {
                logWarn(e);
            }
        }
        return new ResultVO(200, "操作成功", ruleResult);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> body) {
        String role = str(body.get("role"));
        String account = str(body.get("account"));
        String message = str(body.get("message"));

        SseEmitter emitter = new SseEmitter(0L);

        if (role.isEmpty() || message.isEmpty()) {
            try {
                emitter.send(SseEmitter.event().data("{\"type\":\"error\",\"text\":\"缺少角色或问题\"}"));
                emitter.complete();
            } catch (IOException ignored) {}
            return emitter;
        }

        // 规则引擎先识别并执行工具（得到真实业务数据）
        Map<String, Object> ruleResult = aiAssistantService.chat(role, account, message);
        // 将已调用的工具以 tool 事件回传，前端用于展示“已调用工具”
        emitToolEvents(emitter, ruleResult);

        // 没有配置大模型 → 回退规则引擎，一次性把答复以 delta 事件返回（同样可被前端渲染）
        ChatClient client = chatClient();
        if (client == null) {
            try {
                emitter.send(SseEmitter.event().data(deltaEvent(String.valueOf(ruleResult.get("reply")))));
                sendDoneEvent(emitter);
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // 有 LLM → 在背景数据基础上，再注册 call_tool，让模型自己识别意图并调用数据库工具，全程流式
        FunctionToolCallback callTool = FunctionToolCallback.builder("call_tool",
                (ToolDispatch input) -> aiAssistantService.callToolForAi(input.tool(), input.params(), role))
                .description("按用户意图调用系统已授权的业务数据/工具，返回真实数据。可用工具：" + toolNames(role)
                        + "。params 为目标工具入参（如 weather.now 需要 city；grid.task.list 等的账号参数由系统自动填充）")
                .inputType(ToolDispatch.class)
                .build();

        String system = "你是东软环保公众监督系统的" + roleLabel(role) + "智能助手。"
                + "系统已按你的角色预取背景数据如下：\n" + ruleResult.get("reply")
                + "\n你的角色授权工具：\n" + toolDescriptions(role)
                + "\n当用户询问的数据在背景数据中已给出时，直接基于它组织简洁回答；"
                + "当用户询问天气/任务/反馈/审批/名单/统计等而背景数据未覆盖时，调用 call_tool 获取真实数据再回答，不要编造数据。";
        String user = "背景数据：" + ruleResult.get("reply") + "\n\n用户问题：" + message;

        client.prompt().system(system).user(user).toolCallbacks(callTool).stream().content()
                .subscribe(
                        chunk -> {
                            try {
                                emitter.send(SseEmitter.event().data(deltaEvent(chunk)));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        err -> {
                            try {
                                emitter.send(SseEmitter.event().data("{\"type\":\"error\",\"text\":" + jsonEncode(
                                        "流式生成失败：" + (err.getMessage() == null ? String.valueOf(err) : err.getMessage())) + "}"));
                            } catch (IOException ignored) {}
                            emitter.completeWithError(err);
                        },
                        () -> {
                            sendDoneEvent(emitter);
                            emitter.complete();
                        }
                );

        return emitter;
    }

    /** 大模型“自驱”工具调用的入参模型（tool=工具名，params=工具参数；role 由服务端注入并强校验，模型无法伪造）。 */
    public record ToolDispatch(String tool, Map<String, Object> params) {}

    /** 当前角色授权工具的逗号列表（供 call_tool 的入参说明使用） */
    private String toolNames(String role) {
        List<String> names = new ArrayList<>();
        for (Map<String, Object> t : aiAssistantService.toolsForRole(role == null ? "" : role)) {
            names.add(String.valueOf(t.get("name")));
        }
        return String.join(", ", names);
    }

    private ChatClient chatClient() {
        return chatClientBuilder == null ? null : chatClientBuilder.build();
    }

    /** 把规则引擎调用的工具以 tool 事件回传前端 */
    @SuppressWarnings("unchecked")
    private void emitToolEvents(SseEmitter emitter, Map<String, Object> ruleResult) {
        Object calls = ruleResult.get("toolCalls");
        if (!(calls instanceof List)) {
            return;
        }
        for (Object c : (List<Object>) calls) {
            if (c instanceof Map) {
                Object name = ((Map<Object, Object>) c).get("name");
                if (name != null) {
                    try {
                        emitter.send(SseEmitter.event().data(toolEvent(String.valueOf(name))));
                    } catch (IOException ignored) {}
                }
            }
        }
    }

    /** 生成 {type:delta, text:...} 事件 */
    private static String deltaEvent(String text) {
        return "{\"type\":\"delta\",\"text\":" + jsonEncode(text == null ? "" : text) + "}";
    }

    /** 生成 {type:tool, name:...} 事件 */
    private static String toolEvent(String name) {
        return "{\"type\":\"tool\",\"name\":" + jsonEncode(name == null ? "" : name) + "}";
    }

    /** 结束事件 {type:done} */
    private static void sendDoneEvent(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("done").data("{\"type\":\"done\"}"));
        } catch (IOException ignored) {}
    }

    /** JSON 字符串转义（中文与普通字符原样输出） */
    private static String jsonEncode(String s) {
        if (s == null) {
            return "\"\"";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
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
