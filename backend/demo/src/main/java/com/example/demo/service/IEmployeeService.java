package com.example.demo.service;

import com.example.demo.entity.Employee;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 员工（网格员/管理员/决策者） 服务类
 */
public interface IEmployeeService extends IService<Employee> {

    /** 网格员列表（带负责区域名称） */
    List<Employee> selectGridWorkers();
}
