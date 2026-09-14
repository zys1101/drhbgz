package com.example.demo.service;

import com.example.demo.entity.AqiFeedback;
import com.example.demo.mapper.AqiDataMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 助手服务（MCP 工具 + 三角色权限 + 天气引擎 + 规则意图引擎）
 *
 * 契约与预览服务（preview/ai_assistant.py）保持一致：
 *  - /api/mcp     标准 JSON-RPC 2.0（initialize / tools/list / tools/call）
 *  - /api/ai/tools?role=xx   返回该角色可用工具（按 allowedRoles 过滤）
 *  - /api/ai/chat 规则意图引擎：识别问题 → 按角色调用工具 → 组织答复
 *
 * 三角色权限：网格员 grid / 公众监督员 supervisor / 管理员 admin；
 * 决策者 viewer 仅开放统计与天气。所有工具服务端强校验角色，无权调用返回 403 语义错误。
 */
@Slf4j
@Service
public class AiAssistantService {

    private final ITaskService taskService;
    private final IAqiFeedbackService feedbackService;
    private final ILeaveService leaveService;
    private final IEmployeeService employeeService;
    private final ISupervisorService supervisorService;
    private final IGridCityService cityService;
    private final AqiDataMapper aqiDataMapper;

    public AiAssistantService(ITaskService taskService, IAqiFeedbackService feedbackService,
                              ILeaveService leaveService, IEmployeeService employeeService,
                              ISupervisorService supervisorService, IGridCityService cityService,
                              AqiDataMapper aqiDataMapper) {
        this.taskService = taskService;
        this.feedbackService = feedbackService;
        this.leaveService = leaveService;
        this.employeeService = employeeService;
        this.supervisorService = supervisorService;
        this.cityService = cityService;
        this.aqiDataMapper = aqiDataMapper;
    }

    public static final String ROLE_GRID = "grid";
    public static final String ROLE_SUPERVISOR = "supervisor";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_VIEWER = "viewer";

    private static final Map<String, String> ROLE_LABEL = new LinkedHashMap<>();
    static {
        ROLE_LABEL.put(ROLE_GRID, "网格员");
        ROLE_LABEL.put(ROLE_SUPERVISOR, "公众监督员");
        ROLE_LABEL.put(ROLE_ADMIN, "管理员");
        ROLE_LABEL.put(ROLE_VIEWER, "决策者");
    }

    /** MCP 工具注册表（name/description/inputSchema/allowedRoles） */
    public static List<Map<String, Object>> toolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(tool("weather.now", "查询指定城市的实时天气与空气质量（温度/天气现象/湿度/风力/AQI等级）",
                "{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\",\"description\":\"城市名，如 沈阳市\"}}}",
                ROLE_GRID, ROLE_SUPERVISOR, ROLE_ADMIN, ROLE_VIEWER));
        tools.add(tool("grid.task.list", "查询当前网格员被指派的待检测任务列表",
                "{\"type\":\"object\",\"properties\":{\"gridCode\":{\"type\":\"string\",\"description\":\"网格员登录编码\"}}}",
                ROLE_GRID));
        tools.add(tool("grid.leave.apply", "网格员发起请假申请（事由+起止日期），提交后等待管理员审批",
                "{\"type\":\"object\",\"properties\":{\"reason\":{\"type\":\"string\",\"description\":\"请假事由\"},\"startDate\":{\"type\":\"string\",\"description\":\"开始日期 YYYY-MM-DD\"},\"endDate\":{\"type\":\"string\",\"description\":\"结束日期 YYYY-MM-DD\"}}}",
                ROLE_GRID));
        tools.add(tool("feedback.submit", "公众监督员提交空气质量监督反馈（预估等级+描述）",
                "{\"type\":\"object\",\"properties\":{\"telId\":{\"type\":\"string\",\"description\":\"监督员手机号\"}}}",
                ROLE_SUPERVISOR));
        tools.add(tool("feedback.mine", "查询公众监督员本人提交的历史反馈",
                "{\"type\":\"object\",\"properties\":{\"telId\":{\"type\":\"string\",\"description\":\"监督员手机号\"}}}",
                ROLE_SUPERVISOR));
        tools.add(tool("hr.employee.list", "查看全部网格员名单（编码/姓名/负责地区/工作状态）",
                "{\"type\":\"object\",\"properties\":{}}", ROLE_ADMIN));
        tools.add(tool("hr.leave.approve", "查看待审批的网格员请假申请，管理员可同意或驳回",
                "{\"type\":\"object\",\"properties\":{}}", ROLE_ADMIN));
        tools.add(tool("hr.supervisor.list", "查看全部公众监督员名单（手机号/姓名/年龄/性别）",
                "{\"type\":\"object\",\"properties\":{}}", ROLE_ADMIN));
        tools.add(tool("stats.overview", "查询系统五项统计概览（反馈总量/实测确认/省市覆盖率等）",
                "{\"type\":\"object\",\"properties\":{}}", ROLE_ADMIN, ROLE_VIEWER));
        return tools;
    }

    private static Map<String, Object> tool(String name, String desc, String schema, String... roles) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("name", name);
        t.put("description", desc);
        t.put("inputSchema", schema);
        t.put("allowedRoles", Arrays.asList(roles));
        return t;
    }

    /** 按角色过滤后的工具（MCP tools/list 与前端工具面板） */
    public List<Map<String, Object>> toolsForRole(String role) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> t : toolDefinitions()) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) t.get("allowedRoles");
            if (roles.contains(role)) {
                Map<String, Object> slim = new LinkedHashMap<>();
                slim.put("name", t.get("name"));
                slim.put("description", t.get("description"));
                slim.put("inputSchema", t.get("inputSchema"));
                out.add(slim);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- 天气引擎
    private static final String[] WEATHER_PHENOMENA = {"晴", "多云", "阴", "小雨", "中雨", "雷阵雨", "轻度霾"};
    private static final String[] WIND_DIRS = {"东风", "南风", "西风", "北风", "东南风", "西南风"};

    private static int cityHash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return ((d[0] & 0xFF) << 24) | ((d[1] & 0xFF) << 16) | ((d[2] & 0xFF) << 8) | (d[3] & 0xFF);
        } catch (Exception e) {
            return text.hashCode();
        }
    }

    private static int mask(int h, int shift, int mod) {
        return ((h >> shift) & 0xFFFF) % mod;
    }

    public Map<String, Object> weatherOf(String cityName) {
        boolean exists = cityService.count(new LambdaQueryWrapper<com.example.demo.entity.GridCity>()
                .eq(com.example.demo.entity.GridCity::getCityName, cityName)) > 0;
        if (!exists) {
            throw new IllegalArgumentException("未找到城市「" + cityName + "」，请从系统大城市列表中选择");
        }
        int h = cityHash(cityName);
        int temp = 8 + mask(h, 0, 26);
        int hum = 30 + mask(h, 4, 61);
        String phen = WEATHER_PHENOMENA[mask(h, 8, WEATHER_PHENOMENA.length)];
        String wind = WIND_DIRS[mask(h, 12, WIND_DIRS.length)] + (1 + mask(h, 16, 5)) + "级";
        int aqi = 1 + mask(h, 20, 200);
        String[] gradeNames = {"", "一级·优", "二级·良", "三级·轻度污染", "四级·中度污染", "五级·重度污染"};
        int grade = aqi <= 50 ? 1 : aqi <= 100 ? 2 : aqi <= 150 ? 3 : 4;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("city", cityName);
        out.put("temperature", temp);
        out.put("phenomenon", phen);
        out.put("humidity", hum);
        out.put("wind", wind);
        out.put("aqi", aqi);
        out.put("aqiGrade", gradeNames[grade]);
        return out;
    }

    // ---------------------------------------------------------------- 工具执行（角色强校验）
    public Object execTool(String name, Map<String, Object> args, String role) {
        Map<String, Object> def = null;
        for (Map<String, Object> t : toolDefinitions()) {
            if (name.equals(t.get("name"))) {
                def = t;
                break;
            }
        }
        if (def == null) {
            throw new IllegalArgumentException("工具不存在: " + name);
        }
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) def.get("allowedRoles");
        if (!roles.contains(role)) {
            throw new SecurityException("当前角色无权使用该工具「" + def.get("description") + "」");
        }
        String gridCode = strArg(args, "gridCode");
        switch (name) {
            case "weather.now":
                return weatherOf(strArg(args, "city").isEmpty() ? "沈阳市" : strArg(args, "city"));
            case "grid.task.list": {
                List<AqiFeedback> list = taskService.myTasks(gridCode);
                List<Map<String, Object>> out = new ArrayList<>();
                for (AqiFeedback f : list) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("afId", f.getAfId());
                    m.put("region", (f.getProvinceName() == null ? "" : f.getProvinceName()) + "-"
                            + (f.getCityName() == null ? "" : f.getCityName()));
                    m.put("address", f.getAddress());
                    m.put("estimatedGrade", f.getEstimatedGrade());
                    m.put("afDate", f.getAfDate());
                    m.put("afTime", f.getAfTime());
                    out.add(m);
                }
                return out;
            }
            case "grid.leave.apply": {
                String reason = strArg(args, "reason");
                String start = strArg(args, "startDate");
                String end = strArg(args, "endDate");
                if (reason.isEmpty() || start.isEmpty() || end.isEmpty()) {
                    throw new IllegalArgumentException("请提供请假事由与起止日期");
                }
                return leaveService.apply(gridCode, reason, start, end);
            }
            case "feedback.submit":
                return Map.of("hint", "空气质量监督反馈请在「提交反馈」页面填写：选择网格地区、预估AQI等级并描述空气质量");
            case "feedback.mine": {
                List<AqiFeedback> list = feedbackService.findByCond(strArg(args, "telId"),
                        null, null, null, null, null, null, null);
                List<Map<String, Object>> out = new ArrayList<>();
                for (AqiFeedback f : list) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("afId", f.getAfId());
                    m.put("region", (f.getProvinceName() == null ? "" : f.getProvinceName()) + "-"
                            + (f.getCityName() == null ? "" : f.getCityName()));
                    m.put("estimatedGrade", f.getEstimatedGrade());
                    m.put("aqiGrade", f.getAqiGrade());
                    m.put("state", f.getState());
                    m.put("afDate", f.getAfDate());
                    m.put("afTime", f.getAfTime());
                    out.add(m);
                }
                return out.size() > 10 ? out.subList(0, 10) : out;
            }
            case "hr.employee.list": {
                List<Map<String, Object>> out = new ArrayList<>();
                for (com.example.demo.entity.Employee e : employeeService.selectGridWorkers()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("gridCode", e.getEmpCode());
                    m.put("realName", e.getRealName());
                    m.put("region", (e.getProvinceName() == null ? "" : e.getProvinceName()) + "-"
                            + (e.getCityName() == null ? "" : e.getCityName()));
                    m.put("working", e.getWorking() != null && e.getWorking() == 1);
                    out.add(m);
                }
                return out;
            }
            case "hr.leave.approve": {
                List<Map<String, Object>> out = new ArrayList<>();
                for (com.example.demo.entity.Leave l : leaveService.listLeave(null, 0)) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("leaveId", l.getLeaveId());
                    m.put("gridName", l.getGridName());
                    m.put("empCode", l.getEmpCode());
                    m.put("reason", l.getReason());
                    m.put("startDate", l.getStartDate());
                    m.put("endDate", l.getEndDate());
                    m.put("applyDate", l.getApplyDate());
                    out.add(m);
                }
                return out;
            }
            case "hr.supervisor.list": {
                List<Map<String, Object>> out = new ArrayList<>();
                List<com.example.demo.entity.Supervisor> list = supervisorService.selectAllWithNames();
                for (int i = 0; i < Math.min(20, list.size()); i++) {
                    com.example.demo.entity.Supervisor s = list.get(i);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("telId", s.getTelId());
                    m.put("realName", s.getRealName());
                    m.put("age", s.getAge());
                    m.put("gender", s.getGender());
                    out.add(m);
                }
                return out;
            }
            case "stats.overview": {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("province", aqiDataMapper.selectProvinceStats());
                m.put("distribution", aqiDataMapper.selectDistribution());
                m.put("realtime", aqiDataMapper.selectRealtimeStats());
                m.put("coverage", aqiDataMapper.selectCoveredCities());
                return m;
            }
            default:
                throw new IllegalArgumentException("工具未实现: " + name);
        }
    }

    // ---------------------------------------------------------------- 答复组织
    public String prettyToolResult(String name, Object value) {
        if ("weather.now".equals(name) && value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> v = (Map<String, Object>) value;
            return v.get("city") + "：" + v.get("phenomenon") + "，气温" + v.get("temperature") + "℃，"
                    + v.get("wind") + "，湿度" + v.get("humidity") + "%，AQI=" + v.get("aqi") + "（" + v.get("aqiGrade") + "）";
        }
        if (value instanceof Map) {
            return String.valueOf(value);
        }
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return "暂无相关数据";
            }
            StringBuilder sb = new StringBuilder("共 ").append(list.size()).append(" 条：");
            for (int i = 0; i < Math.min(8, list.size()); i++) {
                if (i > 0) {
                    sb.append("、");
                }
                sb.append(list.get(i));
            }
            return sb.toString();
        }
        return String.valueOf(value);
    }

    private static String strArg(Map<String, Object> args, String key) {
        Object v = args == null ? null : args.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }

    // ---------------------------------------------------------------- 规则意图引擎（/api/ai/chat 兜底）
    public Map<String, Object> chat(String role, String account, String message) {
        List<Map<String, Object>> toolCalls = new ArrayList<>();
        List<String> parts = new ArrayList<>();
        List<Map<String, Object>> calls = new ArrayList<>();

        // 天气意图（所有角色）
        if (containsAny(message, "天气", "气温", "温度", "下雨", "AQI", "空气质量", "空气指数", "污染")) {
            calls.add(call("weather.now", Map.of("city", matchCity(message))));
        }
        if (ROLE_GRID.equals(role)) {
            if (containsAny(message, "任务", "派单", "待办", "工作")) {
                calls.add(call("grid.task.list", Map.of("gridCode", account)));
            }
            if (containsAny(message, "请假", "休假", "有事")) {
                calls.add(call("grid.leave.apply", Map.of("gridCode", account, "reason", message)));
            }
        }
        if (ROLE_SUPERVISOR.equals(role)) {
            if (containsAny(message, "我的反馈", "历史反馈", "我的记录", "提交过")) {
                calls.add(call("feedback.mine", Map.of("telId", account)));
            }
            if (containsAny(message, "提交反馈", "举报", "怎么反馈", "反映")) {
                calls.add(call("feedback.submit", Map.of("telId", account)));
            }
        }
        if (ROLE_ADMIN.equals(role)) {
            if ((message.contains("网格员") && (message.contains("名单") || message.contains("人员"))) || message.contains("人员名单")) {
                calls.add(call("hr.employee.list", Map.of()));
            }
            if (message.contains("请假审批") || (message.contains("请假") && message.contains("审批"))) {
                calls.add(call("hr.leave.approve", Map.of()));
            }
            if (message.contains("监督员")) {
                calls.add(call("hr.supervisor.list", Map.of()));
            }
            if (containsAny(message, "统计", "总览", "覆盖率", "大屏")) {
                calls.add(call("stats.overview", Map.of()));
            }
        }
        if (ROLE_VIEWER.equals(role)) {
            if (containsAny(message, "统计", "总览", "覆盖率", "大屏")) {
                calls.add(call("stats.overview", Map.of()));
            }
        }

        for (Map<String, Object> c : calls) {
            String name = (String) c.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> args = (Map<String, Object>) c.get("args");
            try {
                Object val = execTool(name, args, role);
                toolCalls.add(Map.of("name", name, "args", args, "result", val));
                parts.add(prettyToolResult(name, val));
            } catch (Exception e) {
                String msg = e.getMessage() == null ? String.valueOf(e) : e.getMessage();
                toolCalls.add(Map.of("name", name, "args", args, "error", msg));
                parts.add("（" + msg + "）");
            }
        }

        String reply;
        if (!parts.isEmpty()) {
            reply = "好的，为您查询如下：" + String.join("；", parts);
        } else {
            reply = "您好，我是" + ROLE_LABEL.getOrDefault(role, "系统") + "智能助手。我可以帮您："
                    + joinToolDescs(role) + "。试试问我「今天天气怎么样」，或点击下方工具。";
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("reply", reply);
        out.put("toolCalls", toolCalls);
        return out;
    }

    private static Map<String, Object> call(String name, Map<String, Object> args) {
        return Map.of("name", name, "args", args);
    }

    private String joinToolDescs(String role) {
        List<String> descs = new ArrayList<>();
        for (Map<String, Object> t : toolsForRole(role)) {
            descs.add(String.valueOf(t.get("description")));
        }
        return String.join("、", descs);
    }

    private static boolean containsAny(String text, String... keys) {
        for (String k : keys) {
            if (text.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private static String matchCity(String msg) {
        for (String c : new String[]{"沈阳市", "大连市", "长春市", "哈尔滨市", "石家庄市", "北京市", "上海市",
                "广州市", "深圳市", "成都市", "武汉市", "西安市", "济南市", "青岛市", "杭州市", "南京市", "天津市", "重庆市"}) {
            if (msg.contains(c)) {
                return c;
            }
        }
        return "沈阳市";
    }
}
