package com.example.demo.service.impl;

import com.example.demo.entity.AqiFeedback;
import com.example.demo.mapper.AqiFeedbackMapper;
import com.example.demo.service.IAqiFeedbackService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 空气质量公众监督反馈信息 服务实现类
 */
@Service
public class AqiFeedbackServiceImpl extends ServiceImpl<AqiFeedbackMapper, AqiFeedback> implements IAqiFeedbackService {

    @Override
    public List<AqiFeedback> findAll() {
        return baseMapper.findAll();
    }

    @Override
    public List<AqiFeedback> findByCond(String telId, Integer provinceId, Integer cityId,
                                        Integer grade, Integer state, String dateFrom,
                                        String dateTo, String keyword) {
        return baseMapper.selectByCond(telId, provinceId, cityId, grade, state, dateFrom, dateTo, keyword);
    }
}
