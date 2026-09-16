package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 决策者看板专用聚合查询（网格员人力 / 反馈覆盖度环比）。
 *
 * 单独成文件而不塞进既有 Mapper：这些查询只服务于 NEPV 决策看板，
 * 便于与 preview 服务一一对应地核对口径。
 */
@Mapper
public interface DecisionStatsMapper {

    /** 每个网格员当前在办的已指派任务数（state=1） */
    @Select("SELECT gm_id AS gmId, COUNT(*) AS cnt FROM aqi_feedback "
            + "WHERE state = 1 AND gm_id IS NOT NULL GROUP BY gm_id")
    List<Map<String, Object>> selectBusyByGrid();

    /**
     * 近若干个月按“城市 + 月份”的反馈数（用于环比）。
     * af_date 是 'yyyy-MM-dd' 字符串，取前 7 位即月份。
     */
    @Select("SELECT f.city_id AS cityId, p.province_name AS provinceName, c.city_name AS cityName, "
            + "LEFT(f.af_date, 7) AS month, COUNT(*) AS cnt "
            + "FROM aqi_feedback f "
            + "JOIN grid_province p ON f.province_id = p.province_id "
            + "JOIN grid_city c ON f.city_id = c.city_id "
            + "WHERE f.af_date >= #{startMonth} "
            + "GROUP BY f.city_id, p.province_name, c.city_name, LEFT(f.af_date, 7)")
    List<Map<String, Object>> selectFeedbackByCityMonth(@Param("startMonth") String startMonth);

    /** 各城市已确认实测数据的平均 AQI 等级与样本数（用于判断“环境是否确实良好”） */
    @Select("SELECT f.city_id AS cityId, AVG(d.aqi_grade) AS avgGrade, COUNT(*) AS measured "
            + "FROM aqi_data d JOIN aqi_feedback f ON d.af_id = f.af_id "
            + "WHERE d.state = 1 GROUP BY f.city_id")
    List<Map<String, Object>> selectCityAvgGrade();

    /** 各城市历史反馈总量（用于“反馈多/少”排序） */
    @Select("SELECT f.city_id AS cityId, COUNT(*) AS cnt FROM aqi_feedback f GROUP BY f.city_id")
    List<Map<String, Object>> selectFeedbackCountByCity();

    /** 各城市待指派任务数（用于判断哪些区域任务积压） */
    @Select("SELECT f.city_id AS cityId, COUNT(*) AS cnt FROM aqi_feedback f "
            + "WHERE f.state = 0 GROUP BY f.city_id")
    List<Map<String, Object>> selectPendingByCity();
}
