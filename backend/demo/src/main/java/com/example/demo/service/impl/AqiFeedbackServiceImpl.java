package com.example.demo.service.impl;

import com.example.demo.entity.AqiFeedback;
import com.example.demo.mapper.AqiFeedbackMapper;
import com.example.demo.service.IAqiFeedbackService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author laohan
 * @since 2026-09-03
 */
@Service
public class AqiFeedbackServiceImpl extends ServiceImpl<AqiFeedbackMapper, AqiFeedback> implements IAqiFeedbackService {
    public List<AqiFeedback> findAll(){
        return baseMapper.findAll();
    }
}
