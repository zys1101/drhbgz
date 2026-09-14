package com.example.demo.service.impl;

import com.example.demo.entity.Leave;
import com.example.demo.mapper.LeaveMapper;
import com.example.demo.service.ILeaveService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 请假 服务实现类（人员管理/HR）
 */
@Service
public class LeaveServiceImpl extends ServiceImpl<LeaveMapper, Leave> implements ILeaveService {

    @Override
    public List<Leave> listLeave(String empCode, Integer state) {
        return baseMapper.list(empCode, state);
    }
}
