package com.example.demo.service;

import com.example.demo.entity.Leave;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 请假 服务类（人员管理/HR）
 */
public interface ILeaveService extends IService<Leave> {

    /**
     * 请假记录列表，可按网格员编码/状态筛选
     */
    List<Leave> listLeave(String empCode, Integer state);

    /**
     * 网格员发起请假申请（校验网格员身份/事由/起止日期）
     * @return 请假编号
     */
    Integer apply(String gridCode, String reason, String startDate, String endDate);
}
