package com.example.demo.service;

import com.example.demo.entity.AqiData;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 网格员实测AQI数据 服务类
 */
public interface IAqiDataService extends IService<AqiData> {

    /** 实测数据列表（带关联信息） */
    List<AqiData> selectDataList();

    /** 实测数据详情 */
    AqiData selectDataDetail(Integer dataId);
}
