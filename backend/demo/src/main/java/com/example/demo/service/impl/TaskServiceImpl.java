package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo.common.BusinessException;
import com.example.demo.entity.AqiData;
import com.example.demo.entity.AqiFeedback;
import com.example.demo.entity.Employee;
import com.example.demo.mapper.AqiDataMapper;
import com.example.demo.mapper.AqiFeedbackMapper;
import com.example.demo.mapper.EmployeeMapper;
import com.example.demo.service.ITaskService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 任务流转服务实现
 * 状态流转：0待指派 →(指派)→ 1已指派 →(提交实测)→ 2待确认 →(确认)→ 3已确认
 *                                        ↘(退回)→ 0待指派（重新指派检测）
 */
@Slf4j
@Service
@AllArgsConstructor
public class TaskServiceImpl implements ITaskService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AqiFeedbackMapper feedbackMapper;
    private final AqiDataMapper aqiDataMapper;
    private final EmployeeMapper employeeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Integer afId, String gridCode) {
        AqiFeedback feedback = feedbackMapper.selectById(afId);
        if (feedback == null) {
            throw new BusinessException("反馈数据不存在");
        }
        if (feedback.getState() == 3) {
            throw new BusinessException("该反馈已完成确认，不能再指派");
        }
        Employee worker = employeeMapper.selectOne(
                new LambdaUpdateWrapper<Employee>().eq(Employee::getEmpCode, gridCode));
        if (worker == null || !Employee.ROLE_GRID.equals(worker.getRole())) {
            throw new BusinessException("网格员不存在");
        }
        if (worker.getWorking() == null || worker.getWorking() != 1) {
            throw new BusinessException("该网格员当前处于非工作状态（请假/人员管理维护）");
        }
        LocalDateTime now = LocalDateTime.now();
        feedback.setGmId(worker.getEmpId());
        feedback.setAssignDate(now.format(DATE_FMT));
        feedback.setAssignTime(now.format(TIME_FMT));
        feedback.setState(1); // 已指派
        feedbackMapper.updateById(feedback);
        log.info("反馈{}已指派给网格员{}（本地/异地指派）", afId, gridCode);
    }

    @Override
    public List<AqiFeedback> myTasks(String gridCode) {
        Employee worker = employeeMapper.selectOne(
                new LambdaUpdateWrapper<Employee>().eq(Employee::getEmpCode, gridCode));
        if (worker == null) {
            throw new BusinessException("网格员不存在");
        }
        // 指派给该网格员、且尚未完成确认的任务
        return feedbackMapper.selectByCond(null, null, null, null,
                AqiFeedback.STATE_ASSIGNED, null, null, null)
                .stream()
                .filter(f -> worker.getEmpId().equals(f.getGmId()))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AqiData submitMeasure(Integer afId, String gridCode,
                                 Integer so2Grade, Integer coGrade, Integer pm25Grade) {
        AqiFeedback feedback = feedbackMapper.selectById(afId);
        if (feedback == null) {
            throw new BusinessException("反馈任务不存在");
        }
        if (feedback.getState() != AqiFeedback.STATE_ASSIGNED) {
            throw new BusinessException("任务当前状态不可提交实测数据");
        }
        Employee worker = employeeMapper.selectOne(
                new LambdaUpdateWrapper<Employee>().eq(Employee::getEmpCode, gridCode));
        if (worker == null) {
            throw new BusinessException("网格员不存在");
        }
        validateGrade(so2Grade, "SO2二氧化硫");
        validateGrade(coGrade, "CO一氧化碳");
        validateGrade(pm25Grade, "PM2.5悬浮颗粒物");

        // AQI = MAX（SO2AQI，COAQI，PM2.5AQI）
        int aqiGrade = Math.max(so2Grade, Math.max(coGrade, pm25Grade));

        LocalDateTime now = LocalDateTime.now();
        AqiData data = new AqiData();
        data.setAfId(afId);
        data.setSo2Grade(so2Grade);
        data.setCoGrade(coGrade);
        data.setPm25Grade(pm25Grade);
        data.setAqiGrade(aqiGrade);
        data.setEmpId(worker.getEmpId());
        data.setGridCode(worker.getEmpCode());
        data.setSubmitDate(now.format(DATE_FMT));
        data.setSubmitTime(now.format(TIME_FMT));
        data.setState(AqiData.STATE_WAIT_CONFIRM);
        aqiDataMapper.insert(data);

        feedback.setState(2); // 已提交实测，待管理员确认
        feedbackMapper.updateById(feedback);
        log.info("任务{}实测数据已提交，AQI等级={}", afId, aqiGrade);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Integer dataId) {
        AqiData data = aqiDataMapper.selectById(dataId);
        if (data == null) {
            throw new BusinessException("实测数据不存在");
        }
        if (data.getState() != AqiData.STATE_WAIT_CONFIRM) {
            throw new BusinessException("该数据当前状态不可确认");
        }
        data.setState(AqiData.STATE_CONFIRMED);
        aqiDataMapper.updateById(data);

        AqiFeedback feedback = feedbackMapper.selectById(data.getAfId());
        if (feedback != null) {
            feedback.setState(3); // 已确认（已完成）
            feedbackMapper.updateById(feedback);
        }
        log.info("实测数据{}已确认，纳入统计范围", dataId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Integer dataId) {
        AqiData data = aqiDataMapper.selectById(dataId);
        if (data == null) {
            throw new BusinessException("实测数据不存在");
        }
        if (data.getState() != AqiData.STATE_WAIT_CONFIRM) {
            throw new BusinessException("该数据当前状态不可退回");
        }
        data.setState(AqiData.STATE_REJECTED);
        aqiDataMapper.updateById(data);

        AqiFeedback feedback = feedbackMapper.selectById(data.getAfId());
        if (feedback != null) {
            // 退回后任务重新变为待指派，需重新指派检测
            feedback.setState(0);
            feedback.setGmId(null);
            feedback.setRemarks("实测数据存在异常，已退回，需重新指派检测");
            LambdaUpdateWrapper<AqiFeedback> uw = new LambdaUpdateWrapper<>();
            uw.eq(AqiFeedback::getAfId, feedback.getAfId())
                    .set(AqiFeedback::getState, 0)
                    .set(AqiFeedback::getGmId, null)
                    .set(AqiFeedback::getRemarks, feedback.getRemarks());
            feedbackMapper.update(null, uw);
        }
        log.info("实测数据{}已退回，任务{}重新进入待指派", dataId, data.getAfId());
    }

    private void validateGrade(Integer grade, String name) {
        if (grade == null || grade < 1 || grade > 6) {
            throw new BusinessException("请完整录入" + name + "AQI浓度等级（1-6级）");
        }
    }
}
