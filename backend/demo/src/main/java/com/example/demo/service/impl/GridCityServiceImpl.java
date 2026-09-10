package com.example.demo.service.impl;

import com.example.demo.entity.GridCity;
import com.example.demo.mapper.GridCityMapper;
import com.example.demo.service.IGridCityService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 城市行政区 服务实现类
 */
@Service
public class GridCityServiceImpl extends ServiceImpl<GridCityMapper, GridCity> implements IGridCityService {
}
