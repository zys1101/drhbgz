package com.example.demo.service.impl;

import com.example.demo.entity.Employee;
import com.example.demo.entity.Leave;
import com.example.demo.mapper.LeaveMapper;
import com.example.demo.service.IEmployeeService;
import com.example.demo.service.ILeaveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 请假 服务实现类（人员管理/HR）
 */
@Service
public class LeaveServiceImpl extends ServiceImpl<LeaveMapper, Leave> implements ILeaveService {

    private final IEmployeeService employeeService;

    public LeaveServiceImpl(IEmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Override
    public List<Leave> listLeave(String empCode, Integer state) {
        return baseMapper.list(empCode, state);
    }

    @Override
    public Integer apply(String gridCode, String reason, String startDate, String endDate) {
        if (gridCode == null || gridCode.trim().isEmpty()) {
            throw new IllegalArgumentException("网格员不存在");
        }
        Employee emp = employeeService.getOne(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getEmpCode, gridCode.trim())
                .eq(Employee::getRole, Employee.ROLE_GRID));
        if (emp == null) {
            throw new IllegalArgumentException("网格员不存在");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写请假事由");
        }
        boolean dateOk = startDate != null && startDate.matches("\d{4}-\d{2}-\d{2}")
                && endDate != null && endDate.matches("\d{4}-\d{2}-\d{2}")
                && startDate.compareTo(endDate) <= 0;
        if (!dateOk) {
            throw new IllegalArgumentException("请选择正确的起止日期");
        }
        String now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Leave lv = new Leave();
        lv.setEmpId(emp.getEmpId());
        lv.setReason(reason.trim());
        lv.setStartDate(startDate);
        lv.setEndDate(endDate);
        lv.setState(Leave.STATE_PENDING);
        lv.setApplyDate(now.substring(0, 10));
        lv.setApplyTime(now.substring(11));
        save(lv);
        return lv.getLeaveId();
    }
}
