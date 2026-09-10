package com.example.demo.controller;

import com.example.demo.common.ResultVO;
import com.example.demo.entity.AqiData;
import com.example.demo.service.IAqiDataService;
import com.example.demo.service.ITaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * NEPM确认AQI数据接口（用例3-11）
 */
@Tag(name = "确认AQI数据")
@RestController
@RequestMapping("/aqiData")
@AllArgsConstructor
public class AqiDataController {

    private final IAqiDataService aqiDataService;
    private final ITaskService taskService;

    /**
     * 浏览网格员提交的AQI数据列表（带关联信息）
     */
    @GetMapping("/list")
    @Operation(summary = "确认AQI数据列表")
    public ResultVO list() {
        List<AqiData> list = aqiDataService.selectDataList();
        return new ResultVO(200, "查询成功", list);
    }

    /**
     * 浏览提交AQI数据详情
     */
    @GetMapping("/find/{dataId}")
    @Operation(summary = "AQI数据详情")
    public ResultVO find(@PathVariable Integer dataId) {
        AqiData data = aqiDataService.selectDataDetail(dataId);
        return new ResultVO(200, "查询成功", data);
    }

    /**
     * 确认数据无误，纳入统计
     */
    @GetMapping("/confirm/{dataId}")
    @Operation(summary = "确认AQI数据")
    public ResultVO confirm(@PathVariable Integer dataId) {
        taskService.confirm(dataId);
        return new ResultVO(200, "数据已确认，纳入统计范围", true);
    }

    /**
     * 退回异常数据：任务状态置为“待指派”，需重新指派检测
     */
    @GetMapping("/reject/{dataId}")
    @Operation(summary = "退回AQI数据")
    public ResultVO reject(@PathVariable Integer dataId) {
        taskService.reject(dataId);
        return new ResultVO(200, "已退回，任务重新进入待指派状态", true);
    }
}
