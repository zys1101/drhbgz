package com.example.demo.controller;

import com.example.demo.mapper.AqiDataMapper;
import com.example.demo.service.IGridCityService;
import com.example.demo.service.IGridProvinceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.GridCity;
import com.example.demo.entity.GridProvince;
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
 * 统计数据接口（NEPM统计数据管理 / NEPV可视化大屏）
 */
@Tag(name = "统计数据")
@RestController
@RequestMapping("/stats")
@AllArgsConstructor
@CrossOrigin
public class StatsController {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AqiDataMapper aqiDataMapper;
    private final IGridProvinceService provinceService;
    private final IGridCityService cityService;

    /**
     * 1. 省分组检查统计：SO2/CO/PM2.5/AQI浓度等级超标累计数量
     */
    @GetMapping("/province")
    @Operation(summary = "省分组超标统计")
    public ResultVO provinceStats() {
        return new ResultVO(200, "查询成功", aqiDataMapper.selectProvinceStats());
    }

    /**
     * 2. AQI指数分布统计：以AQI指数级别分组统计累计数量
     */
    @GetMapping("/distribution")
    @Operation(summary = "AQI指数分布统计")
    public ResultVO distribution() {
        List<Map<String, Object>> rows = aqiDataMapper.selectDistribution();
        Map<Integer, Long> countByGrade = new HashMap<>();
        for (Map<String, Object> row : rows) {
            int grade = ((Number) row.get("grade")).intValue();
            long cnt = ((Number) row.get("cnt")).longValue();
            countByGrade.put(grade, cnt);
        }
        String[] names = {"", "一级（优）", "二级（良）", "三级（轻度污染）",
                "四级（中度污染）", "五级（重度污染）", "六级（严重污染）"};
        List<Map<String, Object>> result = new ArrayList<>();
        for (int g = 1; g <= 6; g++) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", names[g]);
            item.put("value", countByGrade.getOrDefault(g, 0L));
            result.add(item);
        }
        return new ResultVO(200, "查询成功", result);
    }

    /**
     * 3. AQI指数趋势统计：当前12个月内每个月的全国AQI超标累计数量
     */
    @GetMapping("/trend")
    @Operation(summary = "AQI指数趋势统计")
    public ResultVO trend() {
        // 近12个月（含当月）
        List<String> months = new ArrayList<>();
        LocalDate cur = LocalDate.now().withDayOfMonth(1);
        for (int i = 11; i >= 0; i--) {
            months.add(cur.minusMonths(i).format(MONTH_FMT));
        }
        String startDate = months.get(0) + "-01";
        Map<String, Long> exceedByMonth = new HashMap<>();
        for (Map<String, Object> row : aqiDataMapper.selectTrend(startDate)) {
            exceedByMonth.put(String.valueOf(row.get("month")), ((Number) row.get("exceed")).longValue());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String m : months) {
            Map<String, Object> item = new HashMap<>();
            item.put("month", m);
            item.put("exceed", exceedByMonth.getOrDefault(m, 0L));
            result.add(item);
        }
        return new ResultVO(200, "查询成功", result);
    }

    /**
     * 4. 空气质量检测数量实时统计：累计检测数 / 良好累计 / 超标累计
     */
    @GetMapping("/realtime")
    @Operation(summary = "检测数量实时统计")
    public ResultVO realtime() {
        Map<String, Object> row = aqiDataMapper.selectRealtimeStats();
        Map<String, Object> result = new HashMap<>();
        result.put("total", asLong(row == null ? null : row.get("total")));
        result.put("good", asLong(row == null ? null : row.get("good")));
        result.put("exceed", asLong(row == null ? null : row.get("exceed")));
        return new ResultVO(200, "查询成功", result);
    }

    /**
     * 5. 全国网格覆盖率统计：
     * 使用本系统的网格区域（有网格员或有反馈记录的城市）占全国34省 / 106大城市的比例
     */
    @GetMapping("/coverage")
    @Operation(summary = "全国网格覆盖率统计")
    public ResultVO coverage() {
        long provinceTotal = provinceService.count();
        long cityTotal = cityService.count(
                new LambdaQueryWrapper<GridCity>().eq(GridCity::getIsBigCity, 1));
        List<Map<String, Object>> covered = aqiDataMapper.selectCoveredCities();

        Set<String> provinceSet = new TreeSet<>();
        List<Map<String, Object>> coveredList = new ArrayList<>();
        for (Map<String, Object> c : covered) {
            provinceSet.add(String.valueOf(c.get("provinceName")));
            Map<String, Object> item = new HashMap<>();
            item.put("province", c.get("provinceName"));
            item.put("city", c.get("cityName"));
            coveredList.add(item);
        }
        coveredList.sort(Comparator.comparing(a -> String.valueOf(a.get("province"))));

        Map<String, Object> result = new HashMap<>();
        result.put("provinceCovered", provinceSet.size());
        result.put("provinceTotal", provinceTotal);
        result.put("cityCovered", covered.size());
        result.put("cityTotal", cityTotal);
        result.put("coveredList", coveredList);
        return new ResultVO(200, "查询成功", result);
    }

    private long asLong(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }
}
