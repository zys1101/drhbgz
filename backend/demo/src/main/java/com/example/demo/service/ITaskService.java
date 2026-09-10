package com.example.demo.service;

import com.example.demo.entity.AqiData;
import com.example.demo.entity.AqiFeedback;

import java.util.List;
import java.util.Map;

/**
 * 任务流转服务：指派网格员（NEPM）→ 网格员提交实测数据（NEPG）→ 确认/退回（NEPM）
 */
public interface ITaskService {

    /** 指派网格员 */
    void assign(Integer afId, String gridCode);

    /** 指派给网格员的任务列表 */
    List<AqiFeedback> myTasks(String gridCode);

    /** 网格员提交实测AQI数据（AQI = MAX(SO2, CO, PM2.5)） */
    AqiData submitMeasure(Integer afId, String gridCode, Integer so2Grade, Integer coGrade, Integer pm25Grade);

    /** 管理员确认实测AQI数据（纳入统计） */
    void confirm(Integer dataId);

    /** 管理员退回异常实测数据（任务重新变为待指派） */
    void reject(Integer dataId);
}
