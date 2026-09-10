package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.ResultVO;
import com.example.demo.entity.GridCity;
import com.example.demo.entity.GridProvince;
import com.example.demo.service.IGridCityService;
import com.example.demo.service.IGridProvinceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 行政区划（省、市二级联动，数据来源于系统行政区划数据）
 */
@Tag(name = "行政区划")
@RestController
@RequestMapping("/province")
@CrossOrigin
public class ProvinceController {

    @Resource
    private IGridProvinceService provinceService;

    @Resource
    private IGridCityService cityService;

    /** 省份列表 */
    @GetMapping("/list")
    @Operation(summary = "省份列表")
    public ResultVO list() {
        List<GridProvince> list = provinceService.list(
                new LambdaQueryWrapper<GridProvince>().orderByAsc(GridProvince::getProvinceId));
        return new ResultVO(200, "查询成功", list);
    }

    /** 根据省编号获取城市列表（仅网格化覆盖的大城市） */
    @GetMapping("/getCitys")
    @Operation(summary = "根据省获取城市列表")
    public ResultVO getCitys(@RequestParam Integer provinceId) {
        List<GridCity> list = cityService.list(
                new LambdaQueryWrapper<GridCity>()
                        .eq(GridCity::getProvinceId, provinceId)
                        .orderByAsc(GridCity::getCityId));
        return new ResultVO(200, "查询成功", list);
    }
}
