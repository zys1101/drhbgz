package com.example.demo.mapper;

import com.example.demo.entity.Supervisor;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 公众监督员 Mapper 接口
 */
@Mapper
public interface SupervisorMapper extends BaseMapper<Supervisor> {

    /** 查询监督员信息（带绑定的省/市名称） */
    @Select("SELECT s.*, p.province_name, c.city_name FROM supervisor s " +
            "LEFT JOIN grid_province p ON s.province_id = p.province_id " +
            "LEFT JOIN grid_city c ON s.city_id = c.city_id " +
            "WHERE s.tel_id = #{telId}")
    Supervisor selectWithNames(@Param("telId") String telId);

    /** 监督员列表（带绑定的省/市名称，按注册时间倒序） */
    @Select("SELECT s.*, p.province_name, c.city_name FROM supervisor s " +
            "LEFT JOIN grid_province p ON s.province_id = p.province_id " +
            "LEFT JOIN grid_city c ON s.city_id = c.city_id " +
            "ORDER BY s.register_date DESC, s.tel_id")
    List<Supervisor> selectAllWithNames();
}
