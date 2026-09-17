package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.ResultVO;
import com.example.demo.entity.AqiFeedback;
import com.example.demo.entity.Employee;
import com.example.demo.entity.GridDemand;
import com.example.demo.mapper.AqiFeedbackMapper;
import com.example.demo.mapper.DecisionStatsMapper;
import com.example.demo.mapper.EmployeeMapper;
import com.example.demo.mapper.GridDemandMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 决策者（NEPV）看板决策依据接口。
 *
 * 1. /stats/workforce        网格员人力：总数 / 空闲 / 忙碌 / 请假中 / 是否需增员
 * 2. /stats/feedbackCoverage 反馈覆盖度环比：哪些城市反馈多、哪些少，
 *                            以及“少”的原因是环境确实良好，还是反馈缺失/覆盖不足
 *
 * 口径说明（与 preview 服务保持一致）：
 *  - 忙碌：在岗（working=1）且名下存在 state=1（已指派未完成）任务的网格员
 *  - 空闲：在岗但没有在办任务的网格员
 *  - 需要增员：存在未处理的增员请求；或仍有待指派任务但已无空闲网格员
 *  - 覆盖不足：该城市没有在岗网格员（有反馈却无人可派）
 *  - 环境良好：反馈环比下降，且该城市已确认实测的平均 AQI 等级 ≤ 2（优/良）
 */
@Tag(name = "决策者看板统计")
@RestController
@RequestMapping("/stats")
@AllArgsConstructor
@CrossOrigin
public class DecisionStatsController {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final EmployeeMapper employeeMapper;
    private final AqiFeedbackMapper aqiFeedbackMapper;
    private final GridDemandMapper gridDemandMapper;
    private final DecisionStatsMapper decisionStatsMapper;
    private final com.example.demo.config.NepTaskProperties taskProperties;

    /** 网格员人力看板 */
    @GetMapping("/workforce")
    @Operation(summary = "网格员人力看板")
    public ResultVO workforce() {
        List<Employee> workers = employeeMapper.selectGridWorkers();

        Map<Integer, Long> busyByGrid = new HashMap<>();
        for (Map<String, Object> r : decisionStatsMapper.selectBusyByGrid()) {
            busyByGrid.put(asInt(r.get("gmId")), asLong(r.get("cnt")));
        }

        int total = workers.size();
        int onLeave = 0;
        int busy = 0;
        Map<Integer, Map<String, Object>> regionMap = new LinkedHashMap<>();
        for (Employee w : workers) {
            boolean working = w.getWorking() != null && w.getWorking() == 1;
            if (!working) {
                onLeave++;
            }
            boolean isBusy = working && busyByGrid.getOrDefault(w.getEmpId(), 0L) > 0;
            if (isBusy) {
                busy++;
            }

            Integer key = w.getCityId() == null ? -1 : w.getCityId();
            Map<String, Object> region = regionMap.computeIfAbsent(key, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("provinceName", w.getProvinceName());
                m.put("cityName", w.getCityName());
                m.put("total", 0);
                m.put("working", 0);
                m.put("busy", 0);
                m.put("idle", 0);
                return m;
            });
            region.put("total", (Integer) region.get("total") + 1);
            if (working) {
                region.put("working", (Integer) region.get("working") + 1);
                if (isBusy) {
                    region.put("busy", (Integer) region.get("busy") + 1);
                } else {
                    region.put("idle", (Integer) region.get("idle") + 1);
                }
            }
        }

        int workingCount = total - onLeave;
        int idle = workingCount - busy;

        long pendingTasks = aqiFeedbackMapper.selectCount(
                new LambdaQueryWrapper<AqiFeedback>().eq(AqiFeedback::getState, AqiFeedback.STATE_UNASSIGNED));
        List<GridDemand> pendingDemands = gridDemandMapper.list(GridDemand.STATE_PENDING, null);

        // 是否需增员：① 有未处理的增员请求（某区域一个在岗网格员都没有）；
        //            ② 待指派任务量已超过“空闲网格员 × 人均承载上限”
        int capacity = taskProperties.getWorkerCapacity();
        int demandNeed = pendingDemands.size();
        int backlogNeed = 0;
        if (capacity > 0 && pendingTasks > (long) idle * capacity) {
            long extra = pendingTasks - (long) idle * capacity;
            backlogNeed = (int) Math.ceil(extra / (double) capacity);
        }
        int suggestAdd = demandNeed + backlogNeed;
        boolean needMore = suggestAdd > 0;

        List<Map<String, Object>> lackRegions = new ArrayList<>();
        for (GridDemand d : pendingDemands) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("provinceName", d.getProvinceName());
            m.put("cityName", d.getCityName());
            m.put("reason", d.getReason());
            m.put("afId", d.getAfId());
            lackRegions.add(m);
        }

        // 给出“为什么需要增员”的可读依据，便于决策者直接判断
        List<String> needReasons = new ArrayList<>();
        for (GridDemand d : pendingDemands) {
            needReasons.add(String.format("%s · %s 无在岗网格员，已提交增员请求（反馈 %s）",
                    d.getProvinceName(), d.getCityName(),
                    d.getAfId() == null ? "-" : String.valueOf(d.getAfId())));
        }
        if (backlogNeed > 0) {
            needReasons.add(String.format("待指派任务 %d 条，超出 %d 名空闲网格员按人均 %d 条的承载能力，建议增员 %d 人",
                    pendingTasks, idle, capacity, backlogNeed));
        }

        // 可发起增援申请的区域：有任务却一个在岗网格员都没有。
        // 决策者大屏据此给出“提交增援申请”入口，并标记该区域是否已提交过（避免重复提交）。
        Map<String, GridDemand> demandByRegion = new HashMap<>();
        for (GridDemand d : pendingDemands) {
            demandByRegion.put(d.getProvinceId() + "-" + d.getCityId(), d);
        }
        List<Map<String, Object>> needWorkerRegions = new ArrayList<>();
        for (Map<String, Object> r : decisionStatsMapper.selectRegionsWithoutWorker()) {
            Map<String, Object> m = new LinkedHashMap<>();
            Integer pid = asInt(r.get("provinceId"));
            Integer cid = asInt(r.get("cityId"));
            m.put("provinceId", pid);
            m.put("cityId", cid);
            m.put("provinceName", r.get("provinceName"));
            m.put("cityName", r.get("cityName"));
            m.put("pendingTasks", asLong(r.get("pendingTasks")));
            GridDemand exist = demandByRegion.get(pid + "-" + cid);
            m.put("hasDemand", exist != null);
            m.put("demandId", exist == null ? null : exist.getDemandId());
            needWorkerRegions.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("working", workingCount);
        result.put("onLeave", onLeave);
        result.put("busy", busy);
        result.put("idle", idle);
        result.put("capacity", capacity);
        result.put("pendingTasks", pendingTasks);
        result.put("pendingDemands", demandNeed);
        result.put("suggestAdd", suggestAdd);
        result.put("needMore", needMore);
        result.put("needReasons", needReasons);
        result.put("regions", new ArrayList<>(regionMap.values()));
        result.put("lackRegions", lackRegions);
        result.put("needWorkerRegions", needWorkerRegions);
        return new ResultVO(200, "查询成功", result);
    }

    /** 反馈覆盖度环比分析 */
    @GetMapping("/feedbackCoverage")
    @Operation(summary = "反馈覆盖度环比分析")
    public ResultVO feedbackCoverage() {
        LocalDate today = LocalDate.now();
        String currentMonth = today.format(MONTH_FMT);
        String previousMonth = today.minusMonths(1).format(MONTH_FMT);

        // 按城市聚合本月/上月反馈数
        Map<Integer, Map<String, Object>> cityMap = new LinkedHashMap<>();
        for (Map<String, Object> r : decisionStatsMapper.selectFeedbackByCityMonth(previousMonth + "-01")) {
            Integer cityId = asInt(r.get("cityId"));
            Map<String, Object> c = cityMap.computeIfAbsent(cityId, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("provinceName", r.get("provinceName"));
                m.put("cityName", r.get("cityName"));
                m.put("cityId", k);
                m.put("current", 0L);
                m.put("previous", 0L);
                return m;
            });
            String month = String.valueOf(r.get("month"));
            long cnt = asLong(r.get("cnt"));
            if (currentMonth.equals(month)) {
                c.put("current", asLong(c.get("current")) + cnt);
            } else if (previousMonth.equals(month)) {
                c.put("previous", asLong(c.get("previous")) + cnt);
            }
        }

        // 各城市在岗网格员数
        Map<Integer, Integer> workingByCity = new HashMap<>();
        for (Employee w : employeeMapper.selectGridWorkers()) {
            if (w.getWorking() != null && w.getWorking() == 1 && w.getCityId() != null) {
                workingByCity.merge(w.getCityId(), 1, Integer::sum);
            }
        }
        // 各城市已确认实测的平均等级
        Map<Integer, Double> avgGradeByCity = new HashMap<>();
        Map<Integer, Long> measuredByCity = new HashMap<>();
        for (Map<String, Object> r : decisionStatsMapper.selectCityAvgGrade()) {
            Integer cityId = asInt(r.get("cityId"));
            Object avg = r.get("avgGrade");
            avgGradeByCity.put(cityId, avg == null ? null : ((Number) avg).doubleValue());
            measuredByCity.put(cityId, asLong(r.get("measured")));
        }
        // 各城市待指派任务数
        Map<Integer, Long> pendingByCity = new HashMap<>();
        for (Map<String, Object> r : decisionStatsMapper.selectPendingByCity()) {
            pendingByCity.put(asInt(r.get("cityId")), asLong(r.get("cnt")));
        }

        List<Map<String, Object>> cities = new ArrayList<>();
        long currentTotal = 0;
        long previousTotal = 0;
        for (Map.Entry<Integer, Map<String, Object>> e : cityMap.entrySet()) {
            Integer cityId = e.getKey();
            Map<String, Object> c = e.getValue();
            long cur = asLong(c.get("current"));
            long prev = asLong(c.get("previous"));
            currentTotal += cur;
            previousTotal += prev;

            int working = workingByCity.getOrDefault(cityId, 0);
            Double avgGrade = avgGradeByCity.get(cityId);
            long measured = measuredByCity.getOrDefault(cityId, 0L);
            long pending = pendingByCity.getOrDefault(cityId, 0L);

            c.put("working", working);
            c.put("avgGrade", avgGrade);
            c.put("measured", measured);
            c.put("pendingTasks", pending);
            c.put("delta", cur - prev);
            c.put("deltaPercent", prev == 0 ? null : Math.round((cur - prev) * 1000.0 / prev) / 10.0);
            c.put("reason", reasonOf(working, cur, prev, avgGrade, measured));
            cities.add(c);
        }

        // 反馈“多/少”榜单：按本月反馈数排序
        List<Map<String, Object>> sorted = new ArrayList<>(cities);
        sorted.sort(Comparator.comparingLong((Map<String, Object> m) -> asLong(m.get("current"))).reversed());
        List<Map<String, Object>> more = sorted.subList(0, Math.min(5, sorted.size()));
        List<Map<String, Object>> less = new ArrayList<>(sorted);
        Collections.reverse(less);
        less = less.subList(0, Math.min(5, less.size()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentMonth", currentMonth);
        result.put("previousMonth", previousMonth);
        result.put("currentTotal", currentTotal);
        result.put("previousTotal", previousTotal);
        result.put("cities", cities);
        result.put("more", more);
        result.put("less", less);
        return new ResultVO(200, "查询成功", result);
    }

    /**
     * 判断某城市反馈“多/少”的原因。
     * 关键区分：反馈少究竟是环境确实良好，还是反馈缺失/覆盖不足。
     */
    private String reasonOf(int working, long cur, long prev, Double avgGrade, long measured) {
        if (working == 0) {
            return "覆盖不足：该区域无在岗网格员，反馈少更可能是缺少检测覆盖而非环境良好";
        }
        boolean goodAir = avgGrade != null && measured > 0 && avgGrade <= 2.0;
        if (goodAir && cur <= prev) {
            return String.format("环境良好：已确认实测平均 AQI 等级 %.1f（优/良，样本 %d），反馈自然较少",
                    avgGrade, measured);
        }
        if (cur < prev) {
            return "反馈减少：该区域有在岗网格员，但反馈环比下降，需关注公众参与度（可能宣传/引导不足）";
        }
        if (cur > prev) {
            return "反馈增加：环比上升，建议关注该区域空气质量与治理进展";
        }
        return "基本持平：需继续观察（该区域已确认的实测样本不足，暂无法判定为环境良好）";
    }

    private static int asInt(Object v) {
        return v == null ? -1 : ((Number) v).intValue();
    }

    private static long asLong(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }
}
