package com.example.demo.controller;

import com.example.demo.common.ResultVO;
import com.example.demo.entity.AqiLevel;
import com.example.demo.service.IAqiLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AQI级别表管理（空气质量指数（AQI）范围及相应类别表）
 */
@Tag(name = "AQI级别管理")
@RestController
@RequestMapping("/aqi")
@AllArgsConstructor
public class AqiLevelController {

    private final IAqiLevelService aqiLevelService;

    /** 查询所有AQI级别 */
    @GetMapping("/list")
    @Operation(summary = "AQI级别列表")
    public ResultVO list() {
        List<AqiLevel> list = aqiLevelService.list();
        return new ResultVO(200, "查询成功", list);
    }

    /** 根据aqiId查询 */
    @GetMapping("/find/{aqiId}")
    @Operation(summary = "AQI级别详情")
    public ResultVO find(@PathVariable Integer aqiId) {
        return new ResultVO(200, "查询成功", aqiLevelService.getById(aqiId));
    }

    /** 新增 */
    @PostMapping("/save")
    @Operation(summary = "新增AQI级别")
    public ResultVO save(@RequestBody AqiLevel level) {
        boolean success = aqiLevelService.save(level);
        return new ResultVO(200, "保存成功", success);
    }

    /** 修改 */
    @PostMapping("/update")
    @Operation(summary = "修改AQI级别")
    public ResultVO update(@RequestBody AqiLevel level) {
        boolean success = aqiLevelService.updateById(level);
        return new ResultVO(200, "更新成功", success);
    }

    /** 删除 */
    @DeleteMapping("/delete/{aqiId}")
    @Operation(summary = "删除AQI级别")
    public ResultVO delete(@PathVariable Integer aqiId) {
        boolean success = aqiLevelService.removeById(aqiId);
        return new ResultVO(200, "删除成功", success);
    }
}
