package com.example.demo.mapper;

import com.example.demo.entity.AqiFeedback;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 空气质量公众监督反馈信息 Mapper 接口
 */
@Mapper
public interface AqiFeedbackMapper extends BaseMapper<AqiFeedback> {

    /** 查询所有反馈（带省/市名称、已确认实测数据），按反馈时间倒序 */
    @Select("SELECT aqi.*, p.province_name, c.city_name, e.real_name AS grid_name, e.emp_code AS grid_code, " +
            "d.aqi_grade, d.so2_grade, d.co_grade, d.pm25_grade, " +
            "d.submit_date AS measure_date, d.submit_time AS measure_time " +
            "FROM aqi_feedback aqi " +
            "JOIN grid_province p ON aqi.province_id = p.province_id " +
            "JOIN grid_city c ON aqi.city_id = c.city_id " +
            "LEFT JOIN employee e ON aqi.gm_id = e.emp_id " +
            "LEFT JOIN aqi_data d ON d.af_id = aqi.af_id AND d.state = 1 " +
            "ORDER BY aqi.af_date DESC, aqi.af_time DESC")
    List<AqiFeedback> findAll();

    /**
     * 条件查询反馈列表（NEPM公众监督数据列表查询 / NEPS历史反馈查询）
     */
    @Select("<script>" +
            "SELECT aqi.*, p.province_name, c.city_name, e.real_name AS grid_name, e.emp_code AS grid_code, " +
            "d.aqi_grade, d.so2_grade, d.co_grade, d.pm25_grade, " +
            "d.submit_date AS measure_date, d.submit_time AS measure_time " +
            "FROM aqi_feedback aqi " +
            "JOIN grid_province p ON aqi.province_id = p.province_id " +
            "JOIN grid_city c ON aqi.city_id = c.city_id " +
            "LEFT JOIN employee e ON aqi.gm_id = e.emp_id " +
            "LEFT JOIN aqi_data d ON d.af_id = aqi.af_id AND d.state = 1 " +
            "<where>" +
            " <if test='telId != null and telId != \"\"'> AND aqi.tel_id = #{telId} </if>" +
            " <if test='provinceId != null'> AND aqi.province_id = #{provinceId} </if>" +
            " <if test='cityId != null'> AND aqi.city_id = #{cityId} </if>" +
            " <if test='grade != null'> AND aqi.estimated_grade = #{grade} </if>" +
            " <if test='state != null'> AND aqi.state = #{state} </if>" +
            " <if test='dateFrom != null and dateFrom != \"\"'> AND aqi.af_date &gt;= #{dateFrom} </if>" +
            " <if test='dateTo != null and dateTo != \"\"'> AND aqi.af_date &lt;= #{dateTo} </if>" +
            " <if test='keyword != null and keyword != \"\"'> AND (aqi.address LIKE CONCAT('%', #{keyword}, '%')" +
            " OR aqi.information LIKE CONCAT('%', #{keyword}, '%') OR aqi.tel_id LIKE CONCAT('%', #{keyword}, '%')" +
            " OR p.province_name LIKE CONCAT('%', #{keyword}, '%') OR c.city_name LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "</where>" +
            "ORDER BY aqi.af_date DESC, aqi.af_time DESC" +
            "</script>")
    List<AqiFeedback> selectByCond(@Param("telId") String telId,
                                   @Param("provinceId") Integer provinceId,
                                   @Param("cityId") Integer cityId,
                                   @Param("grade") Integer grade,
                                   @Param("state") Integer state,
                                   @Param("dateFrom") String dateFrom,
                                   @Param("dateTo") String dateTo,
                                   @Param("keyword") String keyword);
}
