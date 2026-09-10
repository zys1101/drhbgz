package com.example.demo.mapper;

import com.example.demo.entity.Employee;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 员工 Mapper 接口（网格员/管理员/决策者）
 */
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    /** 网格员列表（带负责区域名称） */
    @Select("SELECT e.*, p.province_name, c.city_name FROM employee e " +
            "LEFT JOIN grid_province p ON e.province_id = p.province_id " +
            "LEFT JOIN grid_city c ON e.city_id = c.city_id " +
            "WHERE e.role = 'grid' ORDER BY e.emp_id")
    List<Employee> selectGridWorkers();
}
