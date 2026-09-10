package com.example.demo.service.impl;

import com.example.demo.entity.AqiData;
import com.example.demo.mapper.AqiDataMapper;
import com.example.demo.service.IAqiDataService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 网格员实测AQI数据 服务实现类
 */
@Service
public class AqiDataServiceImpl extends ServiceImpl<AqiDataMapper, AqiData> implements IAqiDataService {

    @Override
    public List<AqiData> selectDataList() {
        return baseMapper.selectDataList();
    }

    @Override
    public AqiData selectDataDetail(Integer dataId) {
        return baseMapper.selectDataDetail(dataId);
    }
}
