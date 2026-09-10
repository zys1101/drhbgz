package com.example.demo.service.impl;

import com.example.demo.entity.Supervisor;
import com.example.demo.mapper.SupervisorMapper;
import com.example.demo.service.ISupervisorService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 公众监督员 服务实现类
 */
@Service
public class SupervisorServiceImpl extends ServiceImpl<SupervisorMapper, Supervisor> implements ISupervisorService {

    @Override
    public Supervisor selectWithNames(String telId) {
        Supervisor sup = baseMapper.selectWithNames(telId);
        if (sup != null) {
            sup.setPassword(null); // 不向前端泄露密码
        }
        return sup;
    }
}
