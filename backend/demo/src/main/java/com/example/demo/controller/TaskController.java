package com.example.demo.controller;

import com.example.demo.common.ResultVO;
import com.example.demo.entity.AqiData;
import com.example.demo.entity.AqiFeedback;
import com.example.demo.service.ITaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 任务流转接口：NEPM指派网格员 / NEPG网格员任务与实测数据
 */
@Tag(name = "任务流转")
@RestController
@RequestMapping("/task")
@AllArgsConstructor
@Slf4j
public class TaskController {

    private final ITaskService taskService;

    /**
     * NEPM指派网格员（用例3-10）
     * 本地指派：当前网格区域有可工作网格员；异地指派：就近安排其它区域网格员
     */
    @PostMapping("/assign")
    @Operation(summary = "指派网格员")
    public ResultVO assign(@RequestBody Map<String, Object> body) {
        Integer afId = body.get("afId") == null ? null : Integer.parseInt(String.valueOf(body.get("afId")));
        String gridCode = body.get("gridCode") == null ? null : String.valueOf(body.get("gridCode"));
        taskService.assign(afId, gridCode);
        return new ResultVO(200, "指派成功", true);
    }

    /**
     * NEPG网格员浏览指派给自己的反馈任务（用例3-7）
     */
    @GetMapping("/list/{gridCode}")
    @Operation(summary = "网格员任务列表")
    public ResultVO myTasks(@PathVariable String gridCode) {
        List<AqiFeedback> list = taskService.myTasks(gridCode);
        return new ResultVO(200, "查询成功", list);
    }

    /**
     * NEPG网格员输入并提交实测AQI数据（用例3-8）
     * AQI = MAX（SO2AQI，COAQI，PM2.5AQI）
     */
    @PostMapping("/measure")
    @Operation(summary = "提交实测AQI数据")
    public ResultVO measure(@RequestBody Map<String, Object> body) {
        Integer afId = toInt(body.get("afId"));
        String gridCode = String.valueOf(body.getOrDefault("gridCode", ""));
        Integer so2Grade = toInt(body.get("so2Grade"));
        Integer coGrade = toInt(body.get("coGrade"));
        Integer pm25Grade = toInt(body.get("pm25Grade"));
        AqiData data = taskService.submitMeasure(afId, gridCode, so2Grade, coGrade, pm25Grade);
        return new ResultVO(200, "提交成功", data);
    }

    /**
     * 回收超时未接单任务（管理员手动触发；平时由 TaskRepoolScheduler 定时自动执行）
     * 规则：已指派超过 nep.task.repool-hours（默认 24 小时）仍未提交实测数据 → 回到“待指派”
     */
    @PostMapping("/repool")
    @Operation(summary = "回收超时未接单任务")
    public ResultVO repool() {
        int n = taskService.repoolTimedOutTasks();
        return new ResultVO(200, n > 0 ? "已回收 " + n + " 条超时任务" : "没有超时未接单的任务", n);
    }

    private Integer toInt(Object v) {
        if (v == null || "".equals(v)) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
