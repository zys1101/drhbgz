package com.example.demo.service.impl;

import com.example.demo.entity.AqiLevel;
import com.example.demo.mapper.AqiLevelMapper;
import com.example.demo.service.IAqiLevelService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * AQI级别表 服务实现类
 */
@Service
public class AqiLevelServiceImpl extends ServiceImpl<AqiLevelMapper, AqiLevel> implements IAqiLevelService {
}
