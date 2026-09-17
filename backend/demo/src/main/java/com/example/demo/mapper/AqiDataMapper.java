package com.example.demo.mapper;

import com.example.demo.entity.AqiData;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 网格员实测AQI数据 Mapper 接口（含统计查询）
 */
@Mapper
public interface AqiDataMapper extends BaseMapper<AqiData> {

    /** 实测数据列表（关联反馈/省/市信息），按提交时间倒序 */
    @Select("SELECT d.*, f.province_id, f.city_id, f.address, f.tel_id, f.estimated_grade, f.information, " +
            "p.province_name, c.city_name " +
            "FROM aqi_data d " +
            "JOIN aqi_feedback f ON d.af_id = f.af_id " +
            "JOIN grid_province p ON f.province_id = p.province_id " +
            "JOIN grid_city c ON f.city_id = c.city_id " +
            "ORDER BY d.data_id DESC")
    List<AqiData> selectDataList();

    /** 实测数据详情 */
    @Select("SELECT d.*, f.province_id, f.city_id, f.address, f.tel_id, f.estimated_grade, f.information, " +
            "p.province_name, c.city_name " +
            "FROM aqi_data d " +
            "JOIN aqi_feedback f ON d.af_id = f.af_id " +
            "JOIN grid_province p ON f.province_id = p.province_id " +
            "JOIN grid_city c ON f.city_id = c.city_id " +
            "WHERE d.data_id = #{dataId}")
    AqiData selectDataDetail(@Param("dataId") Integer dataId);

    // ---------------- 统计（NEPM统计数据管理 / NEPV可视化大屏） ----------------

    /**
     * 1. 省分组检查统计：以省为分组统计各污染物浓度超标累计数量
     * 超标口径：浓度等级 >= 三级（轻度污染及以上）
     */
    @Select("SELECT p.province_name AS province, " +
            "SUM(CASE WHEN d.so2_grade >= 3 THEN 1 ELSE 0 END) AS so2, " +
            "SUM(CASE WHEN d.co_grade >= 3 THEN 1 ELSE 0 END) AS co, " +
            "SUM(CASE WHEN d.pm25_grade >= 3 THEN 1 ELSE 0 END) AS pm25, " +
            "SUM(CASE WHEN d.aqi_grade >= 3 THEN 1 ELSE 0 END) AS aqi, " +
            "COUNT(*) AS total " +
            "FROM aqi_data d " +
            "JOIN aqi_feedback f ON d.af_id = f.af_id " +
            "JOIN grid_province p ON f.province_id = p.province_id " +
            "WHERE d.state = 1 " +
            "GROUP BY p.province_id, p.province_name " +
            "ORDER BY aqi DESC")
    List<Map<String, Object>> selectProvinceStats();

    /** 2. AQI指数分布统计：按AQI等级分组计数（仅已确认数据） */
    @Select("SELECT d.aqi_grade AS grade, COUNT(*) AS cnt FROM aqi_data d " +
            "WHERE d.state = 1 GROUP BY d.aqi_grade")
    List<Map<String, Object>> selectDistribution();

    /** 3. AQI指数趋势统计：指定起始日后按月统计全国AQI超标累计数量 */
    @Select("SELECT LEFT(d.submit_date, 7) AS month, COUNT(*) AS exceed FROM aqi_data d " +
            "WHERE d.state = 1 AND d.aqi_grade >= 3 AND d.submit_date >= #{startDate} " +
            "GROUP BY LEFT(d.submit_date, 7)")
    List<Map<String, Object>> selectTrend(@Param("startDate") String startDate);

    /** 4. 空气质量检测数量实时统计 */
    @Select("SELECT COUNT(*) AS total, " +
            "SUM(CASE WHEN d.aqi_grade <= 2 THEN 1 ELSE 0 END) AS good, " +
            "SUM(CASE WHEN d.aqi_grade >= 3 THEN 1 ELSE 0 END) AS exceed " +
            "FROM aqi_data d WHERE d.state = 1")
    Map<String, Object> selectRealtimeStats();

    /** 5. 全国网格覆盖率：当前使用本系统的网格城市（有网格员或有反馈记录的城市） */
    @Select("SELECT DISTINCT c.city_id AS cityId, c.city_name AS cityName, p.province_name AS provinceName " +
            "FROM grid_city c " +
            "JOIN grid_province p ON c.province_id = p.province_id " +
            "WHERE c.city_id IN (" +
            "  SELECT city_id FROM employee WHERE role = 'grid' AND city_id IS NOT NULL " +
            "  UNION " +
            "  SELECT city_id FROM aqi_feedback)")
    List<Map<String, Object>> selectCoveredCities();
}
