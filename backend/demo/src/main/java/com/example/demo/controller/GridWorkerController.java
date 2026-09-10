package com.example.demo.controller;

import com.example.demo.common.ResultVO;
import com.example.demo.entity.Employee;
import com.example.demo.service.IEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网格员名单接口（工作状态由“东软HR系统”统一管理）
 */
@Tag(name = "网格员")
@RestController
@RequestMapping("/gridWorker")
@AllArgsConstructor
@CrossOrigin
public class GridWorkerController {

    private final IEmployeeService employeeService;

    /**
     * 网格员列表（NEPM指派时选择网格员）
     */
    @GetMapping("/list")
    @Operation(summary = "网格员列表")
    public ResultVO list() {
        List<Employee> workers = employeeService.selectGridWorkers();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Employee e : workers) {
            Map<String, Object> item = new HashMap<>();
            item.put("empId", e.getEmpId());
            item.put("gridCode", e.getEmpCode());
            item.put("realName", e.getRealName());
            item.put("region", (e.getProvinceName() == null ? "" : e.getProvinceName())
                    + "-" + (e.getCityName() == null ? "" : e.getCityName()));
            item.put("working", e.getWorking() != null && e.getWorking() == 1);
            result.add(item);
        }
        return new ResultVO(200, "查询成功", result);
    }
}
