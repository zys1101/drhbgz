package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.GridDemand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 网格员增员请求 Mapper
 */
@Mapper
public interface GridDemandMapper extends BaseMapper<GridDemand> {

    /** 增员请求列表（带省市名称），可按状态筛选 */
    @Select("<script>" +
            "SELECT d.*, p.province_name, c.city_name FROM grid_demand d " +
            "LEFT JOIN grid_province p ON d.province_id = p.province_id " +
            "LEFT JOIN grid_city c ON d.city_id = c.city_id " +
            "<where>" +
            " <if test='state != null'> AND d.state = #{state} </if>" +
            " <if test='cityId != null'> AND d.city_id = #{cityId} </if>" +
            "</where>" +
            "ORDER BY d.state ASC, d.demand_id DESC" +
            "</script>")
    List<GridDemand> list(@Param("state") Integer state, @Param("cityId") Integer cityId);
}
