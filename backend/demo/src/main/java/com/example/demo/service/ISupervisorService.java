package com.example.demo.service;

import java.util.List;

import com.example.demo.entity.Supervisor;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 公众监督员 服务类
 */
public interface ISupervisorService extends IService<Supervisor> {

    /** 查询监督员信息（带省/市名称，不含密码） */
    Supervisor selectWithNames(String telId);

    /** 监督员列表（带省/市名称，按注册时间倒序） */
    List<Supervisor> selectAllWithNames();
}
