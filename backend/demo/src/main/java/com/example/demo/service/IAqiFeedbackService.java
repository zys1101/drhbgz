package com.example.demo.service;

import com.example.demo.entity.AqiFeedback;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 空气质量公众监督反馈信息 服务类
 */
public interface IAqiFeedbackService extends IService<AqiFeedback> {

    /** 查询所有反馈（带省/市名称） */
    List<AqiFeedback> findAll();

    /** 条件查询反馈列表 */
    List<AqiFeedback> findByCond(String telId, Integer provinceId, Integer cityId,
                                 Integer grade, Integer state, String dateFrom,
                                 String dateTo, String keyword);
}
