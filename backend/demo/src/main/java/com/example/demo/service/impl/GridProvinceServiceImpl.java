package com.example.demo.service.impl;

import com.example.demo.entity.GridProvince;
import com.example.demo.mapper.GridProvinceMapper;
import com.example.demo.service.IGridProvinceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 省级行政区 服务实现类
 */
@Service
public class GridProvinceServiceImpl extends ServiceImpl<GridProvinceMapper, GridProvince> implements IGridProvinceService {
}
