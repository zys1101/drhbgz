package com.example.demo.mapper;

import com.example.demo.entity.Leave;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 请假 Mapper 接口（人员管理/HR）
 */
@Mapper
public interface LeaveMapper extends BaseMapper<Leave> {

    /**
     * 请假记录列表（带网格员编码与姓名），可按网格员/状态筛选
     */
    @Select("<script>" +
            "SELECT l.*, e.emp_code, e.real_name AS grid_name FROM leave l " +
            "JOIN employee e ON l.emp_id = e.emp_id " +
            "<where>" +
            " <if test='empCode != null and empCode != \"\"'> AND e.emp_code = #{empCode} </if>" +
            " <if test='state != null'> AND l.state = #{state} </if>" +
            "</where>" +
            "ORDER BY l.leave_id DESC" +
            "</script>")
    List<Leave> list(@Param("empCode") String empCode, @Param("state") Integer state);
}
